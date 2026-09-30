package com.hotelbooking.exception;

public class RoomNotAvailableException extends RuntimeException {

    public RoomNotAvailableException(String roomTypeId) {
        super("No rooms left for room type " + roomTypeId + " on the requested dates");
    }
}
