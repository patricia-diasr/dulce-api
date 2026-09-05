package com.dulce.backend.order;

import com.dulce.backend.auth.AuthenticatedUser;
import com.dulce.backend.auth.Role;
import com.dulce.backend.catalog.Flavor;
import com.dulce.backend.catalog.FlavorRepository;
import com.dulce.backend.catalog.FlavorSize;
import com.dulce.backend.catalog.FlavorSizeRepository;
import com.dulce.backend.catalog.Size;
import com.dulce.backend.catalog.SizeRepository;
import com.dulce.backend.common.exception.BadRequestException;
import com.dulce.backend.common.exception.InvalidOrderStateException;
import com.dulce.backend.common.exception.ResourceNotFoundException;
import com.dulce.backend.customer.Customer;
import com.dulce.backend.customer.CustomerRepository;
import com.dulce.backend.notification.NotificationService;
import com.dulce.backend.order.dto.OrderContentRequest;
import com.dulce.backend.order.dto.OrderItemRequest;
import com.dulce.backend.order.dto.OrderResponse;
import com.dulce.backend.payment.Invoice;
import com.dulce.backend.payment.InvoiceRepository;
import com.dulce.backend.payment.InvoiceStatus;
import com.dulce.backend.schedule.ScheduleBlockChecker;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private static final Duration MIN_CREATE_LEAD_TIME = Duration.ofHours(72);
    private static final Duration MIN_EDIT_LEAD_TIME = Duration.ofHours(72);
    private static final Duration MIN_CANCEL_LEAD_TIME = Duration.ofHours(12);

    private final CakeOrderRepository cakeOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final FlavorRepository flavorRepository;
    private final SizeRepository sizeRepository;
    private final FlavorSizeRepository flavorSizeRepository;
    private final ScheduleBlockChecker scheduleBlockChecker;
    private final OrderMapper orderMapper;
    private final NotificationService notificationService;

    public OrderService(
            CakeOrderRepository cakeOrderRepository,
            OrderItemRepository orderItemRepository,
            InvoiceRepository invoiceRepository,
            CustomerRepository customerRepository,
            FlavorRepository flavorRepository,
            SizeRepository sizeRepository,
            FlavorSizeRepository flavorSizeRepository,
            ScheduleBlockChecker scheduleBlockChecker,
            OrderMapper orderMapper,
            NotificationService notificationService) {
        this.cakeOrderRepository = cakeOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.invoiceRepository = invoiceRepository;
        this.customerRepository = customerRepository;
        this.flavorRepository = flavorRepository;
        this.sizeRepository = sizeRepository;
        this.flavorSizeRepository = flavorSizeRepository;
        this.scheduleBlockChecker = scheduleBlockChecker;
        this.orderMapper = orderMapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public OrderResponse create(
            Long customerId, OrderContentRequest request, AuthenticatedUser requester) {
        Customer customer =
                customerRepository
                        .findById(customerId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Cliente não encontrado."));

        boolean isAdmin = requester.role() == Role.ADMIN;

        validatePickupAt(request.pickupAt(), isAdmin);

        CakeOrder order = new CakeOrder();
        order.setCustomer(customer);
        order.setCreatedAt(OffsetDateTime.now());
        order.setPickupAt(request.pickupAt());
        order.setNotes(request.notes());
        order.setStatus(isAdmin ? OrderStatus.ACCEPTED : OrderStatus.PENDING);
        order.setCreationChannel(
                isAdmin ? OrderCreationChannel.ADMIN : OrderCreationChannel.CUSTOMER);
        order = cakeOrderRepository.save(order);

        BigDecimal itemsTotal = replaceItems(order, request.items());
        BigDecimal discount = resolveDiscount(request.discount(), isAdmin, itemsTotal, null);

        Invoice invoice = new Invoice();
        invoice.setCakeOrder(order);
        invoice.setGrossAmount(itemsTotal);
        invoice.setDiscount(discount);
        invoice.setStatus(InvoiceStatus.PENDING);
        invoiceRepository.save(invoice);

        notificationService.notifyOrderCreated(order.getId());

        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse update(
            Long orderId, OrderContentRequest request, AuthenticatedUser requester) {
        CakeOrder order = findOrderOrThrow(orderId);
        boolean isAdmin = requester.role() == Role.ADMIN;

        if (!isAdmin) {
            requireOwnership(order, requester);
        }

        ensureEditable(order, isAdmin);
        validatePickupAt(request.pickupAt(), isAdmin);

        order.setPickupAt(request.pickupAt());
        order.setNotes(request.notes());

        BigDecimal itemsTotal = replaceItems(order, request.items());

        Invoice invoice =
                invoiceRepository
                        .findByCakeOrderId(order.getId())
                        .orElseThrow(
                                () -> new IllegalStateException("Pedido sem fatura associada."));
        BigDecimal discount =
                resolveDiscount(request.discount(), isAdmin, itemsTotal, invoice.getDiscount());
        invoice.setGrossAmount(itemsTotal);
        invoice.setDiscount(discount);
        invoiceRepository.save(invoice);

        if (!isAdmin && order.getStatus() == OrderStatus.ACCEPTED) {
            order.setStatus(OrderStatus.PENDING);
        }

        notificationService.notifyOrderUpdated(order.getId());

        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse accept(Long orderId) {
        CakeOrder order = findOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException("Só é possível aceitar pedidos pendentes.");
        }

        order.setStatus(OrderStatus.ACCEPTED);
        notificationService.notifyOrderAccepted(order.getId());

        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse reject(Long orderId) {
        CakeOrder order = findOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException("Só é possível recusar pedidos pendentes.");
        }

        order.setStatus(OrderStatus.REJECTED);
        notificationService.notifyOrderRejected(order.getId());

        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse cancel(Long orderId, AuthenticatedUser requester) {
        CakeOrder order = findOrderOrThrow(orderId);
        boolean isAdmin = requester.role() == Role.ADMIN;

        if (!isAdmin) {
            requireOwnership(order, requester);
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.ACCEPTED) {
            throw new InvalidOrderStateException("Esse pedido não pode ser cancelado.");
        }

        if (!isAdmin) {
            Duration remaining = Duration.between(OffsetDateTime.now(), order.getPickupAt());

            if (remaining.compareTo(MIN_CANCEL_LEAD_TIME) < 0) {
                throw new InvalidOrderStateException(
                        "Prazo para cancelamento expirado — entre em contato diretamente.");
            }
        }

        order.setStatus(OrderStatus.CANCELED);
        notificationService.notifyOrderCanceled(order.getId());

        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse complete(Long orderId) {
        CakeOrder order = findOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.ACCEPTED) {
            throw new InvalidOrderStateException("Só é possível concluir pedidos aceitos.");
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(OffsetDateTime.now());
        notificationService.notifyOrderCompleted(order.getId());

        return orderMapper.toResponse(order);
    }

    private CakeOrder findOrderOrThrow(Long orderId) {
        return cakeOrderRepository
                .findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado."));
    }

    private void requireOwnership(CakeOrder order, AuthenticatedUser requester) {
        if (!order.getCustomer().getId().equals(requester.id())) {
            throw new AccessDeniedException("Você não tem permissão para acessar este pedido.");
        }
    }

    private void ensureEditable(CakeOrder order, boolean isAdmin) {
        if (order.getStatus() == OrderStatus.CANCELED
                || order.getStatus() == OrderStatus.REJECTED
                || order.getStatus() == OrderStatus.COMPLETED) {
            throw new InvalidOrderStateException("Pedido não pode mais ser editado.");
        }

        if (isAdmin || order.getStatus() == OrderStatus.PENDING) {
            return;
        }

        Duration remaining = Duration.between(OffsetDateTime.now(), order.getPickupAt());

        if (remaining.compareTo(MIN_EDIT_LEAD_TIME) < 0) {
            throw new InvalidOrderStateException(
                    "Esse pedido não pode mais ser editado — entre em contato diretamente.");
        }
    }

    private void validatePickupAt(OffsetDateTime pickupAt, boolean isAdmin) {
        if (isAdmin) {
            return;
        }

        Duration lead = Duration.between(OffsetDateTime.now(), pickupAt);

        if (lead.compareTo(MIN_CREATE_LEAD_TIME) < 0) {
            throw new BadRequestException(
                    "Encomendas devem ser feitas com pelo menos 72 horas de antecedência.");
        }

        if (scheduleBlockChecker.isBlocked(pickupAt)) {
            throw new BadRequestException("Esse horário não está disponível para retirada.");
        }
    }

    private BigDecimal replaceItems(CakeOrder order, List<OrderItemRequest> requests) {
        orderItemRepository.deleteAll(orderItemRepository.findByCakeOrderId(order.getId()));

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest req : requests) {
            Flavor flavor =
                    flavorRepository
                            .findById(req.flavorId())
                            .orElseThrow(
                                    () -> new ResourceNotFoundException("Recheio não encontrado."));

            if (!flavor.isActive()) {
                throw new BadRequestException("Recheio não está mais disponível.");
            }

            Size size =
                    sizeRepository
                            .findById(req.sizeId())
                            .orElseThrow(
                                    () -> new ResourceNotFoundException("Tamanho não encontrado."));
            FlavorSize flavorSize =
                    flavorSizeRepository
                            .findByFlavorIdAndSizeId(flavor.getId(), size.getId())
                            .orElseThrow(
                                    () ->
                                            new BadRequestException(
                                                    "Não há preço cadastrado para esse recheio nesse tamanho."));

            if (req.message() != null && req.message().length() > size.getMaxMessageLength()) {
                throw new BadRequestException(
                        "Mensagem excede o limite de "
                                + size.getMaxMessageLength()
                                + " caracteres para esse tamanho.");
            }

            OrderItem item = new OrderItem();
            item.setCakeOrder(order);
            item.setFlavor(flavor);
            item.setSize(size);
            item.setCakeBase(req.cakeBase());
            item.setTopping(req.topping());
            item.setUnitPrice(flavorSize.getSalePrice());
            item.setMessage(req.message());
            item.setNotes(req.notes());
            orderItemRepository.save(item);

            total = total.add(flavorSize.getSalePrice());
        }

        return total;
    }

    private BigDecimal resolveDiscount(
            BigDecimal requestedDiscount,
            boolean isAdmin,
            BigDecimal itemsTotal,
            BigDecimal existingDiscount) {

        if (requestedDiscount == null) {
            return existingDiscount != null ? existingDiscount : BigDecimal.ZERO;
        }

        if (!isAdmin) {
            throw new AccessDeniedException("Apenas o administrador pode aplicar desconto.");
        }

        if (requestedDiscount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Desconto não pode ser negativo.");
        }

        if (requestedDiscount.compareTo(itemsTotal) > 0) {
            throw new BadRequestException("Desconto não pode ser maior que o valor do pedido.");
        }

        return requestedDiscount;
    }
}
