package com.hotelbooking.service;

import com.hotelbooking.domain.DateRange;
import com.hotelbooking.domain.RoomType;
import com.hotelbooking.service.pricing.StandardPricingStrategy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PricingStrategyTest {

    private static final LocalDate DAY_1 = LocalDate.of(2026, 10, 1);

    private final StandardPricingStrategy pricing = new StandardPricingStrategy();

    @Test
    void priceIsRatePerNightTimesNights() {
        RoomType room = new RoomType("r1", "Deluxe", 2, 1, new BigDecimal("1000"));

        assertThat(pricing.priceFor(room, new DateRange(DAY_1, DAY_1.plusDays(3)))).isEqualByComparingTo("3000");
    }

    @Test
    void keepsPaiseExact() {
        RoomType room = new RoomType("r2", "Odd", 2, 1, new BigDecimal("999.99"));

        assertThat(pricing.priceFor(room, new DateRange(DAY_1, DAY_1.plusDays(2)))).isEqualTo(new BigDecimal("1999.98"));
    }
}
