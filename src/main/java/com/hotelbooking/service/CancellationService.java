package com.hotelbooking.service;

import com.hotelbooking.domain.Booking;
import com.hotelbooking.domain.BookingType;
import com.hotelbooking.exception.RefundFailedException;
import com.hotelbooking.repository.BookingRepository;
import com.hotelbooking.service.payment.PaymentGateway;
import com.hotelbooking.service.refund.RefundPolicy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CancellationService {

    private final BookingRepository bookings;
    private final Map<BookingType, RefundPolicy> refundPolicies;
    private final PaymentGateway gateway;
    private final InventoryLock lock;
    private final Clock clock;

    public CancellationService(BookingRepository bookings, List<RefundPolicy> refundPolicies, PaymentGateway gateway,
                               InventoryLock lock, Clock clock) {
        this.bookings = bookings;
        this.refundPolicies = refundPolicies.stream()
                .collect(Collectors.toMap(RefundPolicy::bookingType, Function.identity()));
        for (BookingType type : BookingType.values()) {
            if (!this.refundPolicies.containsKey(type)) {
                throw new IllegalStateException("No RefundPolicy for booking type " + type);
            }
        }
        this.gateway = gateway;
        this.lock = lock;
        this.clock = clock;
    }

    public Booking cancel(String bookingId) {
        String roomTypeId = bookings.getById(bookingId).getRoomTypeId();
        return lock.withLock(roomTypeId, () -> {
            Booking booking = bookings.getById(bookingId);
            LocalDate today = LocalDate.now(clock);
            booking.ensureCancellable(today);
            BigDecimal refund = booking.capRefund(refundPolicies.get(booking.getType()).refundFor(booking, today));
            if (refund.signum() > 0 && !gateway.refund(booking.getPayment().reference(), refund).success()) {
                throw new RefundFailedException(booking.getId());
            }
            booking.cancel(refund);
            return bookings.save(booking);
        });
    }
}
