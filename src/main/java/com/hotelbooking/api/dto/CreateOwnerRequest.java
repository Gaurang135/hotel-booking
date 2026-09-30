package com.hotelbooking.api.dto;

import com.hotelbooking.domain.Owner;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateOwnerRequest(@NotBlank String name, @Email String email) {

    public Owner toOwner() {
        return Owner.create(name, email);
    }
}
