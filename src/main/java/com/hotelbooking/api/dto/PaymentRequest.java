package com.hotelbooking.api.dto;

import com.hotelbooking.domain.PaymentMethod;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record PaymentRequest(@NotNull PaymentMethod method, @NotNull Map<String, String> details) {
}
