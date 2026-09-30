package com.hotelbooking.service.refund;

import com.hotelbooking.domain.Booking;
import com.hotelbooking.domain.BookingType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class NonRefundableRefundPolicy implements RefundPolicy {

    @Override
    public BookingType bookingType() {
        return BookingType.NON_REFUNDABLE;
    }

    @Override
    public BigDecimal refundFor(Booking booking, LocalDate cancelledOn) {
        return BigDecimal.ZERO;
    }
}
