package com.example.hotel.repository;

import com.example.hotel.domain.Room;
import org.springframework.data.jpa.domain.Specification;

public final class RoomSpecifications {

    private RoomSpecifications() {
    }

    public static Specification<Room> filter(Long hotelId, Boolean available) {
        return (root, query, builder) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (hotelId != null) {
                predicates.add(builder.equal(root.get("hotel").get("id"), hotelId));
            }
            if (available != null) {
                predicates.add(builder.equal(root.get("available"), available));
            }
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
