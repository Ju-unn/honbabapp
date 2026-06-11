package com.example.honbabapp.api.data.request.meeting;

/**
 * QR 스캔 요청 바디 DTO
 * POST /api/meeting/qr_scan.php
 *
 * 요청 예시:
 * {
 *   "code_token": "1d749f4a...(64자)",
 *   "user_id": 3,
 *   "lat": 37.4979,
 *   "lng": 127.0276
 * }
 */
public class QrScanRequest {

    private String code_token;
    private int user_id;
    private double lat;   // 스캔 기기의 현재 위도 (FusedLocationProviderClient 에서 획득)
    private double lng;   // 스캔 기기의 현재 경도

    public QrScanRequest(String code_token, int user_id, double lat, double lng) {
        this.code_token = code_token;
        this.user_id = user_id;
        this.lat = lat;
        this.lng = lng;
    }

    public String getCode_token() {
        return code_token;
    }

    public void setCode_token(String code_token) {
        this.code_token = code_token;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
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