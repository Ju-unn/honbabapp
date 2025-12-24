package com.example.honbabapp.ui.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.honbabapp.api.data.meeting.Meeting;
import com.example.honbabapp.data.MockData;
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
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

        /// allMeetingList
        RvMapMeetingCardBinding binding = RvMapMeetingCardBinding.inflate(layoutInflater,parent,false);
        ViewHolder viewHolder = new ViewHolder(binding);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Meeting meeting = meetingList.get(position);
        holder.bind(meeting);
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

        this.meetingList.clear();
        this.meetingList.addAll(meetings);
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

            // 7. 이미지 (TODO: Glide로 로딩)
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






























