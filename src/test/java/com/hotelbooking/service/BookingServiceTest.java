package com.hotelbooking.service;

import com.hotelbooking.domain.Booking;
import com.hotelbooking.domain.BookingType;
import com.hotelbooking.domain.BookingStatus;
import com.hotelbooking.exception.InvalidInputException;
import com.hotelbooking.exception.NotFoundException;
import com.hotelbooking.exception.RoomNotAvailableException;
import com.hotelbooking.support.TestApp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.hotelbooking.domain.PropertyType.HOTEL;
import static com.hotelbooking.support.TestApp.room;
import static com.hotelbooking.support.TestApp.stay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingServiceTest {

    private final TestApp app = new TestApp();

    @BeforeEach
    void setUp() {
        app.addProperty("p1", HOTEL, "Bengaluru", "Koramangala", 4, Set.of(), room("single", 2, 1, "1000"));
    }

    @Test
    void createsAPendingBookingPricedForTheWholeStay() {
        Booking booking = app.bookingService.create("p1", "single", stay(1, 4), 2, "Ravi", BookingType.FLEXIBLE);

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(booking.getTotalPrice()).isEqualByComparingTo("3000");
    }

    @Test
    void rejectsBookingWhenTheLastRoomIsTaken() {
        app.book("p1", "single", stay(1, 3));

        assertThatThrownBy(() -> app.book("p1", "single", stay(2, 4))).isInstanceOf(RoomNotAvailableException.class);
    }

    @Test
    void roomIsBookableAgainAfterCancellation() {
        Booking first = app.book("p1", "single", stay(1, 3));
        app.cancellationService.cancel(first.getId());

        assertThat(app.book("p1", "single", stay(1, 3)).getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
    }

    @Test
    void rejectsCheckInInThePast() {
        assertThatThrownBy(() -> app.book("p1", "single", stay(-1, 1))).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void unknownPropertyOrRoomTypeIsNotFound() {
        assertThatThrownBy(() -> app.book("missing", "single", stay(1, 2))).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> app.book("p1", "missing", stay(1, 2))).isInstanceOf(NotFoundException.class);
    }

    @Test
    void tooManyGuestsIsInvalidInputEvenWhenSoldOut() {
        app.book("p1", "single", stay(1, 3));

        assertThatThrownBy(() -> app.bookingService.create("p1", "single", stay(1, 3), 5, "Ravi", BookingType.FLEXIBLE))
                .isInstanceOf(InvalidInputException.class);
    }

    @RepeatedTest(5)
    void onlyOneOfManyConcurrentRequestsGetsTheLastRoom() throws Exception {
        int requests = 20;
        ExecutorService pool = Executors.newFixedThreadPool(requests);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            results.add(pool.submit(() -> {
                start.await();
                try {
                    app.book("p1", "single", stay(1, 3));
                    return true;
                } catch (RoomNotAvailableException e) {
                    return false;
                }
            }));
        }

        start.countDown();
        int successes = 0;
        for (Future<Boolean> result : results) {
            successes += result.get() ? 1 : 0;
        }
        pool.shutdown();

        assertThat(successes).isEqualTo(1);
        assertThat(app.bookings.findByRoomTypeId("single")).hasSize(1);
    }
}
