package com.dulce.backend.order;

import com.dulce.backend.auth.AuthenticatedUser;
import com.dulce.backend.order.dto.OrderContentRequest;
import com.dulce.backend.order.dto.OrderResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PatchMapping("/{orderId}")
    public OrderResponse update(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderContentRequest request,
            @AuthenticationPrincipal AuthenticatedUser requester) {
        return orderService.update(orderId, request, requester);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{orderId}/accept")
    public OrderResponse accept(@PathVariable Long orderId) {
        return orderService.accept(orderId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{orderId}/reject")
    public OrderResponse reject(@PathVariable Long orderId) {
        return orderService.reject(orderId);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancel(
            @PathVariable Long orderId, @AuthenticationPrincipal AuthenticatedUser requester) {
        return orderService.cancel(orderId, requester);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{orderId}/complete")
    public OrderResponse complete(@PathVariable Long orderId) {
        return orderService.complete(orderId);
    }
}
