package com.dulce.backend.customer;

import com.dulce.backend.common.exception.ResourceNotFoundException;
import com.dulce.backend.customer.dto.CustomerDetailResponse;
import com.dulce.backend.customer.dto.CustomerPageResponse;
import com.dulce.backend.customer.dto.CustomerRegistrationRequest;
import com.dulce.backend.customer.dto.CustomerResponse;
import com.dulce.backend.customer.dto.CustomerSummaryResponse;
import com.dulce.backend.order.CakeOrder;
import com.dulce.backend.order.CakeOrderRepository;
import com.dulce.backend.order.OrderItem;
import com.dulce.backend.order.OrderItemRepository;
import com.dulce.backend.order.dto.OrderItemResponse;
import com.dulce.backend.order.dto.OrderSummaryResponse;
import com.dulce.backend.payment.Invoice;
import com.dulce.backend.payment.InvoiceRepository;
import com.dulce.backend.payment.PaymentRepository;
import com.dulce.backend.payment.dto.InvoiceResponse;
import com.dulce.backend.payment.dto.PaymentResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CakeOrderRepository cakeOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    public CustomerService(
            CustomerRepository customerRepository,
            CakeOrderRepository cakeOrderRepository,
            OrderItemRepository orderItemRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository) {
        this.customerRepository = customerRepository;
        this.cakeOrderRepository = cakeOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public CustomerResponse register(CustomerRegistrationRequest request) {
        if (customerRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException("Já existe um cliente cadastrado com este e-mail.");
        }

        Customer customer = new Customer();
        customer.setName(request.name());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());

        Customer saved = customerRepository.save(customer);

        return new CustomerResponse(
                saved.getId(), saved.getName(), saved.getEmail(), saved.getPhone());
    }

    @Transactional(readOnly = true)
    public CustomerPageResponse list(String name, String email, String phone, int page, int size) {
        Specification<Customer> spec =
                Specification.where(CustomerSpecifications.nameContains(name))
                        .and(CustomerSpecifications.emailContains(email))
                        .and(CustomerSpecifications.phoneContains(phone));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        Page<Customer> result = customerRepository.findAll(spec, pageable);

        List<CustomerSummaryResponse> content =
                result.getContent().stream().map(this::toSummary).toList();

        return new CustomerPageResponse(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public CustomerDetailResponse getDetailById(Long id) {
        Customer customer =
                customerRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Cliente não encontrado."));
        return toDetailResponse(customer);
    }

    private CustomerSummaryResponse toSummary(Customer customer) {
        return new CustomerSummaryResponse(
                customer.getId(),
                customer.getName(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getNotes());
    }

    private CustomerDetailResponse toDetailResponse(Customer customer) {
        List<OrderSummaryResponse> orders =
                cakeOrderRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
                        .map(this::toOrderSummary)
                        .toList();

        return new CustomerDetailResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getNotes(),
                orders);
    }

    private OrderSummaryResponse toOrderSummary(CakeOrder order) {
        List<OrderItemResponse> items =
                orderItemRepository.findByCakeOrderId(order.getId()).stream()
                        .map(this::toOrderItemResponse)
                        .toList();

        InvoiceResponse invoice =
                invoiceRepository
                        .findByCakeOrderId(order.getId())
                        .map(this::toInvoiceResponse)
                        .orElse(null);

        return new OrderSummaryResponse(
                order.getId(),
                order.getCreatedAt(),
                order.getPickupAt(),
                order.getCompletedAt(),
                order.getStatus(),
                order.getCreationChannel(),
                order.getNotes(),
                items,
                invoice);
    }

    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getFlavor().getId(),
                item.getFlavor().getName(),
                item.getSize().getId(),
                item.getSize().getName(),
                item.getTopping(),
                item.getCakeBase(),
                item.getUnitPrice(),
                item.getMessage(),
                item.getNotes());
    }

    private InvoiceResponse toInvoiceResponse(Invoice invoice) {
        List<PaymentResponse> payments =
                paymentRepository.findByInvoiceId(invoice.getId()).stream()
                        .map(
                                p ->
                                        new PaymentResponse(
                                                p.getId(),
                                                p.getAmount(),
                                                p.getPaidAt(),
                                                p.getPaymentMethod()))
                        .toList();

        return new InvoiceResponse(
                invoice.getId(),
                invoice.getGrossAmount(),
                invoice.getDiscount(),
                invoice.getStatus(),
                payments);
    }
}
