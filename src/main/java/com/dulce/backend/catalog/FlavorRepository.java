package com.dulce.backend.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlavorRepository extends JpaRepository<Flavor, Long> {

    boolean existsByNameIgnoreCase(String name);

    List<Flavor> findByActive(boolean active);
}
