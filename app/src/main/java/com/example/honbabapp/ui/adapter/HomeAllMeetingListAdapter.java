package com.example.honbabapp.ui.adapter;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.activity.meeting.MeetingDetailActivity;
import com.example.honbabapp.api.data.meeting.Meeting;
import com.example.honbabapp.databinding.RvMapMeetingCardBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * HomeFragment - 전체 모임 목록 어댑터
 * 실제 API 데이터를 RecyclerView에 표시
 */
public class HomeAllMeetingListAdapter extends RecyclerView.Adapter<HomeAllMeetingListAdapter.ViewHolder> {

    private static final String TAG = "HomeAllMeetingAdapter";
    private List<Meeting> meetingList = new ArrayList<>();


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RvMapMeetingCardBinding binding = RvMapMeetingCardBinding.inflate(
                LayoutInflater.from(parent.getContext()) , parent , false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(meetingList.get(position));
    }

    @Override
    public int getItemCount() {
        return meetingList.size();
    }

    /**
     * 데이터 업데이트 메서드
     * Fragment에서 API 응답 받으면 호출
     */

    public void updateMeetings(List<Meeting> meetings) {
        if (meetings == null) {
            Log.w(TAG, "updateMeetings: meetings is null");
            return;
        }
        Log.i(TAG, "=== updateMeetings 호출됨 ===");
        Log.i(TAG, "받은 모임 개수: " + meetings.size());

        this.meetingList.clear();
        this.meetingList.addAll(meetings);
        Log.i(TAG, "Adapter에 저장된 모임 개수: " + this.meetingList.size());
        notifyDataSetChanged();
        Log.i(TAG, "모임 목록 업데이트 완료: " + meetings.size() + "개");
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        RvMapMeetingCardBinding binding;
        public ViewHolder(RvMapMeetingCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Meeting 데이터를 UI에 바인딩
         */
        public void bind(Meeting meeting) {
            
            // 클릭 이벤트
            binding.getRoot().setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext() , MeetingDetailActivity.class);
                intent.putExtra("meeting_id" , meeting.getMeeting_id());
                v.getContext().startActivity(intent);
            });

            // 1. 모임 제목
            binding.tvMeetingTitle.setText(meeting.getMeeting_title());

            // 2. 위치 정보
            if (meeting.getLocation() != null) {
                String locationText = formatLocation(meeting);
                binding.tvMeetingLocation.setText(locationText);
            } else {
                binding.tvMeetingLocation.setText("위치 정보 없음");
            }

            // 3. 모임 시간
            if (meeting.getMeeting_date() != null) {
                String timeText = formatTime(meeting.getMeeting_date());
                binding.tvMeetingTime.setText(timeText);
            }

            // 4. 모임 설명
            binding.tvMeetingDescription.setText(meeting.getMeeting_description());

            // 5. 참여 인원 정보
            int current = meeting.getMeeting_current_participants();
            int max = meeting.getMeeting_max_participants();
            int needed = max - current;

            if (needed > 0) {
                binding.tvParticipantsNeeded.setText("+" + needed + "명 더 필요");
            } else {
                binding.tvParticipantsNeeded.setText("모집 완료");
            }

            // 6. 게시 시간 (TODO: 나중에 실제 계산 로직 추가)

            // 7. 이미지
            Glide.with(itemView.getContext())
                    .load(meeting.getMeeting_image())
                    .placeholder(R.drawable.placeholder_meeting_image)
                    .error(R.drawable.placeholder_meeting_image)
                    .centerCrop()
                    .into(binding.ivMeetingImage);
        }

        /**
         * 위치 정보 포맷팅
         * "관악구 신림동" 형식으로 표시
         */
        private String formatLocation(Meeting meeting) {
            String sigungu = meeting.getLocation().getSigungu();
            String dong = meeting.getLocation().getDong();

            if (dong != null && !dong.isEmpty()) {
                return sigungu + " " + dong;
            }
            return sigungu;
        }

        /**
         * 시간 정보 포맷팅
         * "2025-12-25 19:00:00" → "오후 7:00"
         */
        private String formatTime(String datetime) {
            try {
                // 2025-12-25 19:00:00 형식 응답
                String[] parts = datetime.split(" ");
                if (parts.length >= 2) {
                    String time = parts[1]; // 19:00:00
                    String[] timeParts = time.split(":");
                    int hour = Integer.parseInt(timeParts[0]);
                    int minute = Integer.parseInt(timeParts[1]);

                    String ampm = hour >= 12 ? "오후" : "오전";
                    int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);

                    return String.format("%s %d:%02d" , ampm , displayHour , minute);
                }
            } catch (Exception e) {
                Log.e("ViewHolder", "시간 파싱 오류: " + e.getMessage());
            }

            return datetime;
        }
    }
}
