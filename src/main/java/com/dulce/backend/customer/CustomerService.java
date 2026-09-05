package com.dulce.backend.customer;

import com.dulce.backend.common.exception.BadRequestException;
import com.dulce.backend.common.exception.ResourceNotFoundException;
import com.dulce.backend.customer.dto.CustomerDetailResponse;
import com.dulce.backend.customer.dto.CustomerPageResponse;
import com.dulce.backend.customer.dto.CustomerRegistrationRequest;
import com.dulce.backend.customer.dto.CustomerResponse;
import com.dulce.backend.customer.dto.CustomerSummaryResponse;
import com.dulce.backend.order.CakeOrderRepository;
import com.dulce.backend.order.OrderMapper;
import com.dulce.backend.order.dto.OrderSummaryResponse;
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
    private final OrderMapper orderMapper;

    public CustomerService(
            CustomerRepository customerRepository,
            CakeOrderRepository cakeOrderRepository,
            OrderMapper orderMapper) {
        this.customerRepository = customerRepository;
        this.cakeOrderRepository = cakeOrderRepository;
        this.orderMapper = orderMapper;
    }

    @Transactional
    public CustomerResponse register(CustomerRegistrationRequest request, boolean isAdminRequest) {
        if (!isAdminRequest && (request.email() == null || request.email().isBlank())) {
            throw new BadRequestException("E-mail é obrigatório.");
        }

        if (request.email() != null && customerRepository.existsByEmail(request.email())) {
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

        List<OrderSummaryResponse> orders =
                cakeOrderRepository.findByCustomerIdOrderByCreatedAtDesc(id).stream()
                        .map(orderMapper::toSummary)
                        .toList();

        return new CustomerDetailResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getNotes(),
                orders);
    }

    private CustomerSummaryResponse toSummary(Customer customer) {
        return new CustomerSummaryResponse(
                customer.getId(),
                customer.getName(),
                customer.getPhone(),
                customer.getEmail(),
                customer.getNotes());
    }
}
