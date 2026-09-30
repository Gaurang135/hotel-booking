package com.hotelbooking.service;

import java.util.function.Supplier;

/** Runs an action while holding a room type's lock, so "check availability, then save" can't interleave. */
public interface InventoryLock {

    <T> T withLock(String roomTypeId, Supplier<T> action);
}
