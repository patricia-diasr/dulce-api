package com.dulce.backend.customer;

import org.springframework.data.jpa.domain.Specification;

public final class CustomerSpecifications {

    private CustomerSpecifications() {}

    public static Specification<Customer> nameContains(String name) {
        return (root, query, cb) ->
                (name == null || name.isBlank())
                        ? null
                        : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Customer> emailContains(String email) {
        return (root, query, cb) ->
                (email == null || email.isBlank())
                        ? null
                        : cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
    }

    public static Specification<Customer> phoneContains(String phone) {
        return (root, query, cb) -> {
            if (phone == null || phone.isBlank()) {
                return null;
            }
            String digitsOnly = phone.replaceAll("\\D", "");
            var normalizedColumn =
                    cb.function(
                            "regexp_replace",
                            String.class,
                            root.get("phone"),
                            cb.literal("\\D"),
                            cb.literal(""),
                            cb.literal("g"));
            return cb.like(normalizedColumn, "%" + digitsOnly + "%");
        };
    }
}
