package com.hotelbooking.domain;

import java.math.BigDecimal;

public record Payment(PaymentMethod method, BigDecimal amount, boolean success, String reference, String idempotencyKey) {
}
