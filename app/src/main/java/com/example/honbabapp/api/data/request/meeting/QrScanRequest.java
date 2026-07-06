package com.example.honbabapp.api.data.request.meeting;

/**
 * QR 스캔 요청 바디 DTO
 * POST /api/qr/qr_scan.php
 *
 * user_id는 서버가 JWT 토큰에서 추출하므로 요청 바디에 포함하지 않는다.
 */
public class QrScanRequest {

    private String code_token;
    private double lat;   // 스캔 기기의 현재 위도 (FusedLocationProviderClient 에서 획득)
    private double lng;   // 스캔 기기의 현재 경도

    public QrScanRequest(String code_token, double lat, double lng) {
        this.code_token = code_token;
        this.lat = lat;
        this.lng = lng;
    }

    public String getCode_token() {
        return code_token;
    }

    public void setCode_token(String code_token) {
        this.code_token = code_token;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }
}