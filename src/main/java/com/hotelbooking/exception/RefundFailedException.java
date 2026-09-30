package com.hotelbooking.exception;

public class RefundFailedException extends RuntimeException {

    public RefundFailedException(String bookingId) {
        super("The payment provider declined the refund for booking " + bookingId + "; the booking was not cancelled");
    }
}
