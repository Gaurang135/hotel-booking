package com.hotelbooking.repository;

import com.hotelbooking.domain.Owner;
import com.hotelbooking.exception.NotFoundException;

import java.util.Optional;

public interface OwnerRepository {

    Owner save(Owner owner);

    Optional<Owner> findById(String id);

    default Owner getById(String id) {
        return findById(id).orElseThrow(() -> new NotFoundException("Owner", id));
    }
}
