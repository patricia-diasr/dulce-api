package com.dulce.backend.catalog.dto;

import com.dulce.backend.catalog.CakeFinish;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FlavorCreateRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull CakeFinish defaultCakeBase,
        @NotNull CakeFinish defaultTopping,
        @NotEmpty @Valid List<SizePriceRequest> prices) {

    public FlavorCreateRequest {
        name = name == null ? null : name.trim();
    }
}
