package com.example.honbabapp.ui.adapter;

import com.example.honbabapp.R;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.honbabapp.activity.meeting.MeetingDetailActivity;
import com.example.honbabapp.api.data.meeting.Meeting;
import com.example.honbabapp.databinding.RvMeetingCardBinding;

import java.util.ArrayList;
import java.util.List;

public class MapAllMeetingListAdapter extends RecyclerView.Adapter<MapAllMeetingListAdapter.ViewHolder> {

    private static final String TAG = "MapAllMeetingAdapter";
    private List<Meeting> meetings = new ArrayList<>();

    public void updateMeetings(List<Meeting> newMeetings) {
        this.meetings = newMeetings;
        notifyDataSetChanged();
    }


    @NonNull
    @Override
    public MapAllMeetingListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RvMeetingCardBinding binding = RvMeetingCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MapAllMeetingListAdapter.ViewHolder holder, int position) {
        holder.bind(meetings.get(position));
    }

    @Override
    public int getItemCount() {
        return meetings.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final RvMeetingCardBinding binding;

        public ViewHolder(RvMeetingCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Meeting meeting) {

            // location null 체크 — DB에 location이 없는 모임 방어
            if (meeting.getLocation() != null) {
                binding.tvMeetingLocation.setText(meeting.getLocation().getPlace_name() + "" + meeting.getLocation().getRoad_address());
            } else {
                binding.tvMeetingLocation.setText("");
            }

            // 클릭 이벤트
            binding.getRoot().setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext() , MeetingDetailActivity.class);
                intent.putExtra("meeting_id", meeting.getMeeting_id());
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

            // 3. 모임시간
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

        private String formatLocation(Meeting meeting) {
            String sigungu = meeting.getLocation().getSigungu();
            String dong = meeting.getLocation().getDong();

            if (dong != null && !dong.isEmpty()) {
                return sigungu + " " + dong;
            }
            return sigungu;
        }

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



























