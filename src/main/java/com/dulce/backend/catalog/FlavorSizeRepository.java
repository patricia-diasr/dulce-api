package com.dulce.backend.catalog;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlavorSizeRepository extends JpaRepository<FlavorSize, Long> {

    List<FlavorSize> findByFlavorId(Long flavorId);

    Optional<FlavorSize> findByFlavorIdAndSizeId(Long flavorId, Long sizeId);
}
