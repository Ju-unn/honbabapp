package com.example.honbabapp.ui.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.honbabapp.MeetingListActivity;
import com.example.honbabapp.data.MockData;
import com.example.honbabapp.databinding.FragmentHomeBinding;
import com.example.honbabapp.ui.adapter.HomeAllMeetingListAdapter;
import com.example.honbabapp.ui.adapter.HomeHotMeetingListAdapter;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    private HomeHotMeetingListAdapter hotMeetingListAdapter;
    private HomeAllMeetingListAdapter allMeetingListAdapter;


    public HomeFragment() {
        Log.i("HomeFragment" , "생성");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i("HomeFragment" , "onCreateView");

        binding = FragmentHomeBinding.inflate(inflater , container , false);
        hotMeetingListAdapter = new HomeHotMeetingListAdapter();
        allMeetingListAdapter = new HomeAllMeetingListAdapter();

        /// hot meetings recyclerView
        binding.rvHotMeetings.setAdapter(hotMeetingListAdapter);

        /// all meetings recyclerView
        binding.rvMeetings.setAdapter(allMeetingListAdapter);

        binding.categoryAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext() , MeetingListActivity.class);
                startActivity(intent);
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}