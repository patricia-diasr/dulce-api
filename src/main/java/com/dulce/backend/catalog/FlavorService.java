package com.dulce.backend.catalog;

import com.dulce.backend.catalog.dto.FlavorCreateRequest;
import com.dulce.backend.catalog.dto.FlavorResponse;
import com.dulce.backend.catalog.dto.FlavorUpdateRequest;
import com.dulce.backend.catalog.dto.SizePriceRequest;
import com.dulce.backend.catalog.dto.SizePriceResponse;
import com.dulce.backend.common.exception.BadRequestException;
import com.dulce.backend.common.exception.DuplicateResourceException;
import com.dulce.backend.common.exception.ResourceNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FlavorService {

    private final FlavorRepository flavorRepository;
    private final FlavorSizeRepository flavorSizeRepository;
    private final SizeRepository sizeRepository;

    public FlavorService(
            FlavorRepository flavorRepository,
            FlavorSizeRepository flavorSizeRepository,
            SizeRepository sizeRepository) {
        this.flavorRepository = flavorRepository;
        this.flavorSizeRepository = flavorSizeRepository;
        this.sizeRepository = sizeRepository;
    }

    @Transactional(readOnly = true)
    public List<FlavorResponse> listAll(Boolean active) {
        List<Flavor> flavors =
                active == null ? flavorRepository.findAll() : flavorRepository.findByActive(active);
        return flavors.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public FlavorResponse getById(Long id) {
        return toResponse(findFlavorOrThrow(id));
    }

    @Transactional
    public FlavorResponse create(FlavorCreateRequest request) {
        if (flavorRepository.existsByNameIgnoreCase(request.name())) {
            throw new DuplicateResourceException("Já existe um recheio cadastrado com este nome.");
        }

        List<Size> allSizes = sizeRepository.findAll();
        validatePriceMatrixCoversAllSizes(request.prices(), allSizes);

        Flavor flavor = new Flavor();
        flavor.setName(request.name());
        flavor.setDefaultCakeBase(request.defaultCakeBase());
        flavor.setDefaultTopping(request.defaultTopping());
        flavor.setActive(true);
        flavor = flavorRepository.save(flavor);

        Map<Long, Size> sizesById =
                allSizes.stream().collect(Collectors.toMap(Size::getId, s -> s));

        for (SizePriceRequest priceRequest : request.prices()) {
            FlavorSize flavorSize = new FlavorSize();
            flavorSize.setFlavor(flavor);
            flavorSize.setSize(sizesById.get(priceRequest.sizeId()));
            flavorSize.setCostPrice(priceRequest.costPrice());
            flavorSize.setSalePrice(priceRequest.salePrice());
            flavorSizeRepository.save(flavorSize);
        }

        return toResponse(flavor);
    }

    @Transactional
    public FlavorResponse update(Long id, FlavorUpdateRequest request) {
        Flavor flavor = findFlavorOrThrow(id);

        if (request.name() != null) {
            boolean sameName = request.name().equalsIgnoreCase(flavor.getName());

            if (!sameName && flavorRepository.existsByNameIgnoreCase(request.name())) {
                throw new DuplicateResourceException(
                        "Já existe um recheio cadastrado com este nome.");
            }

            flavor.setName(request.name());
        }

        if (request.defaultCakeBase() != null) {
            flavor.setDefaultCakeBase(request.defaultCakeBase());
        }

        if (request.defaultTopping() != null) {
            flavor.setDefaultTopping(request.defaultTopping());
        }

        if (request.prices() != null && !request.prices().isEmpty()) {
            upsertPrices(flavor, request.prices());
        }

        return toResponse(flavor);
    }

    @Transactional
    public void deactivate(Long id) {
        Flavor flavor = findFlavorOrThrow(id);
        flavor.setActive(false);
    }

    private void upsertPrices(Flavor flavor, List<SizePriceRequest> priceRequests) {
        List<Long> requestedSizeIds = priceRequests.stream().map(SizePriceRequest::sizeId).toList();
        List<Size> validSizes = sizeRepository.findAllById(requestedSizeIds);

        if (validSizes.size() != requestedSizeIds.stream().distinct().count()) {
            throw new BadRequestException("Um ou mais tamanhos informados não existem.");
        }

        Map<Long, Size> sizesById =
                validSizes.stream().collect(Collectors.toMap(Size::getId, s -> s));

        for (SizePriceRequest priceRequest : priceRequests) {
            FlavorSize flavorSize =
                    flavorSizeRepository
                            .findByFlavorIdAndSizeId(flavor.getId(), priceRequest.sizeId())
                            .orElseGet(
                                    () -> {
                                        FlavorSize created = new FlavorSize();
                                        created.setFlavor(flavor);
                                        created.setSize(sizesById.get(priceRequest.sizeId()));
                                        return created;
                                    });

            flavorSize.setCostPrice(priceRequest.costPrice());
            flavorSize.setSalePrice(priceRequest.salePrice());
            flavorSizeRepository.save(flavorSize);
        }
    }

    private void validatePriceMatrixCoversAllSizes(
            List<SizePriceRequest> prices, List<Size> allSizes) {
        Set<Long> providedSizeIds =
                prices.stream().map(SizePriceRequest::sizeId).collect(Collectors.toSet());
        Set<Long> requiredSizeIds = allSizes.stream().map(Size::getId).collect(Collectors.toSet());

        if (!providedSizeIds.equals(requiredSizeIds)) {
            throw new BadRequestException(
                    "É obrigatório informar o preço para todos os "
                            + requiredSizeIds.size()
                            + " tamanhos cadastrados.");
        }
    }

    private Flavor findFlavorOrThrow(Long id) {
        return flavorRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recheio não encontrado."));
    }

    private FlavorResponse toResponse(Flavor flavor) {
        List<SizePriceResponse> prices =
                flavorSizeRepository.findByFlavorId(flavor.getId()).stream()
                        .map(
                                fs ->
                                        new SizePriceResponse(
                                                fs.getSize().getId(),
                                                fs.getSize().getName(),
                                                fs.getCostPrice(),
                                                fs.getSalePrice()))
                        .sorted(Comparator.comparing(SizePriceResponse::sizeId))
                        .toList();

        return new FlavorResponse(
                flavor.getId(),
                flavor.getName(),
                flavor.getDefaultCakeBase(),
                flavor.getDefaultTopping(),
                flavor.isActive(),
                prices);
    }
}
