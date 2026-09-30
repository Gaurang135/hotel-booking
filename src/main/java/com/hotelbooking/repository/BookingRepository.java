package com.hotelbooking.repository;

import com.hotelbooking.domain.Booking;
import com.hotelbooking.exception.NotFoundException;

import java.util.List;
import java.util.Optional;

public interface BookingRepository {

    Booking save(Booking booking);

    Optional<Booking> findById(String id);

    List<Booking> findByRoomTypeId(String roomTypeId);

    default Booking getById(String id) {
        return findById(id).orElseThrow(() -> new NotFoundException("Booking", id));
    }
}
