package com.example.honbabapp.api.data.response.payment;

/**
 * 구매 내역 단건 DTO
 *
 * PHP history.php 응답 data.payments[] 항목:
 * {
 *   "payment_id": 12,
 *   "order_id": "TCKT_3_a1b2...",
 *   "product_code": "TICKET_5",
 *   "order_name": "체험단 등록권 5회",
 *   "ticket_count": 5,
 *   "amount": 13500,
 *   "status": "DONE",                        // DONE | FAILED
 *   "confirmed_at": "2026-07-01 12:00:00"    // FAILED면 null 가능
 * }
 */
public class PaymentHistoryItem {

    private int payment_id;
    private String order_id;
    private String product_code;
    private String order_name;
    private int ticket_count;
    private int amount;
    private String status;
    private String confirmed_at;

    public int getPayment_id() {
        return payment_id;
    }

    public String getOrder_id() {
        return order_id;
    }

    public String getProduct_code() {
        return product_code;
    }

    public String getOrder_name() {
        return order_name;
    }

    public int getTicket_count() {
        return ticket_count;
    }

    public int getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public String getConfirmed_at() {
        return confirmed_at;
    }
}
