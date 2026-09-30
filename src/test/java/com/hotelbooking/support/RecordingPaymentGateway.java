package com.hotelbooking.support;

import com.hotelbooking.domain.PaymentMethod;
import com.hotelbooking.service.payment.GatewayResponse;
import com.hotelbooking.service.payment.MockPaymentGateway;
import com.hotelbooking.service.payment.PaymentGateway;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/** The mock gateway plus counters, so tests can assert how often money moved. */
public class RecordingPaymentGateway implements PaymentGateway {

    private final MockPaymentGateway delegate = new MockPaymentGateway();
    public final AtomicInteger charges = new AtomicInteger();
    public final List<BigDecimal> refunds = new CopyOnWriteArrayList<>();
    public volatile boolean declineRefunds;

    @Override
    public GatewayResponse charge(PaymentMethod method, String instrument, BigDecimal amount) {
        charges.incrementAndGet();
        return delegate.charge(method, instrument, amount);
    }

    @Override
    public GatewayResponse refund(String paymentReference, BigDecimal amount) {
        if (declineRefunds) {
            return GatewayResponse.declined();
        }
        refunds.add(amount);
        return delegate.refund(paymentReference, amount);
    }
}
