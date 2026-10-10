package com.dulce.backend.report;

public enum TopFlavorsSortBy {
    revenue("gross_revenue"),
    cakes("cakes_count"),
    kg("total_kg");

    private final String column;

    TopFlavorsSortBy(String column) {
        this.column = column;
    }

    public String column() {
        return column;
    }
}
