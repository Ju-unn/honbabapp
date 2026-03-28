package com.example.honbabapp.api.data.meeting;

import com.example.honbabapp.api.data.Location;

import java.util.List;

/**
 * 모임 정보 모델
 *
 * API 응답:
 * {
 *   "meeting_id": 1,
 *   "meeting_title": "오늘 저녁 삼겹살",
 *   "meeting_description": "...",
 *   "meeting_category": "한식",
 *   "meeting_date": "2025-12-25 19:00:00",
 *   "meeting_max_participants": 4,
 *   "meeting_current_participants": 2,
 *   "meeting_price_range": "20,000원",
 *   "meeting_status": "모집중",
 *   "meeting_is_hot": true,
 *   "meeting_view_count": 45,
 *   "meeting_created_at": "2025-12-21 10:00:00",
 *   "meeting_image": "https://...",
 *   "location": { ... },
 *   "host": { ... },
 *   "participants": [ ... ]
 * }
 */
public class Meeting {
    private int meeting_id;
    private String meeting_title;
    private String meeting_description;
    private String meeting_category;
    private String meeting_date;
    private int meeting_max_participants;
    private int meeting_current_participants;
    private String meeting_price_range;
    private String meeting_status;
    private boolean meeting_is_hot;
    private int meeting_view_count;
    private String meeting_created_at;
    private String meeting_image;
    private String my_participation_status;

    // 연관 객체
    private Location location;
    private MeetingHost host;
    private List<MeetingParticipant> participants;

    public String getMeeting_image() {
        return meeting_image;
    }
    public void setMeeting_image(String meeting_image) {
        this.meeting_image = meeting_image;
    }
    public String getMy_participation_status() {
        return my_participation_status;
    }

    public int getMeeting_id() {
        return meeting_id;
    }

    public void setMeeting_id(int meeting_id) {
        this.meeting_id = meeting_id;
    }

    public String getMeeting_title() {
        return meeting_title;
    }

    public void setMeeting_title(String meeting_title) {
        this.meeting_title = meeting_title;
    }

    public String getMeeting_description() {
        return meeting_description;
    }

    public void setMeeting_description(String meeting_description) {
        this.meeting_description = meeting_description;
    }

    public String getMeeting_category() {
        return meeting_category;
    }

    public void setMeeting_category(String meeting_category) {
        this.meeting_category = meeting_category;
    }

    public String getMeeting_date() {
        return meeting_date;
    }

    public void setMeeting_date(String meeting_date) {
        this.meeting_date = meeting_date;
    }

    public int getMeeting_max_participants() {
        return meeting_max_participants;
    }

    public void setMeeting_max_participants(int meeting_max_participants) {
        this.meeting_max_participants = meeting_max_participants;
    }

    public int getMeeting_current_participants() {
        return meeting_current_participants;
    }

    public void setMeeting_current_participants(int meeting_current_participants) {
        this.meeting_current_participants = meeting_current_participants;
    }

    public String getMeeting_price_range() {
        return meeting_price_range;
    }

    public void setMeeting_price_range(String meeting_price_range) {
        this.meeting_price_range = meeting_price_range;
    }

    public String getMeeting_status() {
        return meeting_status;
    }

    public void setMeeting_status(String meeting_status) {
        this.meeting_status = meeting_status;
    }

    public boolean isMeeting_is_hot() {
        return meeting_is_hot;
    }

    public void setMeeting_is_hot(boolean meeting_is_hot) {
        this.meeting_is_hot = meeting_is_hot;
    }

    public int getMeeting_view_count() {
        return meeting_view_count;
    }

    public void setMeeting_view_count(int meeting_view_count) {
        this.meeting_view_count = meeting_view_count;
    }

    public String getMeeting_created_at() {
        return meeting_created_at;
    }

    public void setMeeting_created_at(String meeting_created_at) {
        this.meeting_created_at = meeting_created_at;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public MeetingHost getHost() {
        return host;
    }

    public void setHost(MeetingHost host) {
        this.host = host;
    }

    public List<MeetingParticipant> getParticipants() {
        return participants;
    }

    public void setParticipants(List<MeetingParticipant> participants) {
        this.participants = participants;
    }
}
