package com.hotelbooking.service.payment;

public record GatewayResponse(boolean success, String reference) {

    public static GatewayResponse success(String reference) {
        return new GatewayResponse(true, reference);
    }

    public static GatewayResponse declined() {
        return new GatewayResponse(false, null);
    }
}
