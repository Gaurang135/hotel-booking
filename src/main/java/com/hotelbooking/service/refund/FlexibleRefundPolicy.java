package com.hotelbooking.service.refund;

import com.hotelbooking.domain.Booking;
import com.hotelbooking.domain.BookingType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** 7+ days before check-in: 100%, 1-6 days: 50%, same day: 0%. */
@Component
public class FlexibleRefundPolicy implements RefundPolicy {

    @Override
    public BookingType bookingType() {
        return BookingType.FLEXIBLE;
    }

    @Override
    public BigDecimal refundFor(Booking booking, LocalDate cancelledOn) {
        long daysLeft = ChronoUnit.DAYS.between(cancelledOn, booking.getStay().checkIn());
        int percent = daysLeft >= 7 ? 100 : daysLeft >= 1 ? 50 : 0;
        return booking.amountPaid()
                .multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
