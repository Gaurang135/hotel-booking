package com.hotelbooking.service.search;

import com.hotelbooking.domain.Amenity;
import com.hotelbooking.domain.Property;
import com.hotelbooking.domain.PropertyType;
import com.hotelbooking.domain.RoomType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record SearchResult(String propertyId, String name, PropertyType type, LocationView location,
                           int starRating, Set<Amenity> amenities, List<RoomOffer> offers) {

    public static SearchResult of(Property property, List<RoomOffer> offers) {
        return new SearchResult(property.id(), property.name(), property.type(),
                new LocationView(property.location().city(), property.location().locality()),
                property.starRating(), property.amenities(), List.copyOf(offers));
    }

    public record LocationView(String city, String locality) {
    }

    public record RoomOffer(String roomTypeId, String name, int maxGuests, int roomsLeft, BigDecimal totalPrice) {

        public static RoomOffer of(RoomType room, int roomsLeft, BigDecimal totalPrice) {
            return new RoomOffer(room.id(), room.name(), room.maxGuests(), roomsLeft, totalPrice);
        }
    }
}
