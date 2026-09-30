package com.hotelbooking.repository;

import com.hotelbooking.domain.Property;
import com.hotelbooking.exception.NotFoundException;

import java.util.List;
import java.util.Optional;

public interface PropertyRepository {

    Property save(Property property);

    Optional<Property> findById(String id);

    List<Property> findByOwnerId(String ownerId);

    List<Property> findByCity(String city);

    default Property getById(String id) {
        return findById(id).orElseThrow(() -> new NotFoundException("Property", id));
    }
}
