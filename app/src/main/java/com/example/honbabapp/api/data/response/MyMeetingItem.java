package com.example.honbabapp.api.data.response;

public class MyMeetingItem {
    private int meeting_id;
    private String meeting_title;
    private String meeting_date;
    private String meeting_category;
    private String meeting_image;      // S3 URL or null
    private String meeting_status;
    private String user_role;          // "host" or "participant"
    private int meeting_current_participants;
    private int meeting_max_participants;
    private boolean is_past;           // true면 과거 모임

    // Getter들
    public int getMeeting_id() { return meeting_id; }
    public String getMeeting_title() { return meeting_title; }
    public String getMeeting_date() { return meeting_date; }
    public String getMeeting_category() { return meeting_category; }
    public String getMeeting_image() { return meeting_image; }
    public String getMeeting_status() { return meeting_status; }
    public String getUser_role() { return user_role; }
    public int getMeeting_current_participants() { return meeting_current_participants; }
    public int getMeeting_max_participants() { return meeting_max_participants; }
    public boolean isIs_past() { return is_past; }
}
