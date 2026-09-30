package com.hotelbooking.service.payment;

import com.hotelbooking.domain.PaymentMethod;
import com.hotelbooking.exception.InvalidInputException;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentProcessor {

    PaymentMethod method();

    GatewayResponse pay(BigDecimal amount, Map<String, String> details);

    static String requireDetail(Map<String, String> details, String key) {
        String value = details.get(key);
        if (value == null || value.isBlank()) {
            throw new InvalidInputException("Payment details must include '" + key + "'");
        }
        return value.trim();
    }
}
