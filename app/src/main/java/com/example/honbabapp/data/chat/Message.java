package com.example.honbabapp.data.chat;

import java.util.Date;

public class Message {
    /// Message : 채팅 메세지 저장
    public int message_id; /// 메시지 고유 번호
    public int message_room_id; /// ChatRoom 고유 번호
    public Integer message_sender_id; /// 보낸 사람
    public String message_type; /// 메세지 타입 (text , image , system)
    public String message_content; /// 메시지 내용
    public String message_image_url; /// 이미지 url
    public boolean message_is_read; /// 읽음 여부 (읽음 표시용)
    public int message_unread_count;  /// 읽음 카운터
    public Date message_created_at; /// 메세지 전송 시간

    public Message (int message_id ,int message_room_id , Integer message_sender_id , String message_type
    , String message_content , boolean message_is_read , int message_unread_count , Date message_created_at) {
        this.message_id = message_id;
        this.message_room_id = message_room_id;
        this.message_sender_id = message_sender_id;
        this.message_type = message_type;
        this.message_content = message_content;
        this.message_is_read = message_is_read;
        this.message_unread_count = message_unread_count;
        this.message_created_at = message_created_at;
    }

}
