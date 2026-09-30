package com.hotelbooking.service;

import com.hotelbooking.domain.Booking;
import com.hotelbooking.domain.BookingType;
import com.hotelbooking.domain.DateRange;
import com.hotelbooking.domain.Property;
import com.hotelbooking.domain.RoomType;
import com.hotelbooking.exception.NotFoundException;
import com.hotelbooking.exception.RoomNotAvailableException;
import com.hotelbooking.repository.BookingRepository;
import com.hotelbooking.repository.PropertyRepository;
import com.hotelbooking.service.pricing.PricingStrategy;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

@Service
public class BookingService {

    private final PropertyRepository properties;
    private final BookingRepository bookings;
    private final AvailabilityService availability;
    private final PricingStrategy pricing;
    private final InventoryLock lock;
    private final Clock clock;

    public BookingService(PropertyRepository properties, BookingRepository bookings, AvailabilityService availability,
                          PricingStrategy pricing, InventoryLock lock, Clock clock) {
        this.properties = properties;
        this.bookings = bookings;
        this.availability = availability;
        this.pricing = pricing;
        this.lock = lock;
        this.clock = clock;
    }

    public Booking create(String propertyId, String roomTypeId, DateRange stay, int guests, String guestName,
                          BookingType type) {
        stay.ensureNotInPast(LocalDate.now(clock));
        Property property = properties.getById(propertyId);
        RoomType room = property.findRoomType(roomTypeId)
                .orElseThrow(() -> new NotFoundException("Room type", roomTypeId));
        Booking booking = Booking.pending(propertyId, room, stay, guests, guestName, type, pricing.priceFor(room, stay));

        return lock.withLock(roomTypeId, () -> {
            if (availability.roomsLeft(room, stay) < 1) {
                throw new RoomNotAvailableException(roomTypeId);
            }
            return bookings.save(booking);
        });
    }

    public Booking get(String bookingId) {
        return bookings.getById(bookingId);
    }
}
