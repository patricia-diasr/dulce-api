package com.dulce.backend.order;

import com.dulce.backend.auth.AuthenticatedUser;
import com.dulce.backend.order.dto.OrderCalendarPageResponse;
import com.dulce.backend.order.dto.OrderContentRequest;
import com.dulce.backend.order.dto.OrderResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public OrderCalendarPageResponse list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return orderService.listCalendar(from, to, status, page, size);
    }

    @GetMapping("/{orderId}")
    public OrderResponse getById(
            @PathVariable Long orderId, @AuthenticationPrincipal AuthenticatedUser requester) {
        return orderService.getById(orderId, requester);
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
