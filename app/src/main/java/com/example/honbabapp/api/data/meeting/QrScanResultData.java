package com.example.honbabapp.api.data.meeting;

/**
 * QR 스캔 출석 인증 응답 DTO
 * POST /api/meeting/qr_scan.php 의 data 부분
 *
 * 응답 예시:
 * {
 *   "participation_count": 7
 * }
 */
public class QrScanResultData {

    private int participation_count;  // 인증 후 갱신된 참여 횟수

    public QrScanResultData() {}

    public int getParticipation_count() {
        return participation_count;
    }

    public void setParticipation_count(int participation_count) {
        this.participation_count = participation_count;
    }
}