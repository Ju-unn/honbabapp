package com.example.honbabapp.api.data.meeting;

import com.example.honbabapp.api.data.Location;

import java.util.List;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 모임 정보 모델
 * <p>
 * meeting_type : "일반" | "체험단"
 * campaign_* 필드는 meeting_type="체험단"일 때만 값이 있고,
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

    // ─── 체험단 전용 필드 ───────────────────────────────────────
    private String meeting_type;           // "일반" | "체험단"
    private String campaign_channels;      // "블로그,인스타그램"
    private String campaign_provided;      // "2인 세트 무료 제공"
    private String campaign_requirements;  // "팔로워 1,000명 이상" (nullable)
    private String campaign_deadline;      // "2025-08-31 00:00:00" (nullable)

    // ─── 연관 객체 ─────────────────────────────────────────────
    private Location location;
    private MeetingHost host;
    private List<MeetingParticipant> participants;

    // ─── 기존 Getter / Setter ──────────────────────────────────

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

    public String getMeeting_image() {
        return meeting_image;
    }

    public void setMeeting_image(String meeting_image) {
        this.meeting_image = meeting_image;
    }

    public String getMy_participation_status() {
        return my_participation_status;
    }

    public void setMy_participation_status(String my_participation_status) {
        this.my_participation_status = my_participation_status;
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

    // ─── 체험단 전용 Getter / Setter ────────────────────────────

    public String getMeeting_type() {
        return meeting_type;
    }

    public void setMeeting_type(String meeting_type) {
        this.meeting_type = meeting_type;
    }

    public String getCampaign_channels() {
        return campaign_channels;
    }

    public void setCampaign_channels(String campaign_channels) {
        this.campaign_channels = campaign_channels;
    }

    public String getCampaign_provided() {
        return campaign_provided;
    }

    public void setCampaign_provided(String campaign_provided) {
        this.campaign_provided = campaign_provided;
    }

    public String getCampaign_requirements() {
        return campaign_requirements;
    }

    public void setCampaign_requirements(String campaign_requirements) {
        this.campaign_requirements = campaign_requirements;
    }

    public String getCampaign_deadline() {
        return campaign_deadline;
    }

    public void setCampaign_deadline(String campaign_deadline) {
        this.campaign_deadline = campaign_deadline;
    }

    // ─── 유틸 ──────────────────────────────────────────────────

    public boolean isExpired() {
        if (meeting_date == null) return false;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
            Date date = sdf.parse(meeting_date);
            return date != null && date.before(new Date());
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * 체험단 여부 편의 메서드
     */
    public boolean isCampaign() {
        return "체험단".equals(meeting_type);
    }
}