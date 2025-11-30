package com.example.honbabapp.ui.adapter;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.honbabapp.data.MockData;
import com.example.honbabapp.data.meeting.Meeting;
import com.example.honbabapp.databinding.RvMapMeetingCardBinding;

import java.util.ArrayList;

public class HomeAllMeetingListAdapter extends RecyclerView.Adapter<HomeAllMeetingListAdapter.ViewHolder> {
    ArrayList<Meeting> allMeetingList = MockData.getMeetingList();


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

        /// allMeetingList
        RvMapMeetingCardBinding rvMapMeetingCardBinding = RvMapMeetingCardBinding.inflate(layoutInflater,parent,false);
        ViewHolder viewHolder = new ViewHolder(rvMapMeetingCardBinding);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Meeting allMeeting = allMeetingList.get(position);

        holder.allMeetingBinding.tvMeetingTitle.setText(allMeeting.meeting_title);
        holder.allMeetingBinding.tvMeetingLocation.setText(allMeeting.meeting_location.location_store_address);
        holder.allMeetingBinding.tvMeetingDescription.setText(allMeeting.meeting_description);
    }

    @Override
    public int getItemCount() {
        Log.i("allMeetingSize" , ""+ allMeetingList.size());
        return allMeetingList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        RvMapMeetingCardBinding allMeetingBinding;
        public ViewHolder(RvMapMeetingCardBinding allMeetingBinding) {
            super(allMeetingBinding.getRoot());
            this.allMeetingBinding = allMeetingBinding;
        }
    }
}
