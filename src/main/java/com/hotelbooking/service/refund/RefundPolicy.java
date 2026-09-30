package com.hotelbooking.service.refund;

import com.hotelbooking.domain.Booking;
import com.hotelbooking.domain.BookingType;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RefundPolicy {

    BookingType bookingType();

    BigDecimal refundFor(Booking booking, LocalDate cancelledOn);
}
