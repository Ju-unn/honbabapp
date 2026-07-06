package com.example.honbabapp.api.data.request.meeting;

/**
 * 모임 삭제 요청 모델
 *
 * POST /api/meeting/delete.php
 *
 * host_id는 서버가 JWT 토큰에서 추출하므로 요청 바디에 포함하지 않는다.
 */
public class DeleteMeetingRequest {
    private int meeting_id;

    public DeleteMeetingRequest(int meetingId) {
        this.meeting_id = meetingId;
    }

    public int getMeeting_id() { return meeting_id; }
}