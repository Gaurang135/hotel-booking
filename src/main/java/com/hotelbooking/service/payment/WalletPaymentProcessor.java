package com.hotelbooking.service.payment;

import com.hotelbooking.domain.PaymentMethod;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class WalletPaymentProcessor implements PaymentProcessor {

    private final PaymentGateway gateway;

    public WalletPaymentProcessor(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.WALLET;
    }

    @Override
    public GatewayResponse pay(BigDecimal amount, Map<String, String> details) {
        String walletId = PaymentProcessor.requireDetail(details, "walletId");
        return gateway.charge(method(), walletId, amount);
    }
}
