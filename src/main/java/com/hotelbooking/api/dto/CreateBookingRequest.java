package com.hotelbooking.api.dto;

import com.hotelbooking.domain.BookingType;
import com.hotelbooking.domain.DateRange;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateBookingRequest(
        @NotBlank String propertyId,
        @NotBlank String roomTypeId,
        @NotNull LocalDate checkIn,
        @NotNull LocalDate checkOut,
        @NotNull Integer guests,
        @NotBlank String guestName,
        BookingType bookingType) {

    public CreateBookingRequest {
        bookingType = bookingType == null ? BookingType.FLEXIBLE : bookingType;
    }

    public DateRange stay() {
        return new DateRange(checkIn, checkOut);
    }
}
