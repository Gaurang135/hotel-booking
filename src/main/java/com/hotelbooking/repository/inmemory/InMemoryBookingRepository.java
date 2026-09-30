package com.hotelbooking.repository.inmemory;

import com.hotelbooking.domain.Booking;
import com.hotelbooking.repository.BookingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryBookingRepository implements BookingRepository {

    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();

    @Override
    public Booking save(Booking booking) {
        bookings.put(booking.getId(), booking);
        return booking;
    }

    @Override
    public Optional<Booking> findById(String id) {
        return Optional.ofNullable(bookings.get(id));
    }

    @Override
    public List<Booking> findByRoomTypeId(String roomTypeId) {
        return bookings.values().stream()
                .filter(booking -> booking.getRoomTypeId().equals(roomTypeId))
                .toList();
    }
}
