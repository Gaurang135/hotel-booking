package com.hotelbooking.service;

import com.hotelbooking.domain.Booking;
import com.hotelbooking.domain.DateRange;
import com.hotelbooking.domain.RoomType;
import com.hotelbooking.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AvailabilityService {

    private final BookingRepository bookings;

    public AvailabilityService(BookingRepository bookings) {
        this.bookings = bookings;
    }

    /** Free rooms = total rooms minus the rooms taken on the busiest night of the stay. */
    public int roomsLeft(RoomType room, DateRange stay) {
        List<DateRange> taken = bookings.findByRoomTypeId(room.id()).stream()
                .filter(Booking::holdsRoom)
                .map(Booking::getStay)
                .filter(stay::overlaps)
                .toList();
        int roomsTakenOnBusiestNight = 0;
        for (LocalDate night : stay.eachNight()) {
            int used = (int) taken.stream().filter(booked -> booked.contains(night)).count();
            roomsTakenOnBusiestNight = Math.max(roomsTakenOnBusiestNight, used);
        }
        return room.totalRooms() - roomsTakenOnBusiestNight;
    }
}
