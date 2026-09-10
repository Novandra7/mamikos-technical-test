package com.mamikos.kostapi.kost.repository;

import com.mamikos.kostapi.kost.entity.Kost;
import com.mamikos.kostapi.kost.entity.RoomType;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * Composable, parameter-bound predicates for kost search.
 *
 * <p>Building the query this way — rather than concatenating a string — is what makes SQL
 * injection through a search filter structurally impossible: every value the caller
 * supplies becomes a bind parameter, never text spliced into the query.
 */
public final class KostSpecifications {

    private KostSpecifications() {}

    public static Specification<Kost> isPubliclyVisible() {
        return (root, query, cb) -> cb.and(cb.isTrue(root.get("active")), cb.isNull(root.get("deletedAt")));
    }

    public static Specification<Kost> nameContains(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String pattern = "%" + name.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern);
    }

    public static Specification<Kost> locationContains(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }
        String pattern = "%" + location.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> {
            Predicate city = cb.like(cb.lower(root.get("address").get("city")), pattern);
            Predicate district = cb.like(cb.lower(root.get("address").get("district")), pattern);
            Predicate street = cb.like(cb.lower(root.get("address").get("street")), pattern);
            return cb.or(city, district, street);
        };
    }

    public static Specification<Kost> priceGreaterThanOrEqual(BigDecimal min) {
        if (min == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("pricePerMonth"), min);
    }

    public static Specification<Kost> priceLessThanOrEqual(BigDecimal max) {
        if (max == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("pricePerMonth"), max);
    }

    public static Specification<Kost> hasRoomType(RoomType roomType) {
        if (roomType == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("roomType"), roomType);
    }

    public static Specification<Kost> ownedBy(Long ownerId) {
        return (root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId);
    }

    public static Specification<Kost> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }
}
