package com.example.honbabapp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.honbabapp.databinding.ActivityMeetingListBinding;
import com.example.honbabapp.ui.adapter.MeetingListAdapter;


public class MeetingListActivity extends AppCompatActivity {
    private ActivityMeetingListBinding binding;
    private MeetingListAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMeetingListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initRecyclerView();
    }

    private void initRecyclerView() {
        adapter = new MeetingListAdapter();
        binding.rvMeetingsList.setAdapter(adapter);
    }


}