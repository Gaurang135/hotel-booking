package com.hotelbooking.domain;

import com.hotelbooking.exception.InvalidInputException;

public final class Require {

    private Require() {
    }

    public static String text(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidInputException(field + " is required");
        }
        return value.trim();
    }

    public static void that(boolean condition, String message) {
        if (!condition) {
            throw new InvalidInputException(message);
        }
    }
}
