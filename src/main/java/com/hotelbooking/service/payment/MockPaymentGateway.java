package com.hotelbooking.service.payment;

import com.hotelbooking.domain.PaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/** Fake provider: instruments starting with "fail" are declined, everything else succeeds. */
@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public GatewayResponse charge(PaymentMethod method, String instrument, BigDecimal amount) {
        if (instrument.toLowerCase().startsWith("fail")) {
            return GatewayResponse.declined();
        }
        return GatewayResponse.success("pay_" + UUID.randomUUID());
    }

    @Override
    public GatewayResponse refund(String paymentReference, BigDecimal amount) {
        return GatewayResponse.success("rfnd_" + UUID.randomUUID());
    }
}
