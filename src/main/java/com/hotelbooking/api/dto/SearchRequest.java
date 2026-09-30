package com.hotelbooking.api.dto;

import com.hotelbooking.domain.Amenity;
import com.hotelbooking.service.search.SearchCriteria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

public record SearchRequest(
        @NotBlank String city,
        String locality,
        @NotNull LocalDate checkIn,
        @NotNull LocalDate checkOut,
        @NotNull Integer guests,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Set<Amenity> amenities,
        Integer minStars) {

    public SearchCriteria toCriteria() {
        return new SearchCriteria(city, locality, checkIn, checkOut, guests, minPrice, maxPrice, amenities, minStars);
    }
}
