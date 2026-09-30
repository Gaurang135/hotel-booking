package com.hotelbooking.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record Property(String id, String ownerId, String name, PropertyType type, Location location,
                       int starRating, Set<Amenity> amenities, List<RoomType> roomTypes) {

    public Property {
        id = Require.text(id, "property id");
        ownerId = Require.text(ownerId, "ownerId");
        name = Require.text(name, "property name");
        Require.that(type != null, "property type is required");
        Require.that(location != null, "location is required");
        Require.that(starRating >= 1 && starRating <= 5, "starRating must be between 1 and 5");
        Require.that(roomTypes != null && !roomTypes.isEmpty(), "a property needs at least one room type");
        amenities = amenities == null ? Set.of() : Set.copyOf(amenities);
        roomTypes = List.copyOf(roomTypes);
    }

    public static Property create(String ownerId, String name, PropertyType type, Location location,
                                  int starRating, Set<Amenity> amenities, List<RoomType> roomTypes) {
        return new Property(UUID.randomUUID().toString(), ownerId, name, type, location, starRating, amenities, roomTypes);
    }

    public Optional<RoomType> findRoomType(String roomTypeId) {
        return roomTypes.stream().filter(room -> room.id().equals(roomTypeId)).findFirst();
    }
}
