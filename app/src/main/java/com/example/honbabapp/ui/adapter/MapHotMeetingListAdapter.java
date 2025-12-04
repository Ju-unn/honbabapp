package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.honbabapp.data.MockData;
import com.example.honbabapp.data.meeting.Meeting;
import com.example.honbabapp.databinding.RvHotMeetingBinding;
import com.example.honbabapp.databinding.RvMeetingCardBinding;

import java.util.ArrayList;

public class MapHotMeetingListAdapter extends RecyclerView.Adapter<MapHotMeetingListAdapter.ViewHolder> {
    ArrayList<Meeting> hotMeetingList = new ArrayList<>();
    ArrayList<Meeting> allMeetingList = MockData.getMeetingList();

    public MapHotMeetingListAdapter() {
        for (Meeting m : allMeetingList) {
            if (m.meeting_is_hot) {
                hotMeetingList.add(m);
            }
        }
    }

    @NonNull
    @Override
    public MapHotMeetingListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

        /// hotMeetingList
        RvMeetingCardBinding rvMeetingCardBinding = RvMeetingCardBinding.inflate(layoutInflater , parent , false);
        ViewHolder viewHolder = new ViewHolder(rvMeetingCardBinding);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull MapHotMeetingListAdapter.ViewHolder holder, int position) {
        Meeting hotMeeting = hotMeetingList.get(position);

        /// hotMeetingList
        holder.hotMeetingBinding.tvMeetingTitle.setText(hotMeeting.meeting_title);
        holder.hotMeetingBinding.tvMeetingLocation.setText(hotMeeting.meeting_location.location_store_address);
        holder.hotMeetingBinding.tvMeetingDescription.setText(hotMeeting.meeting_description);
    }

    @Override
    public int getItemCount() {
        return hotMeetingList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        RvMeetingCardBinding hotMeetingBinding;

        public ViewHolder(RvMeetingCardBinding hotMeetingBinding) {
            super(hotMeetingBinding.getRoot());
            this.hotMeetingBinding = hotMeetingBinding;
        }
    }
}
