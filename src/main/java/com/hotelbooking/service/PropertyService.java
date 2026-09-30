package com.hotelbooking.service;

import com.hotelbooking.domain.Owner;
import com.hotelbooking.domain.Property;
import com.hotelbooking.repository.OwnerRepository;
import com.hotelbooking.repository.PropertyRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PropertyService {

    private final OwnerRepository owners;
    private final PropertyRepository properties;

    public PropertyService(OwnerRepository owners, PropertyRepository properties) {
        this.owners = owners;
        this.properties = properties;
    }

    public Owner addOwner(Owner owner) {
        return owners.save(owner);
    }

    public Property addProperty(Property property) {
        owners.getById(property.ownerId());
        return properties.save(property);
    }

    public List<Property> propertiesOf(String ownerId) {
        owners.getById(ownerId);
        return properties.findByOwnerId(ownerId);
    }

    public Property getProperty(String propertyId) {
        return properties.getById(propertyId);
    }
}
