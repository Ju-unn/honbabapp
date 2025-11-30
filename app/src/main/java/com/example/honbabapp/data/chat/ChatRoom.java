package com.example.honbabapp.data.chat;

import com.example.honbabapp.data.user.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatRoom {
    /// ChatRoom : 1대1채팅 , 그룹 채팅방 관리
    public int chatRoom_room_id; /// 채팅방 고유 번호
    public int chatRoom_owner_id; /// 방 생성자 아이디
    public Integer chatRoom_meeting_id; /// 모임 채팅방인 경우 모임 고유번호 (1:1은 null);
    public String chatRoom_room_type; /// 채팅방 타입 (1:1 , group)
    public String chatRoom_room_name; /// 채팅방 이름 (그룹 : title  , 1:1 : user_nick_nmae);
    public String chatRoom_last_message; /// 마지막 메세지 내용 (미리보기용)
    public Date chatRoom_last_message_time; /// 마지막 메세지 시간 (채팅방 정렬)
    public Date chatRoom_created_at; /// 채팅방 생성일시
    public int chatRoom_unread_count; /// 안 읽은 메시지 수
    public ArrayList<User> chatRoom_participants; /// 참여자 리스트 (user 객체)


    public ChatRoom (int chatRoom_room_id , int chatRoom_owner_id , Integer chatRoom_meeting_id , String chatRoom_room_type , String chatRoom_room_name
    , String chatRoom_last_message , Date chatRoom_last_message_time , Date chatRoom_created_at , int chatRoom_unread_count , ArrayList<User> chatRoom_participants) {
        this.chatRoom_room_id = chatRoom_room_id;
        this.chatRoom_owner_id = chatRoom_owner_id;
        this.chatRoom_meeting_id = chatRoom_meeting_id;
        this.chatRoom_room_type = chatRoom_room_type;
        this.chatRoom_room_name = chatRoom_room_name;
        this.chatRoom_last_message = chatRoom_last_message;
        this.chatRoom_last_message_time = chatRoom_last_message_time;
        this.chatRoom_created_at = chatRoom_created_at;
        this.chatRoom_unread_count = chatRoom_unread_count;
        this.chatRoom_participants = chatRoom_participants;
    }
}