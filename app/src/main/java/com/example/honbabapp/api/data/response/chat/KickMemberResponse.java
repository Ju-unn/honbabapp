package com.example.honbabapp.api.data.response.chat;

/**
 * 강퇴 응답 모델
 *
 * POST /api/chat/kick.php 응답의 data 부분:
 * {
 *   "room_id": 1,
 *   "target_user_id": 7,
 *   "remaining": 3,
 *   "meeting_current_participants": 3
 * }
 *
 * 필드 해석:
 * - room_id                      : 강퇴가 일어난 방
 * - target_user_id               : 강퇴된 user_id
 * - remaining                    : 방에 남은 멤버 수 (chat_room_members COUNT)
 * - meeting_current_participants : 모임 현재 인원 (강퇴 후 -1 된 값)
 *
 * 클라이언트 사용처:
 * - ChatRoomDetailActivity : 강퇴 성공 후 loadMeetingDetail() 재호출로 화면 갱신
 *   (이 응답값 직접 안 써도 되긴 하지만 추후 옵티미스틱 업데이트 등에 활용 가능)
 */
public class KickMemberResponse {

    private int room_id;
    private int target_user_id;
    private int remaining;
    private int meeting_current_participants;

    public KickMemberResponse() {}

    // ── Getter / Setter ──

    public int getRoom_id() { return room_id; }
    public void setRoom_id(int room_id) { this.room_id = room_id; }

    public int getTarget_user_id() { return target_user_id; }
    public void setTarget_user_id(int target_user_id) { this.target_user_id = target_user_id; }

    public int getRemaining() { return remaining; }
    public void setRemaining(int remaining) { this.remaining = remaining; }

    public int getMeeting_current_participants() { return meeting_current_participants; }
    public void setMeeting_current_participants(int meeting_current_participants) {
        this.meeting_current_participants = meeting_current_participants;
    }
}