package com.hotelbooking.support;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** A clock tests can move to another day. */
public class MutableClock extends Clock {

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private volatile Instant instant;

    public MutableClock(LocalDate today) {
        setToday(today);
    }

    public void setToday(LocalDate today) {
        instant = today.atTime(12, 0).atZone(ZONE).toInstant();
    }

    @Override
    public ZoneId getZone() {
        return ZONE;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(instant, zone);
    }

    @Override
    public Instant instant() {
        return instant;
    }
}
