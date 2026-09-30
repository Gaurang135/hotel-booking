package com.hotelbooking.api.dto;

import com.hotelbooking.domain.Amenity;
import com.hotelbooking.domain.Location;
import com.hotelbooking.domain.Property;
import com.hotelbooking.domain.PropertyType;
import com.hotelbooking.domain.RoomType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record AddPropertyRequest(
        @NotBlank String name,
        @NotNull PropertyType type,
        @NotBlank String city,
        String locality,
        @NotNull Integer starRating,
        Set<Amenity> amenities,
        @NotEmpty List<@NotNull @Valid RoomTypeRequest> roomTypes) {

    public Property toProperty(String ownerId) {
        List<RoomType> rooms = roomTypes.stream().map(RoomTypeRequest::toRoomType).toList();
        return Property.create(ownerId, name, type, new Location(city, locality), starRating, amenities, rooms);
    }

    public record RoomTypeRequest(
            @NotBlank String name,
            @NotNull @Positive Integer maxGuests,
            @NotNull @Positive Integer totalRooms,
            @NotNull @Positive @Digits(integer = 7, fraction = 2) BigDecimal pricePerNight) {

        RoomType toRoomType() {
            return RoomType.create(name, maxGuests, totalRooms, pricePerNight);
        }
    }
}
