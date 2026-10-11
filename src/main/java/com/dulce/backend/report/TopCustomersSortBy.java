package com.dulce.backend.report;

public enum TopCustomersSortBy {
    revenue("net_amount"),
    orders("orders_count"),
    kg("total_kg");

    private final String column;

    TopCustomersSortBy(String column) {
        this.column = column;
    }

    public String column() {
        return column;
    }
}
