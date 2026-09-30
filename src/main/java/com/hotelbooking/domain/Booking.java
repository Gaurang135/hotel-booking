package com.hotelbooking.domain;

import com.hotelbooking.exception.InvalidBookingStateException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static com.hotelbooking.domain.BookingStatus.CANCELLED;
import static com.hotelbooking.domain.BookingStatus.CONFIRMED;
import static com.hotelbooking.domain.BookingStatus.PAYMENT_FAILED;
import static com.hotelbooking.domain.BookingStatus.PENDING_PAYMENT;

public class Booking {

    private final String id;
    private final String propertyId;
    private final String roomTypeId;
    private final String guestName;
    private final int guests;
    private final DateRange stay;
    private final BookingType type;
    private final BigDecimal totalPrice;
    private BookingStatus status = PENDING_PAYMENT;
    private Payment payment;
    private BigDecimal refundAmount = BigDecimal.ZERO;

    private Booking(String id, String propertyId, String roomTypeId, String guestName, int guests,
                    DateRange stay, BookingType type, BigDecimal totalPrice) {
        this.id = id;
        this.propertyId = propertyId;
        this.roomTypeId = roomTypeId;
        this.guestName = guestName;
        this.guests = guests;
        this.stay = stay;
        this.type = type;
        this.totalPrice = totalPrice;
    }

    public static Booking pending(String propertyId, RoomType room, DateRange stay, int guests,
                                  String guestName, BookingType type, BigDecimal totalPrice) {
        Require.that(guests >= 1, "guests must be at least 1");
        Require.that(room.canFit(guests), room.name() + " fits at most " + room.maxGuests() + " guests");
        Require.that(type != null, "bookingType is required");
        return new Booking(UUID.randomUUID().toString(), propertyId, room.id(),
                Require.text(guestName, "guestName"), guests, stay, type, totalPrice);
    }

    public boolean holdsRoom() {
        return status.holdsRoom();
    }

    public void ensurePayable(LocalDate today) {
        ensureCanMoveBeforeCheckIn(CONFIRMED, today);
    }

    public void ensureCancellable(LocalDate today) {
        ensureCanMoveBeforeCheckIn(CANCELLED, today);
    }

    public void recordPayment(Payment payment) {
        moveTo(payment.success() ? CONFIRMED : PAYMENT_FAILED);
        this.payment = payment;
    }

    public void cancel(BigDecimal refund) {
        if (refund.signum() < 0 || refund.compareTo(amountPaid()) > 0) {
            throw new IllegalStateException("Refund must be between 0 and the amount paid");
        }
        moveTo(CANCELLED);
        this.refundAmount = refund;
    }

    /** Limits a proposed refund to 0..amount paid, so no policy can return more than was charged. */
    public BigDecimal capRefund(BigDecimal proposed) {
        return proposed.max(BigDecimal.ZERO).min(amountPaid());
    }

    public BigDecimal amountPaid() {
        return payment != null && payment.success() ? payment.amount() : BigDecimal.ZERO;
    }

    public boolean wasPaidWith(String idempotencyKey) {
        return idempotencyKey != null && payment != null && idempotencyKey.equals(payment.idempotencyKey());
    }

    private void ensureCanMoveBeforeCheckIn(BookingStatus next, LocalDate today) {
        ensureCanMoveTo(next);
        if (today.isAfter(stay.checkIn())) {
            throw new InvalidBookingStateException("Booking " + id + ": not allowed after the check-in date");
        }
    }

    private void ensureCanMoveTo(BookingStatus next) {
        if (!status.canMoveTo(next)) {
            throw new InvalidBookingStateException("Booking " + id + " is " + status + " and cannot become " + next);
        }
    }

    private void moveTo(BookingStatus next) {
        ensureCanMoveTo(next);
        this.status = next;
    }

    public String getId() {
        return id;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public String getRoomTypeId() {
        return roomTypeId;
    }

    public String getGuestName() {
        return guestName;
    }

    public int getGuests() {
        return guests;
    }

    public DateRange getStay() {
        return stay;
    }

    public BookingType getType() {
        return type;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Payment getPayment() {
        return payment;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }
}
