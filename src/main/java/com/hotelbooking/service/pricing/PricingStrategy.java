package com.hotelbooking.service.pricing;

import com.hotelbooking.domain.DateRange;
import com.hotelbooking.domain.RoomType;

import java.math.BigDecimal;

public interface PricingStrategy {

    BigDecimal priceFor(RoomType room, DateRange stay);
}
