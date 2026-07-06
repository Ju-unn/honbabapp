package com.example.honbabapp.api.data.request.user;

/**
 * 회원 탈퇴 요청 모델
 *
 * POST /api/user/delete_account.php
 *
 * user_id는 서버가 JWT 토큰에서 추출하므로 요청 바디에 포함하지 않는다.
 * 요청 바디가 비어있지만, Retrofit @Body에는 빈 객체라도 있어야 하므로
 * 마커(marker) 클래스로 유지한다.
 */
public class DeleteAccountRequest {
    public DeleteAccountRequest() {
    }
}