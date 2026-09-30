package com.hotelbooking.service.search;

import com.hotelbooking.domain.Amenity;
import com.hotelbooking.domain.DateRange;
import com.hotelbooking.domain.Require;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record SearchCriteria(String city, String locality, LocalDate checkIn, LocalDate checkOut, Integer guests,
                             BigDecimal minPrice, BigDecimal maxPrice, Set<Amenity> amenities, Integer minStars) {

    public SearchCriteria {
        // an empty token like "amenities=WIFI," binds as null; ignore it
        amenities = amenities == null ? Set.of()
                : amenities.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
    }

    public void validate(LocalDate today) {
        Require.text(city, "city");
        Require.that(guests != null && guests >= 1, "guests must be at least 1");
        Require.that(minStars == null || (minStars >= 1 && minStars <= 5), "minStars must be between 1 and 5");
        Require.that(isNotNegative(minPrice) && isNotNegative(maxPrice), "prices cannot be negative");
        Require.that(minPrice == null || maxPrice == null || minPrice.compareTo(maxPrice) <= 0,
                "minPrice cannot be greater than maxPrice");
        stay().ensureNotInPast(today);
    }

    public DateRange stay() {
        return new DateRange(checkIn, checkOut);
    }

    private static boolean isNotNegative(BigDecimal price) {
        return price == null || price.signum() >= 0;
    }
}
