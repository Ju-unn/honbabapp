package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
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
        MyMeetingItem item = meetings.get(position);

        // 제목
        holder.binding.tvMeetingTitle.setText(item.getMeeting_title());

        // 카테고리
        holder.binding.tvMeetingCategory.setText(item.getMeeting_category());

        // 날짜 앞 10자리만
        String date = item.getMeeting_date();
        holder.binding.tvMeetingDate.setText(date != null && date.length() >= 10 ? date.substring(0, 10) : date);

        // 인원
        holder.binding.tvParticipants.setText(
                item.getMeeting_current_participants() + "/"
                        + item.getMeeting_max_participants() + "명");

        // 상태
        holder.binding.tvMeetingStatus.setText(item.getMeeting_status());

        // 역할 배지
        boolean isHost = "host".equals(item.getUser_role());
        holder.binding.tvRoleBadge.setText(isHost ? "호스트" : "참여자");

        // 모임 이미지
        String imageUrl = item.getMeeting_image();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.binding.getRoot().getContext())
                    .load(imageUrl)
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_meeting_image)
                    .into(holder.binding.ivMeetingImage);
        } else {
            holder.binding.ivMeetingImage.setImageResource(R.drawable.placeholder_meeting_image);
        }
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
    }
}
