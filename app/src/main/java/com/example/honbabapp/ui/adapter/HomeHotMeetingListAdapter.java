package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.honbabapp.data.MockData;
import com.example.honbabapp.data.meeting.Meeting;
import com.example.honbabapp.databinding.RvHotMeetingBinding;

import java.util.ArrayList;

public class HomeHotMeetingListAdapter extends RecyclerView.Adapter<HomeHotMeetingListAdapter.ViewHolder>{

    ArrayList<Meeting> hotMeetingList = new ArrayList<>();
    ArrayList<Meeting> allMeetingList = MockData.getMeetingList();

    public HomeHotMeetingListAdapter() {
        for (Meeting m : allMeetingList) {
            if (m.meeting_is_hot) {
                hotMeetingList.add(m);
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

        /// hotMeetingList
        RvHotMeetingBinding rvHotMeetingBinding = RvHotMeetingBinding.inflate(layoutInflater, parent, false);
        ViewHolder viewHolder = new ViewHolder(rvHotMeetingBinding);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Meeting hotMeeting = hotMeetingList.get(position);
        Meeting allMeeting = allMeetingList.get(position);

        /// hotMeetingList
        holder.hotMeetingBinding.tvMeetingTitle.setText(hotMeeting.meeting_title);   ///제목
        holder.hotMeetingBinding.tvMeetingLocation.setText(hotMeeting.meeting_location.location_store_address); /// 주소
        holder.hotMeetingBinding.tvParticipants.setText(hotMeeting.meeting_current_participants + "/" + hotMeeting.meeting_max_participants + "명 참여");

    }

    @Override
    public int getItemCount() {
        return hotMeetingList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        RvHotMeetingBinding hotMeetingBinding;
        public ViewHolder(RvHotMeetingBinding hotMeetingBinding) {
            super(hotMeetingBinding.getRoot());
            this.hotMeetingBinding = hotMeetingBinding;
        }
    }
}
