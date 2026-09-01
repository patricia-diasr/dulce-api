package com.dulce.backend.catalog;

import com.dulce.backend.catalog.dto.FlavorCreateRequest;
import com.dulce.backend.catalog.dto.FlavorResponse;
import com.dulce.backend.catalog.dto.FlavorUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/flavors")
public class FlavorController {

    private final FlavorService flavorService;

    public FlavorController(FlavorService flavorService) {
        this.flavorService = flavorService;
    }

    @GetMapping
    public List<FlavorResponse> list(@RequestParam(required = false) Boolean active) {
        return flavorService.listAll(active);
    }

    @GetMapping("/{id}")
    public FlavorResponse getById(@PathVariable Long id) {
        return flavorService.getById(id);
    }

    @PostMapping
    public ResponseEntity<FlavorResponse> create(@Valid @RequestBody FlavorCreateRequest request) {
        FlavorResponse response = flavorService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public FlavorResponse update(
            @PathVariable Long id, @Valid @RequestBody FlavorUpdateRequest request) {
        return flavorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        flavorService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
