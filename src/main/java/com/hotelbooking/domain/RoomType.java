package com.hotelbooking.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public record RoomType(String id, String name, int maxGuests, int totalRooms, BigDecimal pricePerNight) {

    public RoomType {
        id = Require.text(id, "room type id");
        name = Require.text(name, "room type name");
        Require.that(maxGuests >= 1, "maxGuests must be at least 1");
        Require.that(totalRooms >= 1, "totalRooms must be at least 1");
        pricePerNight = pricePerNight == null ? null : pricePerNight.setScale(2, RoundingMode.HALF_UP);
        Require.that(pricePerNight != null && pricePerNight.signum() > 0, "pricePerNight must be positive");
    }

    public static RoomType create(String name, int maxGuests, int totalRooms, BigDecimal pricePerNight) {
        return new RoomType(UUID.randomUUID().toString(), name, maxGuests, totalRooms, pricePerNight);
    }

    public boolean canFit(int guests) {
        return guests <= maxGuests;
    }
}
