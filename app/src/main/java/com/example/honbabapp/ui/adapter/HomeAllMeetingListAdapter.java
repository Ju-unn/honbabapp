package com.example.honbabapp.ui.adapter;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.activity.meeting.MeetingDetailActivity;
import com.example.honbabapp.api.data.meeting.Meeting;
import com.example.honbabapp.api.data.meeting.MeetingParticipant;
import com.example.honbabapp.databinding.RvMapMeetingCardBinding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

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
                binding.tvParticipantsNeeded.setText("정원 최대");
            }

            // 6. 게시 시간
            binding.tvPostTime.setText(getPostTimeText(meeting));

            // 7. 모임 이미지
            Glide.with(itemView.getContext())
                    .load(meeting.getMeeting_image())
                    .placeholder(R.drawable.placeholder_meeting_image)
                    .error(R.drawable.placeholder_meeting_image)
                    .centerCrop()
                    .into(binding.ivMeetingImage);

            // 8. 참여자 이미지 최대3개
            ImageView[] avatars = {
                    binding.ivAvatar1,
                    binding.ivAvatar2,
                    binding.ivAvatar3
            };

            List<MeetingParticipant> participants = meeting.getParticipants();
            int count = (participants != null) ? Math.min(participants.size(), 3) : 0;

            for (int i = 0; i < 3; i++) {
                if (i < count) {
                    avatars[i].setVisibility(View.VISIBLE);
                    String imageUrl = participants.get(i).getUser_profile_image();
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(itemView.getContext())
                                .load(imageUrl)
                                .placeholder(R.drawable.ic_profile)
                                .circleCrop()
                                .into(avatars[i]);
                    } else {
                        avatars[i].setImageResource(R.drawable.ic_profile);
                    }
                } else {
                    avatars[i].setVisibility(View.GONE);
                }
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

        /**
         * 게시 시간 포맷팅
         * 만료 미팅 → "모집완료"
         * 아니면 created_at 기준 상대 시간 ("방금", "5분 전", "3시간 전" 등)
         */
        private String getPostTimeText(Meeting meeting) {
            if (meeting.isExpired()) {
                return "모집완료";
            }

            if (meeting.getMeeting_created_at() == null) return "";

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
                // 서버는 KST 벽시계 시간을 내려줌 — UTC 로 파싱하면 상대시간이 9시간 밀림
                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                Date createdAt = sdf.parse(meeting.getMeeting_created_at());
                if (createdAt == null) return "";

                long diffMs   = new Date().getTime() - createdAt.getTime();
                long diffMin  = diffMs / (1000 * 60);
                long diffHour = diffMs / (1000 * 60 * 60);
                long diffDay  = diffMs / (1000 * 60 * 60 * 24);

                if (diffMin < 1)   return "방금";
                if (diffMin < 60)  return diffMin + "분 전";
                if (diffHour < 24) return diffHour + "시간 전";
                if (diffDay == 1)  return "하루 전";
                return diffDay + "일 전";

            } catch (ParseException e) {
                return "";
            }
        }




    }
}
