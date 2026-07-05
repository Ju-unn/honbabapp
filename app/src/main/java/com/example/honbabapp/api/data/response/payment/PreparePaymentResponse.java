package com.example.honbabapp.api.data.response.payment;

/**
 * 등록권 결제 준비 응답 DTO
 *
 * PHP prepare.php 응답 데이터:
 * {
 *   "order_id": "TCKT_12_a1b2c3d4e5f6...",
 *   "amount": 3000,
 *   "order_name": "체험단 등록권 1회"
 * }
 */
public class PreparePaymentResponse {

    private String order_id;
    private int amount;
    private String order_name;

    public String getOrder_id() {
        return order_id;
    }

    public int getAmount() {
        return amount;
    }

    public String getOrder_name() {
        return order_name;
    }
}