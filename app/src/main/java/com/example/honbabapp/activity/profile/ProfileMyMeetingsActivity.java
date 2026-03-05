package com.example.honbabapp.activity.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.honbabapp.activity.meeting.MeetingCardWritingActivity;
import com.example.honbabapp.databinding.PageProfileMyMeetingsBinding;

public class ProfileMyMeetingsActivity extends AppCompatActivity {

    private PageProfileMyMeetingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageProfileMyMeetingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 뒤로가기 버튼
        onBackButtonClick();

        // 미팅 만들기 버튼
        meetingCardWritingOnClick();
    }

    /// 뒤로가기 버튼
    public void onBackButtonClick(){
        binding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    /// 모임 만들기 버튼
    public void meetingCardWritingOnClick() {
        binding.btnCreateMeeting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileMyMeetingsActivity.this , MeetingCardWritingActivity.class);
                startActivity(intent);
            }
        });
    }
}