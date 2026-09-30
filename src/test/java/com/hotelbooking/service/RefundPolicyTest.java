package com.hotelbooking.service;

import com.hotelbooking.domain.Booking;
import com.hotelbooking.domain.BookingType;
import com.hotelbooking.domain.DateRange;
import com.hotelbooking.domain.Payment;
import com.hotelbooking.domain.PaymentMethod;
import com.hotelbooking.domain.RoomType;
import com.hotelbooking.service.refund.FlexibleRefundPolicy;
import com.hotelbooking.service.refund.NonRefundableRefundPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class RefundPolicyTest {

    private static final LocalDate CHECK_IN = LocalDate.of(2026, 10, 20);

    private final RoomType room = new RoomType("r1", "Deluxe", 2, 1, new BigDecimal("999.99"));

    private Booking booking(BookingType type) {
        return Booking.pending("p1", room, new DateRange(CHECK_IN, CHECK_IN.plusDays(1)), 1, "Ravi", type, new BigDecimal("999.99"));
    }

    private Booking paid(BookingType type) {
        Booking booking = booking(type);
        booking.recordPayment(new Payment(PaymentMethod.CARD, new BigDecimal("999.99"), true, "pay_1", null));
        return booking;
    }

    @ParameterizedTest(name = "flexible: {0} days before check-in refunds {1}")
    @CsvSource({"10, 999.99", "7, 999.99", "6, 500.00", "1, 500.00", "0, 0.00"})
    void flexibleRefundDependsOnDaysLeftBeforeCheckIn(int daysBefore, String expectedRefund) {
        assertThat(new FlexibleRefundPolicy().refundFor(paid(BookingType.FLEXIBLE), CHECK_IN.minusDays(daysBefore)))
                .isEqualByComparingTo(expectedRefund);
    }

    @Test
    void flexibleUnpaidBookingRefundsNothing() {
        assertThat(new FlexibleRefundPolicy().refundFor(booking(BookingType.FLEXIBLE), CHECK_IN.minusDays(30)))
                .isEqualByComparingTo("0");
    }

    @Test
    void nonRefundableRefundsNothingEvenWellAhead() {
        assertThat(new NonRefundableRefundPolicy().refundFor(paid(BookingType.NON_REFUNDABLE), CHECK_IN.minusDays(30)))
                .isEqualByComparingTo("0");
    }

    @Test
    void eachPolicyDeclaresItsBookingType() {
        assertThat(new FlexibleRefundPolicy().bookingType()).isEqualTo(BookingType.FLEXIBLE);
        assertThat(new NonRefundableRefundPolicy().bookingType()).isEqualTo(BookingType.NON_REFUNDABLE);
    }
}
