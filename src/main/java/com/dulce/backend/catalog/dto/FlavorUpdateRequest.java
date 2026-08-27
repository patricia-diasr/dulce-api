package com.dulce.backend.catalog.dto;

import com.dulce.backend.catalog.CakeFinish;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FlavorUpdateRequest(
        @Size(max = 100) String name,
        CakeFinish defaultCakeBase,
        CakeFinish defaultTopping,
        @Valid List<SizePriceRequest> prices) {

    public FlavorUpdateRequest {
        name = name == null ? null : name.trim();
    }
}
