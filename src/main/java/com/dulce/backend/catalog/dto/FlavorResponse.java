package com.dulce.backend.catalog.dto;

import com.dulce.backend.catalog.CakeFinish;
import java.util.List;

public record FlavorResponse(
        Long id,
        String name,
        CakeFinish defaultCakeBase,
        CakeFinish defaultTopping,
        boolean active,
        List<SizePriceResponse> prices) {}
