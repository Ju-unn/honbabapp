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
import com.example.honbabapp.databinding.RvHotMeetingBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * HomeFragment - 인기 모임 목록 어댑터
 * is_hot=true인 모임만 표시
 */
public class HomeHotMeetingListAdapter extends RecyclerView.Adapter<HomeHotMeetingListAdapter.ViewHolder>{

    private static final String TAG = "HomeHotMeetingAdapter";
    private List<Meeting> hotMeetingList = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        RvHotMeetingBinding binding = RvHotMeetingBinding.inflate(layoutInflater, parent, false);
        ViewHolder viewHolder = new ViewHolder(binding);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Meeting meeting = hotMeetingList.get(position);
        holder.bind(meeting);

    }

    @Override
    public int getItemCount() {
        return hotMeetingList.size();
    }

    public void updateMeetings(List<Meeting> meetings) {
        if (meetings == null) {
            Log.w(TAG, "updateMeetings: meetings is null");
            return;
        }

        this.hotMeetingList.clear();
        this.hotMeetingList.addAll(meetings);
        notifyDataSetChanged();

        Log.i(TAG, "인기 모임 업데이트 완료: " + meetings.size() + "개");
    }


    public class ViewHolder extends RecyclerView.ViewHolder{
        RvHotMeetingBinding binding;
        public ViewHolder(RvHotMeetingBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Meeting 데이터를 UI에 바인딩
         */
        public void bind(Meeting meeting) {
            // 클릭 이벤트 추가
            binding.getRoot().setOnClickListener(v -> {

                Intent intent = new Intent(v.getContext(), MeetingDetailActivity.class);
                intent.putExtra("meeting_id", meeting.getMeeting_id());
                v.getContext().startActivity(intent);
            });

            // 1. 모임 제목
            binding.tvMeetingTitle.setText(meeting.getMeeting_title());

            // 2. 위치 정보 (첫 번째 줄)
            if (meeting.getLocation() != null) {
                String locationText = formatLocation(meeting);
                binding.tvMeetingLocation.setText(locationText);
            } else {
                binding.tvMeetingLocation.setText("위치 정보 없음");
            }

            // 3. 모임 시간 (두 번째 줄)
            if (meeting.getMeeting_date() != null) {
                String timeText = formatTime(meeting.getMeeting_date());
                binding.tvMeetingTime.setText(timeText);
            }

            // 4. 참여 인원
            int current = meeting.getMeeting_current_participants();
            int max = meeting.getMeeting_max_participants();
            String participantsText = current + "/" + max + "명 참여";
            binding.tvParticipants.setText(participantsText);

            // 5. 이미지
            String imageUrl = meeting.getMeeting_image();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_meeting_image)
                        .centerCrop()
                        .into(binding.ivMeetingImage);
            }
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
                // "2025-12-25 19:00:00" 형식
                String[] parts = datetime.split(" ");
                if (parts.length >= 2) {
                    String time = parts[1];  // "19:00:00"
                    String[] timeParts = time.split(":");
                    int hour = Integer.parseInt(timeParts[0]);
                    int minute = Integer.parseInt(timeParts[1]);

                    String ampm = hour >= 12 ? "오후" : "오전";
                    int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);

                    return String.format("%s %d:%02d", ampm, displayHour, minute);
                }
            } catch (Exception e) {
                Log.e("ViewHolder", "시간 파싱 오류: " + e.getMessage());
            }

            return datetime;  // 파싱 실패 시 원본 반환
        }

    }
}
























