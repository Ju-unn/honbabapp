package com.example.honbabapp.api.data.request.meeting;

public class DeleteMeetingRequest {
    private int meeting_id;
    private int host_id;

    public DeleteMeetingRequest(int meetingId, int hostId) {
        this.meeting_id = meetingId;
        this.host_id = hostId;
    }

    public int getMeeting_id() { return meeting_id; }
    public int getHost_id() { return host_id; }
}