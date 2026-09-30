package com.hotelbooking.domain;

import java.util.UUID;

/** An account that owns one property (standalone) or many (a chain). */
public record Owner(String id, String name, String email) {

    public Owner {
        id = Require.text(id, "owner id");
        name = Require.text(name, "owner name");
        email = email == null || email.isBlank() ? null : email.trim();
    }

    public static Owner create(String name, String email) {
        return new Owner(UUID.randomUUID().toString(), name, email);
    }
}
