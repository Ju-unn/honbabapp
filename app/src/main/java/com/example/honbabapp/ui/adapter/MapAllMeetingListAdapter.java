package com.example.honbabapp.ui.adapter;

import com.example.honbabapp.R;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.honbabapp.activity.campaign.CampaignDetailActivity;
import com.example.honbabapp.activity.meeting.MeetingDetailActivity;
import com.example.honbabapp.api.data.meeting.Meeting;
import com.example.honbabapp.api.data.meeting.MeetingParticipant;
import com.example.honbabapp.databinding.RvCampaignCardBinding;
import com.example.honbabapp.databinding.RvMeetingCardBinding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * 지도 하단 목록 어댑터
 *
 */
public class MapAllMeetingListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MapAllMeetingAdapter";

    private static final int VIEW_TYPE_MEETING = 0;   // 일반 모임 → rv_meeting_card.xml
    private static final int VIEW_TYPE_CAMPAIGN = 1;  // 체험단   → rv_campaign_card.xml

    private List<Meeting> meetings = new ArrayList<>();

    public void updateMeetings(List<Meeting> newMeetings) {
        this.meetings = (newMeetings != null) ? newMeetings : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Meeting meeting = meetings.get(position);
        return meeting.isCampaign() ? VIEW_TYPE_CAMPAIGN : VIEW_TYPE_MEETING;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_CAMPAIGN) {
            RvCampaignCardBinding binding = RvCampaignCardBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false
            );
            return new CampaignViewHolder(binding);
        } else {
            RvMeetingCardBinding binding = RvMeetingCardBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false
            );
            return new MeetingViewHolder(binding);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Meeting meeting = meetings.get(position);
        if (holder instanceof CampaignViewHolder) {
            ((CampaignViewHolder) holder).bind(meeting);
        } else if (holder instanceof MeetingViewHolder) {
            ((MeetingViewHolder) holder).bind(meeting);
        }
    }

    @Override
    public int getItemCount() {
        return meetings.size();
    }

    // ─────────────────────────────────────────────────────────────
    // 일반 모임 ViewHolder — rv_meeting_card.xml 
    // ─────────────────────────────────────────────────────────────
    public class MeetingViewHolder extends RecyclerView.ViewHolder {
        private final RvMeetingCardBinding binding;

        public MeetingViewHolder(RvMeetingCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Meeting meeting) {

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
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date createdAt = sdf.parse(meeting.getMeeting_created_at());
                if (createdAt == null) return "";

                long diffMs = new Date().getTime() - createdAt.getTime();
                long diffMin = diffMs / (1000 * 60);
                long diffHour = diffMs / (1000 * 60 * 60);
                long diffDay = diffMs / (1000 * 60 * 60 * 24);

                if (diffMin < 1) return "방금";
                if (diffMin < 60) return diffMin + "분 전";
                if (diffHour < 24) return diffHour + "시간 전";
                if (diffDay == 1) return "하루 전";
                return diffDay + "일 전";

            } catch (ParseException e) {
                return "";
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 체험단 ViewHolder — rv_campaign_card.xml 
    // ─────────────────────────────────────────────────────────────
    public class CampaignViewHolder extends RecyclerView.ViewHolder {
        private final RvCampaignCardBinding binding;

        public CampaignViewHolder(RvCampaignCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Meeting meeting) {
            bindClickEvent(meeting);
            bindImage(meeting);
            bindTitle(meeting);
            bindCampaignProvided(meeting);
            bindCampaignChannels(meeting);
            bindLocation(meeting);
            bindParticipantsRow(meeting);
            bindDeadlineText(meeting);
        }

        // ── 클릭 → CampaignDetailActivity ────────────────────────
        private void bindClickEvent(Meeting meeting) {
            binding.getRoot().setOnClickListener(view -> {
                Intent intent = new Intent(view.getContext(), CampaignDetailActivity.class);
                intent.putExtra("meeting_id", meeting.getMeeting_id());
                view.getContext().startActivity(intent);
            });
        }

        // ── 상단 이미지 ───────────────────────────────────────────
        private void bindImage(Meeting meeting) {
            Glide.with(binding.getRoot().getContext())
                    .load(meeting.getMeeting_image())
                    .placeholder(R.drawable.placeholder_meeting_image)
                    .error(R.drawable.placeholder_meeting_image)
                    .centerCrop()
                    .into(binding.ivCampaignImage);
        }

        // ── 제목 ──────────────────────────────────────────────────
        private void bindTitle(Meeting meeting) {
            binding.tvCampaignTitle.setText(meeting.getMeeting_title());
        }

        // ── 제공 내역 (주황 굵게) ─────────────────────────────────
        private void bindCampaignProvided(Meeting meeting) {
            String provided = meeting.getCampaign_provided();
            if (provided != null && !provided.isEmpty()) {
                binding.tvCampaignProvided.setText(provided);
                binding.tvCampaignProvided.setVisibility(View.VISIBLE);
            } else {
                binding.tvCampaignProvided.setVisibility(View.GONE);
            }
        }

        // ── 홍보 플랫폼 ─────────────────────────────────────────────
        private void bindCampaignChannels(Meeting meeting) {
            String channels = meeting.getCampaign_channels();
            if (channels != null && !channels.isEmpty()) {
                binding.tvCampaignChannels.setText(channels.replace(",", " · "));
                binding.tvCampaignChannels.setVisibility(View.VISIBLE);
            } else {
                binding.tvCampaignChannels.setVisibility(View.GONE);
            }
        }

        // ── 위치 표시 ─────────────────────────────────────────────
        private void bindLocation(Meeting meeting) {
            if (meeting.getLocation() != null) {
                String locationText = meeting.getLocation().getPlace_name();
                if (locationText == null || locationText.isEmpty()) {
                    locationText = meeting.getLocation().getDong();
                }
                binding.tvCampaignLocation.setText(locationText);
            }
        }

        // ── 하단 참여자 아바타 + 남은 인원 ───────────────────────
        private void bindParticipantsRow(Meeting meeting) {
            List<MeetingParticipant> participants = meeting.getParticipants();

            ImageView[] avatars = {
                    binding.ivAvatar1,
                    binding.ivAvatar2,
                    binding.ivAvatar3
            };

            int participantCount = (participants != null) ? participants.size() : 0;

            for (int index = 0; index < avatars.length; index++) {
                if (index < participantCount) {
                    avatars[index].setVisibility(View.VISIBLE);
                    Glide.with(binding.getRoot().getContext())
                            .load(participants.get(index).getUser_profile_image())
                            .placeholder(R.drawable.ic_profile)
                            .circleCrop()
                            .into(avatars[index]);
                } else {
                    avatars[index].setVisibility(View.GONE);
                }
            }

            int needed = meeting.getMeeting_max_participants() - meeting.getMeeting_current_participants();
            binding.tvCampaignParticipantCount.setText(
                    needed > 0 ? "+" + needed + "명 더 모집중" : "정원 마감"
            );
        }

        // ── 마감일 텍스트 (하단 오른쪽) ──────────────────────────
        private void bindDeadlineText(Meeting meeting) {
            String deadline = meeting.getCampaign_deadline();
            if (deadline == null || deadline.isEmpty()) {
                binding.tvCampaignDeadline.setVisibility(View.GONE);
                return;
            }
            try {
                SimpleDateFormat inputFormat  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
                SimpleDateFormat outputFormat = new SimpleDateFormat("MM/dd", Locale.KOREA);
                Date deadlineDate = inputFormat.parse(deadline);
                if (deadlineDate != null) {
                    binding.tvCampaignDeadline.setText("~" + outputFormat.format(deadlineDate) + " 마감");
                    binding.tvCampaignDeadline.setVisibility(View.VISIBLE);
                } else {
                    binding.tvCampaignDeadline.setVisibility(View.GONE);
                }
            } catch (ParseException e) {
                Log.w(TAG, "마감일 파싱 실패: " + deadline);
                binding.tvCampaignDeadline.setVisibility(View.GONE);
            }
        }
    }
}