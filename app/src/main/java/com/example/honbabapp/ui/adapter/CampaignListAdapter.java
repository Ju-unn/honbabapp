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
import com.example.honbabapp.activity.campaign.CampaignDetailActivity;
import com.example.honbabapp.api.data.meeting.Meeting;
import com.example.honbabapp.api.data.meeting.MeetingParticipant;
import com.example.honbabapp.databinding.RvCampaignCardBinding;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * 체험단 목록 어댑터
 * rv_meeting_card 스타일 기반
 *
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

    public void updateCampaignList(List<Meeting> meetings) {
        if (meetings == null) {
            Log.w(TAG, "updateCampaignList: meetings is null");
            return;
        }
        campaignMeetingList.clear();
        campaignMeetingList.addAll(meetings);
        notifyDataSetChanged();
        Log.i(TAG, "체험단 목록 업데이트 완료: " + meetings.size() + "개");
    }

    // ─────────────────────────────────────────────────────────────
    // ViewHolder
    // ─────────────────────────────────────────────────────────────

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final RvCampaignCardBinding binding;

        public ViewHolder(@NonNull RvCampaignCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Meeting meeting) {
            bindClickEvent(meeting);
            bindImage(meeting);
            bindDeadlineBadge(meeting);   // 이미지 위 마감임박 뱃지
            bindTitle(meeting);           // 제목 + 체험단 뱃지 (같은 라인)
            bindCampaignProvided(meeting);
            bindCampaignChannels(meeting);
            bindLocationAndDate(meeting);
            bindParticipantsRow(meeting);
            bindDeadlineText(meeting);    // 하단 마감일 텍스트
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

        // ── 마감임박 뱃지 (이미지 우하단 오버레이) ──────────────
        private void bindDeadlineBadge(Meeting meeting) {
            String deadline = meeting.getCampaign_deadline();
            if (deadline == null || deadline.isEmpty()) {
                binding.tvDeadlineBadge.setVisibility(View.GONE);
                return;
            }
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
                Date deadlineDate = dateFormat.parse(deadline);
                if (deadlineDate != null) {
                    long remainingDays = TimeUnit.MILLISECONDS.toDays(
                            deadlineDate.getTime() - System.currentTimeMillis()
                    );
                    binding.tvDeadlineBadge.setVisibility(
                            (remainingDays >= 0 && remainingDays <= 3) ? View.VISIBLE : View.GONE
                    );
                } else {
                    binding.tvDeadlineBadge.setVisibility(View.GONE);
                }
            } catch (ParseException e) {
                binding.tvDeadlineBadge.setVisibility(View.GONE);
            }
        }

        // ── 제목 (왼쪽) + 체험단 뱃지 (오른쪽) ──────────────────
        private void bindTitle(Meeting meeting) {
            binding.tvCampaignTitle.setText(meeting.getMeeting_title());
            // tv_campaign_badge는 항상 "체험단" 고정 텍스트 — 별도 처리 불필요
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

        // ── 홍보 채널 ─────────────────────────────────────────────
        private void bindCampaignChannels(Meeting meeting) {
            String channels = meeting.getCampaign_channels();
            if (channels != null && !channels.isEmpty()) {
                // "블로그,인스타그램" → "블로그 · 인스타그램"
                binding.tvCampaignChannels.setText(channels.replace(",", " · "));
                binding.tvCampaignChannels.setVisibility(View.VISIBLE);
            } else {
                binding.tvCampaignChannels.setVisibility(View.GONE);
            }
        }

        // ── 위치 + 방문 날짜 ──────────────────────────────────────
        private void bindLocationAndDate(Meeting meeting) {
            if (meeting.getLocation() != null) {
                String locationText = meeting.getLocation().getPlace_name();
                if (locationText == null || locationText.isEmpty()) {
                    locationText = meeting.getLocation().getDong();
                }
                binding.tvCampaignLocation.setText(locationText);
            }

            String meetingDate = meeting.getMeeting_date();
            if (meetingDate != null && meetingDate.length() >= 10) {
                binding.tvCampaignDate.setText(meetingDate.substring(0, 10));
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
                    needed > 0 ? "+" + needed + "명 더 필요" : "정원 마감"
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