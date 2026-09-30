package com.hotelbooking.service.payment;

import com.hotelbooking.domain.PaymentMethod;
import com.hotelbooking.exception.InvalidInputException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class UpiPaymentProcessor implements PaymentProcessor {

    private final PaymentGateway gateway;

    public UpiPaymentProcessor(PaymentGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.UPI;
    }

    @Override
    public GatewayResponse pay(BigDecimal amount, Map<String, String> details) {
        String vpa = PaymentProcessor.requireDetail(details, "vpa");
        if (!vpa.contains("@")) {
            throw new InvalidInputException("vpa must look like name@bank");
        }
        return gateway.charge(method(), vpa, amount);
    }
}
