package com.hotelbooking.service.payment;

import com.hotelbooking.domain.PaymentMethod;

import java.math.BigDecimal;

/** Our contract with the external payment provider. */
public interface PaymentGateway {

    GatewayResponse charge(PaymentMethod method, String instrument, BigDecimal amount);

    GatewayResponse refund(String paymentReference, BigDecimal amount);
}
