package com.example.honbabapp.api.data.request.meeting;

/**
 * 모임 생성 요청 모델
 *
 * POST /api/meeting/create.php
 *
 * {
 *   "user_id": 1,
 *   "meeting_title": "...",
 *   "meeting_description": "...",
 *   "meeting_category": "한식",
 *   "meeting_date": "2025-12-25 19:00:00",
 *   "meeting_max_participants": 4,
 *   "meeting_price_range": "20,000원",
 *   "location": { ... }
 * }
 */
public class MeetingCreateRequest {
    private int user_id;
    private String meeting_title;
    private String meeting_description;
    private String meeting_category;
    private String meeting_date;
    private int meeting_max_participants;
    private String meeting_price_range;
    private String meeting_image;
    private LocationRequest location;

    // 내부 클래스: location 객체
    public static class LocationRequest {
        public String place_name;
        public String address;
        public String road_address;
        public String sido;
        public String sigungu;
        public String dong;
        public String map_x;
        public String map_y;
        public String category;
        public String phone_number;

        public LocationRequest(String place_name, String address, String road_address,
                               String sigungu, String dong, String map_x, String map_y,
                               String category, String phone_number) {
            this.place_name = place_name;
            this.address = address;
            this.road_address = road_address;
            this.sido = null;  // 임시
            this.sigungu = sigungu;
            this.dong = dong;
            this.map_x = map_x;
            this.map_y = map_y;
            this.category = category;
            this.phone_number = phone_number;
        }
    }

    // ──────────────────────────────────────────────
    // Constructor
    // meeting_image 없이 생성 시 null로 초기화됨
    // ──────────────────────────────────────────────
    public MeetingCreateRequest(int user_id, String meeting_title, String meeting_description,
                                String meeting_category, String meeting_date,
                                int meeting_max_participants, String meeting_price_range,
                                LocationRequest location) {
        this.user_id = user_id;
        this.meeting_title = meeting_title;
        this.meeting_description = meeting_description;
        this.meeting_category = meeting_category;
        this.meeting_date = meeting_date;
        this.meeting_max_participants = meeting_max_participants;
        this.meeting_price_range = meeting_price_range;
        this.meeting_image = null;  // 이미지 없이 생성 시 기본값
        this.location = location;
    }

    // ──────────────────────────────────────────────
    // Constructor (meeting_image 포함)
    // 가게 선택 → 이미지 자동 업로드 완료 후 이 생성자 사용
    // ──────────────────────────────────────────────
    public MeetingCreateRequest(int user_id, String meeting_title, String meeting_description,
                                String meeting_category, String meeting_date,
                                int meeting_max_participants, String meeting_price_range,
                                String meeting_image, LocationRequest location) {
        this.user_id = user_id;
        this.meeting_title = meeting_title;
        this.meeting_description = meeting_description;
        this.meeting_category = meeting_category;
        this.meeting_date = meeting_date;
        this.meeting_max_participants = meeting_max_participants;
        this.meeting_price_range = meeting_price_range;
        this.meeting_image = meeting_image;
        this.location = location;
    }

    // Getters and Setters
    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public String getMeeting_title() {
        return meeting_title;
    }

    public void setMeeting_title(String meeting_title) {
        this.meeting_title = meeting_title;
    }

    public String getMeeting_description() {
        return meeting_description;
    }

    public void setMeeting_description(String meeting_description) {
        this.meeting_description = meeting_description;
    }

    public String getMeeting_category() {
        return meeting_category;
    }

    public void setMeeting_category(String meeting_category) {
        this.meeting_category = meeting_category;
    }

    public String getMeeting_date() {
        return meeting_date;
    }

    public void setMeeting_date(String meeting_date) {
        this.meeting_date = meeting_date;
    }

    public int getMeeting_max_participants() {
        return meeting_max_participants;
    }

    public void setMeeting_max_participants(int meeting_max_participants) {
        this.meeting_max_participants = meeting_max_participants;
    }

    public String getMeeting_price_range() {
        return meeting_price_range;
    }

    public void setMeeting_price_range(String meeting_price_range) {
        this.meeting_price_range = meeting_price_range;
    }
    public String getMeeting_image() { return meeting_image; }
    public void setMeeting_image(String meeting_image) { this.meeting_image = meeting_image; }

    public LocationRequest getLocation() {
        return location;
    }

    public void setLocation(LocationRequest location) {
        this.location = location;
    }
}