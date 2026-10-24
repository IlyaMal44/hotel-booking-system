package com.example.hotel.repository;

import com.example.hotel.domain.Hotel;
import org.springframework.data.jpa.domain.Specification;

public final class HotelSpecifications {

    private HotelSpecifications() {
    }

    public static Specification<Hotel> filter(String name, String address, Integer minStars) {
        return (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (name != null && !name.isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (address != null && !address.isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("address")), "%" + address.toLowerCase() + "%"));
            }
            if (minStars != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("stars"), minStars));
            }
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
