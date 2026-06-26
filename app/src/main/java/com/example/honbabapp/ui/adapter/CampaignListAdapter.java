package com.example.honbabapp.ui.adapter;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.honbabapp.activity.meeting.MeetingDetailActivity;
import com.example.honbabapp.api.data.meeting.Meeting;
import com.example.honbabapp.databinding.RvCampaignCardBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * 체험단 목록 어댑터
 * <p>
 * 계획서: HomeAllMeetingListAdapter 재사용 또는 경량 변형
 * → campaign 전용 필드(제공내역/홍보채널 뱃지)가 필요하므로 별도 어댑터 생성
 * → 기본 구조와 클릭 이벤트는 HomeAllMeetingListAdapter와 동일
 */
public class CampaignListAdapter extends RecyclerView.Adapter<CampaignListAdapter.ViewHolder> {

    private static final String TAG = "CampaignListAdapter";
    private List<Meeting> campaignMeetingList = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RvCampaignCardBinding binding = RvCampaignCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(campaignMeetingList.get(position));
    }

    @Override
    public int getItemCount() {
        return campaignMeetingList.size();
    }

    /**
     * 체험단 목록 업데이트
     * CampaignFragment에서 API 응답 후 호출
     */
    public void updateCampaignList(List<Meeting> meetings) {
        if (meetings == null) {
            Log.w(TAG, "updateCampaignList: meetings is null");
            return;
        }
        this.campaignMeetingList.clear();
        this.campaignMeetingList.addAll(meetings);
        notifyDataSetChanged();
        Log.i(TAG, "체험단 목록 업데이트 완료: " + meetings.size() + "개");
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final RvCampaignCardBinding binding;

        public ViewHolder(@NonNull RvCampaignCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Meeting 데이터 → UI 바인딩
         * TODO: 로직 단계에서 campaign 전용 필드 바인딩 추가
         */
        public void bind(Meeting meeting) {
            // 클릭 이벤트 - 기존 MeetingDetailActivity 재사용
            binding.getRoot().setOnClickListener(view -> {
                Intent intent = new Intent(view.getContext(), MeetingDetailActivity.class);
                intent.putExtra("meeting_id", meeting.getMeeting_id());
                view.getContext().startActivity(intent);
            });

            // 모임 제목
            binding.tvCampaignTitle.setText(meeting.getMeeting_title());

            // 카테고리
            if (meeting.getMeeting_category() != null) {
                binding.tvCampaignCategory.setText(meeting.getMeeting_category());
            }

            // 위치
            if (meeting.getLocation() != null) {
                binding.tvCampaignLocation.setText(meeting.getLocation().getPlace_name());
            }

            // 날짜 (앞 10자리)
            String meetingDate = meeting.getMeeting_date();
            if (meetingDate != null && meetingDate.length() >= 10) {
                binding.tvCampaignDate.setText(meetingDate.substring(0, 10));
            }

            // 참여 인원
            int currentCount = meeting.getParticipants() != null ? meeting.getParticipants().size() : 0;
            int maxCount = meeting.getMeeting_max_participants();
            binding.tvCampaignParticipantCount.setText(currentCount + "/" + maxCount + "명");

            // TODO: 로직 단계 - campaign_provided, campaign_channels, campaign_deadline 바인딩
        }
    }
}
