package com.hotelbooking.service;

import com.hotelbooking.domain.Booking;
import com.hotelbooking.domain.BookingStatus;
import com.hotelbooking.domain.PaymentMethod;
import com.hotelbooking.exception.InvalidBookingStateException;
import com.hotelbooking.exception.InvalidInputException;
import com.hotelbooking.service.payment.CardPaymentProcessor;
import com.hotelbooking.support.TestApp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.hotelbooking.domain.PropertyType.HOTEL;
import static com.hotelbooking.support.TestApp.TODAY;
import static com.hotelbooking.support.TestApp.room;
import static com.hotelbooking.support.TestApp.stay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentServiceTest {

    private final TestApp app = new TestApp();
    private Booking booking;

    @BeforeEach
    void setUp() {
        app.addProperty("p1", HOTEL, "Bengaluru", null, 4, Set.of(), room("single", 2, 1, "1000"));
        booking = app.book("p1", "single", stay(5, 7));
    }

    private Booking payUpi(String vpa, String idempotencyKey) {
        return app.paymentService.pay(booking.getId(), PaymentMethod.UPI, Map.of("vpa", vpa), idempotencyKey);
    }

    @Test
    void successfulPaymentConfirmsTheBookingForTheServerSideAmount() {
        Booking paid = payUpi("ravi@okaxis", null);

        assertThat(paid.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(paid.amountPaid()).isEqualByComparingTo("2000");
        assertThat(app.gateway.charges).hasValue(1);
    }

    @Test
    void declinedPaymentFailsTheBookingAndReleasesTheRoom() {
        Booking failed = payUpi("fail@upi", null);

        assertThat(failed.getStatus()).isEqualTo(BookingStatus.PAYMENT_FAILED);
        assertThat(app.book("p1", "single", stay(5, 7)).getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThatThrownBy(() -> payUpi("ravi@okaxis", null)).isInstanceOf(InvalidBookingStateException.class);
    }

    @Test
    void payingAConfirmedBookingAgainIsRejectedWithoutCharging() {
        payUpi("ravi@okaxis", null);

        assertThatThrownBy(() -> payUpi("ravi@okaxis", null)).isInstanceOf(InvalidBookingStateException.class);
        assertThat(app.gateway.charges).hasValue(1);
    }

    @Test
    void retryWithTheSameIdempotencyKeyChargesOnce() {
        payUpi("ravi@okaxis", "key-1");
        Booking retried = payUpi("ravi@okaxis", "key-1");

        assertThat(retried.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(app.gateway.charges).hasValue(1);
    }

    @Test
    void payingAfterTheCheckInDateIsRejectedWithoutCharging() {
        app.clock.setToday(TODAY.plusDays(6));

        assertThatThrownBy(() -> payUpi("ravi@okaxis", null)).isInstanceOf(InvalidBookingStateException.class);
        assertThat(app.gateway.charges).hasValue(0);
    }

    @Test
    void invalidDetailsAreRejectedAndTheBookingStaysPending() {
        assertThatThrownBy(() -> payUpi("not-a-vpa", null)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> app.paymentService.pay(booking.getId(), PaymentMethod.CARD, Map.of(), null))
                .isInstanceOf(InvalidInputException.class);

        assertThat(app.bookingService.get(booking.getId()).getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(app.gateway.charges).hasValue(0);
    }

    @Test
    void eachMethodIsHandledByItsOwnProcessor() {
        Booking paid = app.paymentService.pay(booking.getId(), PaymentMethod.WALLET, Map.of("walletId", "w-42"), null);

        assertThat(paid.getPayment().method()).isEqualTo(PaymentMethod.WALLET);
        assertThat(paid.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
    }

    @Test
    void everyPaymentMethodMustHaveAProcessor() {
        assertThatThrownBy(() -> new PaymentService(app.bookings, List.of(new CardPaymentProcessor(app.gateway)),
                app.lock, app.clock))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("UPI");
    }
}
