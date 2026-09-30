package com.hotelbooking.repository.inmemory;

import com.hotelbooking.domain.Property;
import com.hotelbooking.repository.PropertyRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPropertyRepository implements PropertyRepository {

    private final Map<String, Property> properties = new ConcurrentHashMap<>();

    @Override
    public Property save(Property property) {
        properties.put(property.id(), property);
        return property;
    }

    @Override
    public Optional<Property> findById(String id) {
        return Optional.ofNullable(properties.get(id));
    }

    @Override
    public List<Property> findByOwnerId(String ownerId) {
        return properties.values().stream()
                .filter(property -> property.ownerId().equals(ownerId))
                .toList();
    }

    @Override
    public List<Property> findByCity(String city) {
        return properties.values().stream()
                .filter(property -> property.location().city().equalsIgnoreCase(city.trim()))
                .toList();
    }
}
