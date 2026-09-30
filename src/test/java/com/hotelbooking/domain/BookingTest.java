package com.hotelbooking.domain;

import com.hotelbooking.exception.InvalidBookingStateException;
import com.hotelbooking.exception.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.hotelbooking.domain.BookingStatus.CANCELLED;
import static com.hotelbooking.domain.BookingStatus.CONFIRMED;
import static com.hotelbooking.domain.BookingStatus.PAYMENT_FAILED;
import static com.hotelbooking.domain.BookingStatus.PENDING_PAYMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 1);
    private static final LocalDate CHECK_IN = TODAY.plusDays(5);

    private final RoomType room = new RoomType("r1", "Deluxe", 2, 1, new BigDecimal("1000"));

    private Booking newBooking() {
        return Booking.pending("p1", room, new DateRange(CHECK_IN, CHECK_IN.plusDays(2)), 2, "Ravi", BookingType.FLEXIBLE, new BigDecimal("2000"));
    }

    private static Payment payment(boolean success) {
        return new Payment(PaymentMethod.UPI, new BigDecimal("2000"), success, success ? "pay_1" : null, "key-1");
    }

    @Test
    void newBookingIsPendingAndHoldsTheRoom() {
        Booking booking = newBooking();

        assertThat(booking.getStatus()).isEqualTo(PENDING_PAYMENT);
        assertThat(booking.holdsRoom()).isTrue();
        assertThat(booking.getType()).isEqualTo(BookingType.FLEXIBLE);
    }

    @Test
    void successfulPaymentConfirmsAndKeepsTheRoom() {
        Booking booking = newBooking();

        booking.recordPayment(payment(true));

        assertThat(booking.getStatus()).isEqualTo(CONFIRMED);
        assertThat(booking.holdsRoom()).isTrue();
        assertThat(booking.amountPaid()).isEqualByComparingTo("2000");
    }

    @Test
    void declinedPaymentFailsAndReleasesTheRoom() {
        Booking booking = newBooking();

        booking.recordPayment(payment(false));

        assertThat(booking.getStatus()).isEqualTo(PAYMENT_FAILED);
        assertThat(booking.holdsRoom()).isFalse();
        assertThat(booking.amountPaid()).isEqualByComparingTo("0");
    }

    @Test
    void cancellingReleasesTheRoom() {
        Booking booking = newBooking();
        booking.recordPayment(payment(true));

        booking.cancel(new BigDecimal("2000"));

        assertThat(booking.getStatus()).isEqualTo(CANCELLED);
        assertThat(booking.holdsRoom()).isFalse();
        assertThat(booking.getRefundAmount()).isEqualByComparingTo("2000");
    }

    @Test
    void cannotPayAConfirmedBookingAgain() {
        Booking booking = newBooking();
        booking.recordPayment(payment(true));

        assertThatThrownBy(() -> booking.ensurePayable(TODAY)).isInstanceOf(InvalidBookingStateException.class);
        assertThatThrownBy(() -> booking.recordPayment(payment(true))).isInstanceOf(InvalidBookingStateException.class);
    }

    @Test
    void cannotPayACancelledBooking() {
        Booking booking = newBooking();
        booking.cancel(BigDecimal.ZERO);

        assertThatThrownBy(() -> booking.ensurePayable(TODAY)).isInstanceOf(InvalidBookingStateException.class);
    }

    @Test
    void cannotCancelTwiceOrCancelAFailedBooking() {
        Booking cancelled = newBooking();
        cancelled.cancel(BigDecimal.ZERO);
        Booking failed = newBooking();
        failed.recordPayment(payment(false));

        assertThatThrownBy(() -> cancelled.ensureCancellable(TODAY)).isInstanceOf(InvalidBookingStateException.class);
        assertThatThrownBy(() -> failed.ensureCancellable(TODAY)).isInstanceOf(InvalidBookingStateException.class);
    }

    @Test
    void canPayOrCancelUpToTheCheckInDateButNotAfter() {
        Booking booking = newBooking();

        assertThatCode(() -> booking.ensurePayable(CHECK_IN)).doesNotThrowAnyException();
        assertThatCode(() -> booking.ensureCancellable(CHECK_IN)).doesNotThrowAnyException();
        assertThatThrownBy(() -> booking.ensurePayable(CHECK_IN.plusDays(1))).isInstanceOf(InvalidBookingStateException.class);
        assertThatThrownBy(() -> booking.ensureCancellable(CHECK_IN.plusDays(1))).isInstanceOf(InvalidBookingStateException.class);
    }

    @Test
    void rejectsMoreGuestsThanTheRoomFits() {
        assertThatThrownBy(() -> Booking.pending("p1", room, new DateRange(CHECK_IN, CHECK_IN.plusDays(1)), 3, "Ravi", BookingType.FLEXIBLE, BigDecimal.TEN))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void recognisesItsOwnIdempotencyKeyOnly() {
        Booking booking = newBooking();
        booking.recordPayment(payment(true));

        assertThat(booking.wasPaidWith("key-1")).isTrue();
        assertThat(booking.wasPaidWith("key-2")).isFalse();
        assertThat(booking.wasPaidWith(null)).isFalse();
    }

    @Test
    void refundIsCappedToTheAmountPaid() {
        Booking paid = newBooking();
        paid.recordPayment(payment(true));
        Booking unpaid = newBooking();

        assertThat(paid.capRefund(new BigDecimal("5000"))).isEqualByComparingTo("2000");
        assertThat(paid.capRefund(new BigDecimal("-5"))).isEqualByComparingTo("0");
        assertThat(unpaid.capRefund(new BigDecimal("100"))).isEqualByComparingTo("0");
    }

    @Test
    void cannotCancelWithARefundAboveTheAmountPaid() {
        Booking booking = newBooking();
        booking.recordPayment(payment(true));

        assertThatThrownBy(() -> booking.cancel(new BigDecimal("2000.01"))).isInstanceOf(IllegalStateException.class);
        assertThat(booking.getStatus()).isEqualTo(CONFIRMED);
    }
}
