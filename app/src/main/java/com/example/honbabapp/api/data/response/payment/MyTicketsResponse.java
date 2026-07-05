package com.example.honbabapp.api.data.response.payment;

/**
 * 등록권 잔액 조회 응답 DTO
 *
 * PHP my_tickets.php 응답 데이터:
 * { "ticket_balance": 3 }
 */
public class MyTicketsResponse {

    private int ticket_balance;

    public int getTicket_balance() {
        return ticket_balance;
    }
}