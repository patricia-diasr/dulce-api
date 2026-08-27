package com.dulce.backend.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "flavor")
public class Flavor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "default_cake_base", nullable = false)
    private CakeFinish defaultCakeBase;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "default_topping", nullable = false)
    private CakeFinish defaultTopping;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CakeFinish getDefaultCakeBase() {
        return defaultCakeBase;
    }

    public void setDefaultCakeBase(CakeFinish defaultCakeBase) {
        this.defaultCakeBase = defaultCakeBase;
    }

    public CakeFinish getDefaultTopping() {
        return defaultTopping;
    }

    public void setDefaultTopping(CakeFinish defaultTopping) {
        this.defaultTopping = defaultTopping;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
