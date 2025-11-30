package com.example.honbabapp.data.meeting;
import com.example.honbabapp.data.location.Location;
import com.example.honbabapp.data.user.User;

import java.util.ArrayList;
import java.util.Date;

public class Meeting {
    /// Meeting : 모임생성 , 조회 , 참여관리

    public int meeting_id; /// 모임 고유번호
    public int meeting_host_id; /// 모임 주최자 id
    public String meeting_title; /// 모임 제목
    public String meeting_description; /// 모임 설명 (호스트 메시지)
    public String meeting_category; /// 모임 음식 카테고리(한식 , 중식 , 일식 등)
    public Date meeting_date; ///모임 만나는 날짜 및 시간
    public int meeting_max_participants; /// 최대 참여 인원
    public int meeting_current_participants; /// 현재 인원
    public ArrayList<User> meeting_user_list; /// 미팅에 담을 유저 리스트
    public String meeting_price_range; /// 모임 예상 가격
    public String meeting_image_url; /// 모임 대표 이미지 url
    public String meeting_status; /// 모임 상태 (모집중 , 모집종료 , 모집완료 , 취소(취소는 없어도 괜찮을거같은.. 바로 삭제))
    public int meeting_view_count; /// 모임 조회수 (인기도 측정 및 관심사)
    public boolean meeting_is_hot; /// 인기 모임 여부(홈 hot에 배치 여부 , 조회수 몇 이상이면 등록하게)
    public Date meeting_created_at; /// 모임 생성일시
    public Location meeting_location;


    public Meeting(int meeting_id,String meeting_title , ArrayList<User> meeting_user_list , int meeting_current_participants ,
                   String meeting_description , String meeting_category ,Date meeting_date, int meeting_max_participants ,
                   String meeting_price_range ,String meeting_status , int meeting_view_count , boolean meeting_is_hot , Date meeting_created_at, Location meeting_location) {

        this.meeting_id = meeting_id;
        this.meeting_title = meeting_title;
        this.meeting_user_list = meeting_user_list;
        this.meeting_description = meeting_description;
        this.meeting_current_participants = meeting_current_participants;
        this.meeting_category = meeting_category;
        this.meeting_date = meeting_date;
        this.meeting_max_participants = meeting_max_participants;
        this.meeting_price_range = meeting_price_range;
        this.meeting_status = meeting_status;
        this.meeting_view_count = meeting_view_count;
        this.meeting_is_hot = meeting_is_hot;
        this.meeting_created_at = meeting_created_at;
        this.meeting_location = meeting_location;
    }
}
