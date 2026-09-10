package com.mamikos.kostapi.kost.service;

import com.mamikos.kostapi.common.exception.ValidationException;
import com.mamikos.kostapi.kost.entity.Kost;
import com.mamikos.kostapi.kost.mapper.KostMapper;
import com.mamikos.kostapi.kost.repository.KostRepository;
import com.mamikos.kostapi.kost.repository.KostSpecifications;
import com.mamikos.kostapi.kost.web.dto.request.KostSearchRequest;
import com.mamikos.kostapi.kost.web.dto.response.KostSummaryResponse;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Public kost search: filter by name, location, and price; sort by a whitelisted property.
 *
 * <p>{@code sortBy} is checked against {@link #SORTABLE_PROPERTIES} instead of being fed to
 * {@code Pageable} unchecked — an unvalidated sort property lets a client sort by (and
 * thereby probe for) a column that was never meant to be exposed.
 */
@Service
@Transactional(readOnly = true)
public class KostSearchService {

    private static final Map<String, String> SORTABLE_PROPERTIES =
            Map.of("price", "pricePerMonth", "createdAt", "createdAt", "name", "name");

    private final KostRepository kostRepository;
    private final KostMapper kostMapper;

    public KostSearchService(KostRepository kostRepository, KostMapper kostMapper) {
        this.kostRepository = kostRepository;
        this.kostMapper = kostMapper;
    }

    public Page<KostSummaryResponse> search(KostSearchRequest request) {
        String sortProperty = SORTABLE_PROPERTIES.get(request.sortBy());
        if (sortProperty == null) {
            throw new ValidationException(
                    "sortBy", "sortBy must be one of " + String.join(", ", SORTABLE_PROPERTIES.keySet()));
        }
        Sort.Direction direction = parseDirection(request.order());

        Specification<Kost> spec = Specification.allOf(Stream.of(
                        KostSpecifications.isPubliclyVisible(),
                        KostSpecifications.nameContains(request.name()),
                        KostSpecifications.locationContains(request.location()),
                        KostSpecifications.priceGreaterThanOrEqual(request.priceMin()),
                        KostSpecifications.priceLessThanOrEqual(request.priceMax()),
                        KostSpecifications.hasRoomType(request.roomType()))
                .filter(java.util.Objects::nonNull)
                .toList());

        var pageable = PageRequest.of(request.page() - 1, request.perPage(), Sort.by(direction, sortProperty));
        return kostRepository.findAll(spec, pageable).map(kostMapper::toSummaryResponse);
    }

    private Sort.Direction parseDirection(String order) {
        if ("desc".equalsIgnoreCase(order)) {
            return Sort.Direction.DESC;
        }
        if ("asc".equalsIgnoreCase(order)) {
            return Sort.Direction.ASC;
        }
        throw new ValidationException("order", "order must be either 'asc' or 'desc'");
    }
}
