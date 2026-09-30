package com.hotelbooking.service.pricing;

import com.hotelbooking.domain.DateRange;
import com.hotelbooking.domain.RoomType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class StandardPricingStrategy implements PricingStrategy {

    @Override
    public BigDecimal priceFor(RoomType room, DateRange stay) {
        return room.pricePerNight().multiply(BigDecimal.valueOf(stay.nights()));
    }
}
