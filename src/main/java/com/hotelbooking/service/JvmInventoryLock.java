package com.hotelbooking.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/** In-process locks: correct for a single app instance; a database lock would be another implementation. */
@Component
public class JvmInventoryLock implements InventoryLock {

    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public <T> T withLock(String roomTypeId, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(roomTypeId, id -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
