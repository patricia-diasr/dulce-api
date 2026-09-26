package com.dulce.backend.order;

import java.time.OffsetDateTime;
import org.springframework.data.jpa.domain.Specification;

public final class CakeOrderSpecifications {

    private CakeOrderSpecifications() {}

    public static Specification<CakeOrder> pickupBetween(OffsetDateTime from, OffsetDateTime to) {
        return (root, query, cb) ->
                (from == null || to == null) ? null : cb.between(root.get("pickupAt"), from, to);
    }

    public static Specification<CakeOrder> statusEquals(OrderStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }
}
