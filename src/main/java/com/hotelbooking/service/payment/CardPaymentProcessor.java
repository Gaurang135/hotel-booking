package com.hotelbooking.service.payment;

import com.hotelbooking.domain.PaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class CardPaymentProcessor implements PaymentProcessor {

    private final PaymentGateway gateway;

    public CardPaymentProcessor(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.CARD;
    }

    @Override
    public GatewayResponse pay(BigDecimal amount, Map<String, String> details) {
        String cardToken = PaymentProcessor.requireDetail(details, "cardToken");
        return gateway.charge(method(), cardToken, amount);
    }
}
