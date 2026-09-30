package com.hotelbooking.api.dto;

import com.hotelbooking.domain.Owner;

public record OwnerResponse(String id, String name, String email) {

    public static OwnerResponse from(Owner owner) {
        return new OwnerResponse(owner.id(), owner.name(), owner.email());
    }
}
