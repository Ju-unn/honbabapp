package com.example.honbabapp.api.data.request.payment;

/**
 * 등록권 결제 준비 요청 DTO
 *
 * PHP prepare.php 요청 바디:
 * { "product_code": "TICKET_1" | "TICKET_5" | "TICKET_10" }
 *
 * user_id는 담지 않는다. RetrofitClient의 AuthInterceptor가
 * JWT 토큰을 자동으로 Authorization 헤더에 첨부하므로,
 * 서버가 토큰에서 사용자를 식별한다.
 */
public class PreparePaymentRequest {

    private String product_code;

    public PreparePaymentRequest(String productCode) {
        this.product_code = productCode;
    }

    public String getProduct_code() {
        return product_code;
    }

    public void setProduct_code(String productCode) {
        this.product_code = productCode;
    }
}