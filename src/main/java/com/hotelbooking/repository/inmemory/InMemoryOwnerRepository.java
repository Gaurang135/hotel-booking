package com.hotelbooking.repository.inmemory;

import com.hotelbooking.domain.Owner;
import com.hotelbooking.repository.OwnerRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryOwnerRepository implements OwnerRepository {

    private final Map<String, Owner> owners = new ConcurrentHashMap<>();

    @Override
    public Owner save(Owner owner) {
        owners.put(owner.id(), owner);
        return owner;
    }

    @Override
    public Optional<Owner> findById(String id) {
        return Optional.ofNullable(owners.get(id));
    }
}
