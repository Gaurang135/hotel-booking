package com.hotelbooking.service.search;

import com.hotelbooking.domain.DateRange;
import com.hotelbooking.domain.Property;
import com.hotelbooking.domain.RoomType;
import com.hotelbooking.service.pricing.PricingStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Compares the quoted price per night, so the filter always agrees with the price shown. */
@Component
public class PriceRangeFilter implements SearchFilter {

    private final PricingStrategy pricing;

    public PriceRangeFilter(PricingStrategy pricing) {
        this.pricing = pricing;
    }

    @Override
    public boolean matches(Property property, RoomType room, SearchCriteria criteria) {
        if (criteria.minPrice() == null && criteria.maxPrice() == null) {
            return true;
        }
        DateRange stay = criteria.stay();
        BigDecimal perNight = pricing.priceFor(room, stay)
                .divide(BigDecimal.valueOf(stay.nights()), 2, RoundingMode.HALF_UP);
        return (criteria.minPrice() == null || perNight.compareTo(criteria.minPrice()) >= 0)
                && (criteria.maxPrice() == null || perNight.compareTo(criteria.maxPrice()) <= 0);
    }
}
