package com.hotelbooking.service;

import com.hotelbooking.domain.Booking;
import com.hotelbooking.domain.BookingType;
import com.hotelbooking.domain.Payment;
import com.hotelbooking.domain.PaymentMethod;
import com.hotelbooking.domain.RoomType;
import com.hotelbooking.repository.inmemory.InMemoryBookingRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.hotelbooking.support.TestApp.room;
import static com.hotelbooking.support.TestApp.stay;
import static org.assertj.core.api.Assertions.assertThat;

class AvailabilityServiceTest {

    private final InMemoryBookingRepository bookings = new InMemoryBookingRepository();
    private final AvailabilityService availability = new AvailabilityService(bookings);
    private final RoomType twoRooms = room("deluxe", 2, 2, "1000");

    private Booking booked(RoomType room, int fromDay, int toDay) {
        return bookings.save(Booking.pending("p1", room, stay(fromDay, toDay), 1, "Guest", BookingType.FLEXIBLE, BigDecimal.TEN));
    }

    @Test
    void allRoomsAreFreeWithNoBookings() {
        assertThat(availability.roomsLeft(twoRooms, stay(1, 3))).isEqualTo(2);
    }

    @Test
    void overlappingBookingTakesARoom() {
        booked(twoRooms, 1, 3);

        assertThat(availability.roomsLeft(twoRooms, stay(2, 4))).isEqualTo(1);
    }

    @Test
    void backToBackBookingDoesNotTakeARoom() {
        booked(twoRooms, 1, 3);

        assertThat(availability.roomsLeft(twoRooms, stay(3, 5))).isEqualTo(2);
    }

    @Test
    void countsTheBusiestNightNotTheNumberOfOverlappingBookings() {
        booked(twoRooms, 1, 2);
        booked(twoRooms, 3, 4);

        assertThat(availability.roomsLeft(twoRooms, stay(1, 4))).isEqualTo(1);
    }

    @Test
    void cancelledAndFailedBookingsFreeTheirRooms() {
        booked(twoRooms, 1, 3).cancel(BigDecimal.ZERO);
        booked(twoRooms, 1, 3).recordPayment(new Payment(PaymentMethod.UPI, BigDecimal.TEN, false, null, null));

        assertThat(availability.roomsLeft(twoRooms, stay(1, 3))).isEqualTo(2);
    }

    @Test
    void ignoresBookingsOfOtherRoomTypes() {
        booked(room("suite", 2, 1, "2000"), 1, 3);

        assertThat(availability.roomsLeft(twoRooms, stay(1, 3))).isEqualTo(2);
    }
}
