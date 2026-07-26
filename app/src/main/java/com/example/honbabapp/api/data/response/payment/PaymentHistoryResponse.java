package com.example.honbabapp.api.data.response.payment;

import java.util.List;

/**
 * 구매 내역 조회 응답 DTO
 *
 * PHP history.php 응답 데이터:
 * {
 *   "payments": [ ... ],
 *   "page": 1,
 *   "limit": 20,
 *   "total_count": 3,
 *   "has_more": false
 * }
 */
public class PaymentHistoryResponse {

    private List<PaymentHistoryItem> payments;
    private int page;
    private int limit;
    private int total_count;
    private boolean has_more;

    public List<PaymentHistoryItem> getPayments() {
        return payments;
    }

    public int getPage() {
        return page;
    }

    public int getLimit() {
        return limit;
    }

    public int getTotal_count() {
        return total_count;
    }

    public boolean isHas_more() {
        return has_more;
    }
}
