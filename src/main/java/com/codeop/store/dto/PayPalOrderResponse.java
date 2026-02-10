package com.codeop.store.dto;

public class PayPalOrderResponse {
    private String orderId;
    private String status;

    public PayPalOrderResponse(String orderId, String status) {
        this.orderId = orderId;
        this.status = status;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getStatus() {
        return status;
    }
}
