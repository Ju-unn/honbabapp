package com.example.honbabapp.api.data.request.payment;

/**
 * 등록권 결제 승인 요청 DTO
 *
 * 토스페이먼츠 결제창(SDK) 결제 성공 콜백에서 돌아온
 * paymentKey, orderId, amount를 그대로 서버에 전달한다.
 */
public class ConfirmPaymentRequest {

    private String payment_key;
    private String order_id;
    private int amount;

    public ConfirmPaymentRequest(String paymentKey, String orderId, int amount) {
        this.payment_key = paymentKey;
        this.order_id = orderId;
        this.amount = amount;
    }

    public String getPayment_key() {
        return payment_key;
    }

    public void setPayment_key(String paymentKey) {
        this.payment_key = paymentKey;
    }

    public String getOrder_id() {
        return order_id;
    }

    public void setOrder_id(String orderId) {
        this.order_id = orderId;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}