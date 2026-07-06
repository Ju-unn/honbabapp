package com.example.honbabapp.api.data.request.meeting;

/**
 * 모임 수정 요청 모델
 *
 * POST /api/meeting/update.php
 *
 * user_id는 서버가 JWT 토큰에서 추출하므로 요청 바디에 포함하지 않는다.
 */
public class UpdateMeetingRequest {
    private int meeting_id;
    private String meeting_title;
    private String meeting_description;
    private String meeting_category;
    private String meeting_date;
    private int meeting_max_participants;
    private String meeting_price_range;
    private String meeting_image;
    private LocationRequest location;   // ← 중첩 객체

    // 중첩 클래스
    public static class LocationRequest {
        private String place_name;
        private String address;
        private String road_address;
        private String sido;
        private String sigungu;
        private String dong;
        private String map_x;
        private String map_y;
        private String category;
        private String phone_number;

        public LocationRequest(String place_name, String address, String road_address,
                               String sido, String sigungu, String dong,
                               String map_x, String map_y, String category, String phone_number) {
            this.place_name   = place_name;
            this.address      = address;
            this.road_address = road_address;
            this.sido         = sido;
            this.sigungu      = sigungu;
            this.dong         = dong;
            this.map_x        = map_x;
            this.map_y        = map_y;
            this.category     = category;
            this.phone_number = phone_number;
        }
    }

    public UpdateMeetingRequest(int meeting_id, String meeting_title,
                                String meeting_description, String meeting_category,
                                String meeting_date, int meeting_max_participants,
                                String meeting_price_range, String meeting_image,
                                LocationRequest location) {
        this.meeting_id              = meeting_id;
        this.meeting_title           = meeting_title;
        this.meeting_description     = meeting_description;
        this.meeting_category        = meeting_category;
        this.meeting_date            = meeting_date;
        this.meeting_max_participants = meeting_max_participants;
        this.meeting_price_range     = meeting_price_range;
        this.meeting_image           = meeting_image;
        this.location                = location;
    }
}