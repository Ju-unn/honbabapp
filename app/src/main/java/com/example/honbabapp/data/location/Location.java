package com.example.honbabapp.data.location;

public class Location {
    /// ============ 네이버 api 연동시
    public String location_store_naver_place_id; /// 네이버 플레이스 id
    public String location_store_name; /// 가게이름
    public String location_store_address; /// 가게 주소
    public String location_store_road_address; /// 가게 도로명 주소
    public Double location_store_rating; /// 가게 평점
    public Double location_store_latitude; /// 가게 위도 (지도 표시용);
    public Double location_store_longitide; /// 가게 경도 (지도 표시용);

    public Location (
            String location_store_naver_place_id ,
            String location_store_name ,
            String location_store_address ,
            String location_store_road_address ,
            Double location_store_rating ,
            Double location_store_latitude ,
            Double location_store_longitide) {
        this.location_store_naver_place_id = location_store_naver_place_id;
        this.location_store_name = location_store_name;
        this.location_store_address = location_store_address;
        this.location_store_road_address = location_store_road_address;
        this.location_store_rating = location_store_rating;
        this.location_store_latitude = location_store_latitude;
        this.location_store_longitide = location_store_longitide;
    }
}
