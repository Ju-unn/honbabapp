package com.example.honbabapp.api.data.response.payment;

/**
 * 등록권 결제 승인 응답 DTO
 *
 * PHP confirm.php 응답 데이터:
 * { "ticket_count": 1 }
 */
public class ConfirmPaymentResponse {

    private int ticket_count;

    public int getTicket_count() {
        return ticket_count;
    }
}