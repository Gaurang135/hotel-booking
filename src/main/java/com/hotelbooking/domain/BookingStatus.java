package com.hotelbooking.domain;

public enum BookingStatus {
    PENDING_PAYMENT, CONFIRMED, PAYMENT_FAILED, CANCELLED;

    public boolean canMoveTo(BookingStatus next) {
        return switch (this) {
            case PENDING_PAYMENT -> next == CONFIRMED || next == PAYMENT_FAILED || next == CANCELLED;
            case CONFIRMED -> next == CANCELLED;
            case PAYMENT_FAILED, CANCELLED -> false;
        };
    }

    public boolean holdsRoom() {
        return switch (this) {
            case PENDING_PAYMENT, CONFIRMED -> true;
            case PAYMENT_FAILED, CANCELLED -> false;
        };
    }
}
