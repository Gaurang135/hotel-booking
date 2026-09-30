package com.hotelbooking.service;

import com.hotelbooking.domain.Booking;
import com.hotelbooking.domain.BookingStatus;
import com.hotelbooking.domain.BookingType;
import com.hotelbooking.domain.RoomType;
import com.hotelbooking.exception.InvalidBookingStateException;
import com.hotelbooking.exception.RefundFailedException;
import com.hotelbooking.service.refund.FlexibleRefundPolicy;
import com.hotelbooking.service.refund.NonRefundableRefundPolicy;
import com.hotelbooking.service.refund.RefundPolicy;
import com.hotelbooking.support.TestApp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static com.hotelbooking.domain.PropertyType.HOTEL;
import static com.hotelbooking.support.TestApp.TODAY;
import static com.hotelbooking.support.TestApp.room;
import static com.hotelbooking.support.TestApp.stay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CancellationServiceTest {

    private final TestApp app = new TestApp();
    private final RoomType single = room("single", 2, 1, "1000");

    @BeforeEach
    void setUp() {
        app.addProperty("p1", HOTEL, "Bengaluru", null, 4, Set.of(), single);
    }

    @Test
    void cancellingWellAheadRefundsInFullAndReleasesTheRoom() {
        Booking booking = app.payWithUpi(app.book("p1", "single", stay(10, 12)));

        Booking cancelled = app.cancellationService.cancel(booking.getId());

        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(cancelled.getRefundAmount()).isEqualByComparingTo("2000");
        assertThat(app.gateway.refunds).hasSize(1);
        assertThat(app.availability.roomsLeft(single, stay(10, 12))).isEqualTo(1);
    }

    @Test
    void cancellingCloseToCheckInRefundsHalf() {
        Booking booking = app.payWithUpi(app.book("p1", "single", stay(10, 12)));
        app.clock.setToday(TODAY.plusDays(7));

        assertThat(app.cancellationService.cancel(booking.getId()).getRefundAmount()).isEqualByComparingTo("1000");
        assertThat(app.gateway.refunds.getFirst()).isEqualByComparingTo("1000");
    }

    @Test
    void cancellingAnUnpaidBookingRefundsNothingAndCallsNoGateway() {
        Booking booking = app.book("p1", "single", stay(10, 12));

        Booking cancelled = app.cancellationService.cancel(booking.getId());

        assertThat(cancelled.getRefundAmount()).isEqualByComparingTo("0");
        assertThat(app.gateway.refunds).isEmpty();
    }

    @Test
    void cancellingTwiceIsRejectedAndRefundsOnce() {
        Booking booking = app.payWithUpi(app.book("p1", "single", stay(10, 12)));
        app.cancellationService.cancel(booking.getId());

        assertThatThrownBy(() -> app.cancellationService.cancel(booking.getId()))
                .isInstanceOf(InvalidBookingStateException.class);
        assertThat(app.gateway.refunds).hasSize(1);
    }

    @Test
    void cancellingAfterTheCheckInDateIsRejected() {
        Booking booking = app.payWithUpi(app.book("p1", "single", stay(2, 4)));
        app.clock.setToday(TODAY.plusDays(3));

        assertThatThrownBy(() -> app.cancellationService.cancel(booking.getId()))
                .isInstanceOf(InvalidBookingStateException.class);
    }

    @Test
    void nonRefundableBookingRefundsNothingButStillReleasesTheRoom() {
        Booking booking = app.payWithUpi(app.book("p1", "single", stay(10, 12), BookingType.NON_REFUNDABLE));

        Booking cancelled = app.cancellationService.cancel(booking.getId());

        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(cancelled.getRefundAmount()).isEqualByComparingTo("0");
        assertThat(app.gateway.refunds).isEmpty();
        assertThat(app.availability.roomsLeft(single, stay(10, 12))).isEqualTo(1);
    }

    @Test
    void declinedRefundLeavesTheBookingConfirmedAndTheRoomHeld() {
        Booking booking = app.payWithUpi(app.book("p1", "single", stay(10, 12)));
        app.gateway.declineRefunds = true;

        assertThatThrownBy(() -> app.cancellationService.cancel(booking.getId()))
                .isInstanceOf(RefundFailedException.class);
        assertThat(app.bookingService.get(booking.getId()).getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(app.availability.roomsLeft(single, stay(10, 12))).isEqualTo(0);
    }

    @Test
    void everyBookingTypeMustHaveARefundPolicy() {
        assertThatThrownBy(() -> new CancellationService(app.bookings, List.of(new FlexibleRefundPolicy()),
                app.gateway, app.lock, app.clock))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("NON_REFUNDABLE");
    }

    @Test
    void aPolicyAskingForMoreThanWasPaidIsCappedAndNeverCrashesOnUnpaidBookings() {
        RefundPolicy greedy = new RefundPolicy() {
            @Override
            public BookingType bookingType() {
                return BookingType.FLEXIBLE;
            }

            @Override
            public BigDecimal refundFor(Booking booking, LocalDate cancelledOn) {
                return new BigDecimal("1000000");
            }
        };
        CancellationService service = new CancellationService(app.bookings,
                List.of(greedy, new NonRefundableRefundPolicy()), app.gateway, app.lock, app.clock);
        Booking paid = app.payWithUpi(app.book("p1", "single", stay(10, 12)));
        Booking unpaid = app.book("p1", "single", stay(20, 22));

        assertThat(service.cancel(paid.getId()).getRefundAmount()).isEqualByComparingTo("2000");
        assertThat(service.cancel(unpaid.getId()).getRefundAmount()).isEqualByComparingTo("0");
        assertThat(app.gateway.refunds).containsExactly(new BigDecimal("2000.00"));
    }
}
