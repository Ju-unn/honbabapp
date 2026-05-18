package com.example.honbabapp.ui.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.activity.meeting.MeetingDetailActivity;
import com.example.honbabapp.api.data.response.MyMeetingItem;
import com.example.honbabapp.databinding.RvProfileMeetingItemBinding;

import java.util.ArrayList;
import java.util.List;

public class ProfileMyMeetingAdapter extends RecyclerView.Adapter<ProfileMyMeetingAdapter.ViewHolder> {

    private List<MyMeetingItem> meetings = new ArrayList<>();

    public void updateMeetings(List<MyMeetingItem> newList) {
        this.meetings = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProfileMyMeetingAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RvProfileMeetingItemBinding binding = RvProfileMeetingItemBinding.inflate(LayoutInflater.from(parent.getContext()) , parent , false);
        return new ViewHolder(binding);

    }

    @Override
    public void onBindViewHolder(@NonNull ProfileMyMeetingAdapter.ViewHolder holder, int position) {
        holder.bind(meetings.get(position));
    }

    @Override
    public int getItemCount() {
        return meetings.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        RvProfileMeetingItemBinding binding;

        public ViewHolder(@NonNull RvProfileMeetingItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(MyMeetingItem item) {

            // 클릭 이벤트
            binding.getRoot().setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext() , MeetingDetailActivity.class);
                intent.putExtra("meeting_id" , item.getMeeting_id());
                v.getContext().startActivity(intent);
            });

            // 제목
            binding.tvMeetingTitle.setText(item.getMeeting_title());

            // 카테고리
            binding.tvMeetingCategory.setText(item.getMeeting_category());

            // 날짜 앞 10자리만
            String date = item.getMeeting_date();
            binding.tvMeetingDate.setText(date != null && date.length() >= 10 ? date.substring(0, 10) : date);

            // 인원
            binding.tvParticipants.setText(
                    item.getMeeting_current_participants() + "/"
                            + item.getMeeting_max_participants() + "명");

            // 상태 (취소 > 모집완료 > 모집중 우선순위)
            String statusLabel;
            if ("취소".equals(item.getMeeting_status())) {
                statusLabel = "취소";
            } else if (item.isIs_past() || "모집완료".equals(item.getMeeting_status())) {
                statusLabel = "모집완료";
            } else {
                statusLabel = "모집중";
            }
            binding.tvMeetingStatus.setText(statusLabel);

            // 역할 배지
            boolean isHost = "host".equals(item.getUser_role());
            binding.tvRoleBadge.setText(isHost ? "호스트" : "참여자");

            // 모임 이미지
            String imageUrl = item.getMeeting_image();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(binding.getRoot().getContext())
                        .load(imageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_meeting_image)
                        .into(binding.ivMeetingImage);
            } else {
                binding.ivMeetingImage.setImageResource(R.drawable.placeholder_meeting_image);
            }
        }


    }
}
