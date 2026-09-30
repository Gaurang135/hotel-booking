package com.hotelbooking.api.dto;

import com.hotelbooking.domain.Booking;
import com.hotelbooking.domain.BookingStatus;
import com.hotelbooking.domain.BookingType;
import com.hotelbooking.domain.Payment;
import com.hotelbooking.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BookingResponse(
        String id,
        String propertyId,
        String roomTypeId,
        String guestName,
        int guests,
        LocalDate checkIn,
        LocalDate checkOut,
        int nights,
        BookingType bookingType,
        BigDecimal totalPrice,
        BookingStatus status,
        PaymentMethod paymentMethod,
        String paymentReference,
        BigDecimal refundAmount) {

    public static BookingResponse from(Booking booking) {
        Payment payment = booking.getPayment();
        return new BookingResponse(
                booking.getId(),
                booking.getPropertyId(),
                booking.getRoomTypeId(),
                booking.getGuestName(),
                booking.getGuests(),
                booking.getStay().checkIn(),
                booking.getStay().checkOut(),
                booking.getStay().nights(),
                booking.getType(),
                booking.getTotalPrice(),
                booking.getStatus(),
                payment == null ? null : payment.method(),
                payment == null ? null : payment.reference(),
                booking.getRefundAmount());
    }
}
