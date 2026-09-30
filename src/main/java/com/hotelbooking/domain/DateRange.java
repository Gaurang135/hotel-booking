package com.hotelbooking.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** A stay covering the nights checkIn .. checkOut-1 (checkOut is exclusive). */
public record DateRange(LocalDate checkIn, LocalDate checkOut) {

    public static final int MAX_NIGHTS = 30;

    public DateRange {
        Require.that(checkIn != null && checkOut != null, "checkIn and checkOut are required");
        Require.that(checkOut.isAfter(checkIn), "checkOut must be after checkIn");
        Require.that(ChronoUnit.DAYS.between(checkIn, checkOut) <= MAX_NIGHTS,
                "A stay can be at most " + MAX_NIGHTS + " nights");
    }

    public int nights() {
        return (int) ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public List<LocalDate> eachNight() {
        return checkIn.datesUntil(checkOut).toList();
    }

    public boolean overlaps(DateRange other) {
        return checkIn.isBefore(other.checkOut) && other.checkIn.isBefore(checkOut);
    }

    public boolean contains(LocalDate night) {
        return !night.isBefore(checkIn) && night.isBefore(checkOut);
    }

    public void ensureNotInPast(LocalDate today) {
        Require.that(!checkIn.isBefore(today), "checkIn cannot be in the past");
    }
}
