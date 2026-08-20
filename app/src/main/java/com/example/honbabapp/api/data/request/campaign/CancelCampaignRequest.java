package com.example.honbabapp.api.data.request.campaign;

/**
 * 체험단 참가자 자기 취소 요청 모델
 *
 * POST /api/campaign/campaign_cancel.php
 * { "meeting_id": 1 }
 *
 * user_id는 서버가 JWT 토큰에서 추출하므로 요청 바디에 포함하지 않는다.
 */
public class CancelCampaignRequest {

    private int meeting_id;

    public CancelCampaignRequest() {}

    public CancelCampaignRequest(int meeting_id) {
        this.meeting_id = meeting_id;
    }

    public int getMeeting_id() { return meeting_id; }
    public void setMeeting_id(int meeting_id) { this.meeting_id = meeting_id; }
}
