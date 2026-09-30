package com.hotelbooking.api.dto;

import com.hotelbooking.domain.Amenity;
import com.hotelbooking.domain.Property;
import com.hotelbooking.domain.PropertyType;
import com.hotelbooking.domain.RoomType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record PropertyResponse(String id, String ownerId, String name, PropertyType type, LocationResponse location,
                               int starRating, Set<Amenity> amenities, List<RoomTypeResponse> roomTypes) {

    public static PropertyResponse from(Property property) {
        return new PropertyResponse(property.id(), property.ownerId(), property.name(), property.type(),
                new LocationResponse(property.location().city(), property.location().locality()),
                property.starRating(), property.amenities(),
                property.roomTypes().stream().map(RoomTypeResponse::from).toList());
    }

    public record LocationResponse(String city, String locality) {
    }

    public record RoomTypeResponse(String id, String name, int maxGuests, int totalRooms, BigDecimal pricePerNight) {

        static RoomTypeResponse from(RoomType room) {
            return new RoomTypeResponse(room.id(), room.name(), room.maxGuests(), room.totalRooms(), room.pricePerNight());
        }
    }
}
