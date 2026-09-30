package com.hotelbooking.service;

import com.hotelbooking.domain.Booking;
import com.hotelbooking.domain.Payment;
import com.hotelbooking.domain.PaymentMethod;
import com.hotelbooking.repository.BookingRepository;
import com.hotelbooking.service.payment.GatewayResponse;
import com.hotelbooking.service.payment.PaymentProcessor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final BookingRepository bookings;
    private final Map<PaymentMethod, PaymentProcessor> processors;
    private final InventoryLock lock;
    private final Clock clock;

    public PaymentService(BookingRepository bookings, List<PaymentProcessor> processors, InventoryLock lock, Clock clock) {
        this.bookings = bookings;
        this.processors = processors.stream()
                .collect(Collectors.toMap(PaymentProcessor::method, Function.identity()));
        for (PaymentMethod method : PaymentMethod.values()) {
            if (!this.processors.containsKey(method)) {
                throw new IllegalStateException("No PaymentProcessor for payment method " + method);
            }
        }
        this.lock = lock;
        this.clock = clock;
    }

    public Booking pay(String bookingId, PaymentMethod method, Map<String, String> details, String idempotencyKey) {
        String roomTypeId = bookings.getById(bookingId).getRoomTypeId();
        return lock.withLock(roomTypeId, () -> {
            Booking booking = bookings.getById(bookingId);
            if (booking.wasPaidWith(idempotencyKey)) {
                return booking;
            }
            booking.ensurePayable(LocalDate.now(clock));
            BigDecimal amount = booking.getTotalPrice();
            GatewayResponse response = processors.get(method).pay(amount, details);
            booking.recordPayment(new Payment(method, amount, response.success(), response.reference(), idempotencyKey));
            return bookings.save(booking);
        });
    }
}
