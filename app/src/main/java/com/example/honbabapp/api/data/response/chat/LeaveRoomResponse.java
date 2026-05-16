package com.example.honbabapp.api.data.response.chat;

/**
 * 채팅방 나가기 응답 모델 (V2)
 *
 * POST /api/chat/leave.php 응답의 data 부분:
 * {
 *   "room_id": 5,
 *   "remaining": 3,
 *   "is_host": true,
 *   "is_host_transferred": true,
 *   "new_host_id": 18,           ← null 가능 (위임 안 된 경우)
 *   "room_deleted": false,
 *   "meeting_deleted": false,
 *   "penalty_applied": 0.3,
 *   "new_manner_score": 36.2
 * }
 *
 * 필드 해석:
 * - is_host                : 나간 사람이 호스트였나
 * - is_host_transferred    : 호스트면서 위임이 발생했나 (다음 방장 지정됨)
 * - new_host_id            : 위임받은 새 방장 user_id (없으면 null)
 * - room_deleted           : 채팅방 통째 삭제됐나 (호스트 혼자였을 때)
 * - meeting_deleted        : 모임이 '취소' 상태로 됐나
 * - penalty_applied        : 적용된 패널티 (0.0 / 0.3 / 1.0)
 * - new_manner_score       : 차감 후 매너점수 (0 미만 안 됨)
 */
public class LeaveRoomResponse {

    private int room_id;
    private int remaining;
    private boolean is_host;
    private boolean is_host_transferred;
    private Integer new_host_id;       // null 가능 — Integer 사용
    private boolean room_deleted;
    private boolean meeting_deleted;
    private double penalty_applied;
    private double new_manner_score;

    public LeaveRoomResponse() {}


    // ── Getter / Setter ──────────────────────────

    public int getRoom_id() { return room_id; }
    public void setRoom_id(int room_id) { this.room_id = room_id; }

    public int getRemaining() { return remaining; }
    public void setRemaining(int remaining) { this.remaining = remaining; }

    public boolean isIs_host() { return is_host; }
    public void setIs_host(boolean is_host) { this.is_host = is_host; }

    public boolean isIs_host_transferred() { return is_host_transferred; }
    public void setIs_host_transferred(boolean v) { this.is_host_transferred = v; }

    public Integer getNew_host_id() { return new_host_id; }
    public void setNew_host_id(Integer new_host_id) { this.new_host_id = new_host_id; }

    public boolean isRoom_deleted() { return room_deleted; }
    public void setRoom_deleted(boolean room_deleted) { this.room_deleted = room_deleted; }

    public boolean isMeeting_deleted() { return meeting_deleted; }
    public void setMeeting_deleted(boolean meeting_deleted) { this.meeting_deleted = meeting_deleted; }

    public double getPenalty_applied() { return penalty_applied; }
    public void setPenalty_applied(double penalty_applied) { this.penalty_applied = penalty_applied; }

    public double getNew_manner_score() { return new_manner_score; }
    public void setNew_manner_score(double new_manner_score) { this.new_manner_score = new_manner_score; }
}