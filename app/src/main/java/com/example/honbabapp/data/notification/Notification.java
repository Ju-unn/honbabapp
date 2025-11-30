package com.example.honbabapp.data.notification;

import java.util.Date;

public class Notification {
    /// Notification : 푸시알림 , 앱 내 알림 관리

    public int notification_id; /// 알림 고유 id , 알림삭제등을 위한 고유번호
    public Integer notification_user_id; /// User 참조 (보내는 사람)
    public String notification_type; /// 알림 타입 (meeting , chat , event , system)
    public String notification_title; /// 알림 제목
    public String notification_content; /// 알림 내용
    public Integer notification_relatedId; /// 관련된 모임 / 채팅방 id (클릭 시 해당 화면으로 이동);
    public boolean notification_is_read; /// 읽음 여부
    public Date notification_created_at; /// 알림 발생 시간
}
