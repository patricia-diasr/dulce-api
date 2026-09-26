package com.dulce.backend.customer;

import com.dulce.backend.auth.AuthenticatedUser;
import com.dulce.backend.auth.Role;
import com.dulce.backend.customer.dto.CustomerDetailResponse;
import com.dulce.backend.customer.dto.CustomerPageResponse;
import com.dulce.backend.customer.dto.CustomerRegistrationRequest;
import com.dulce.backend.customer.dto.CustomerResponse;
import com.dulce.backend.customer.dto.CustomerSummaryResponse;
import com.dulce.backend.customer.dto.CustomerUpdateRequest;
import com.dulce.backend.order.OrderService;
import com.dulce.backend.order.dto.OrderContentRequest;
import com.dulce.backend.order.dto.OrderResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final OrderService orderService;

    public CustomerController(CustomerService customerService, OrderService orderService) {
        this.customerService = customerService;
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> register(
            @Valid @RequestBody CustomerRegistrationRequest request,
            @AuthenticationPrincipal AuthenticatedUser requester) {
        boolean isAdminRequest = requester != null && requester.role() == Role.ADMIN;
        CustomerResponse response = customerService.register(request, isAdminRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public CustomerPageResponse list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return customerService.list(name, email, phone, page, size);
    }

    @PreAuthorize("hasRole('ADMIN') or #id.equals(authentication.principal.id())")
    @GetMapping("/{id}")
    public CustomerDetailResponse getById(@PathVariable Long id) {
        return customerService.getDetailById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public CustomerSummaryResponse update(
            @PathVariable Long id, @Valid @RequestBody CustomerUpdateRequest request) {
        return customerService.update(id, request);
    }

    @PreAuthorize("hasRole('ADMIN') or #customerId.equals(authentication.principal.id())")
    @PostMapping("/{customerId}/orders")
    public ResponseEntity<OrderResponse> createOrder(
            @PathVariable Long customerId,
            @Valid @RequestBody OrderContentRequest request,
            @AuthenticationPrincipal AuthenticatedUser requester) {
        OrderResponse response = orderService.create(customerId, request, requester);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
