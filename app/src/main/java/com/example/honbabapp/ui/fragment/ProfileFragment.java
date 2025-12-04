package com.example.honbabapp.ui.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.honbabapp.activity.profile.ProfileAccountSettingActivity;
import com.example.honbabapp.activity.profile.ProfileMyMeetingsActivity;
import com.example.honbabapp.activity.profile.ProfileNotificationSettingActivity;
import com.example.honbabapp.activity.profile.ProfileProfileSettingActivity;
import com.example.honbabapp.databinding.FragmentProfileBinding;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

    public ProfileFragment() {
        Log.i("ProfileFragment" , "생성");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i("ProfileFragment" , "onCreateView");

        binding = FragmentProfileBinding.inflate(inflater , container , false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 내 모임 관리 클릭 이벤트
        menuMyMeetingsOnClick();
        // 알림 설정 클릭 이벤트
        menuNotificationOnclikc();
        // 프로필 수정 클릭 이벤트
        menuProfileSettingOnClick();
        // 계정 / 정보 관리 클릭 이벤트
        menuAccountSettingOnClick();
    }

    /// 내 모임 관리 클릭 이벤트
    public void menuMyMeetingsOnClick() {
        binding.menuMyMeetings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity() , ProfileMyMeetingsActivity.class);
                startActivity(intent);
            }
        });
    }

    /// 알림 설정 클릭 이벤트
    public void menuNotificationOnclikc() {
        binding.menuNotifications.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity() , ProfileNotificationSettingActivity.class);
                startActivity(intent);
            }
        });
    }

    /// 프로필 수정 클릭 이벤트
    public void menuProfileSettingOnClick(){
        binding.menuEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity() , ProfileProfileSettingActivity.class);
                startActivity(intent);
            }
        });
    }

    /// 계정 / 정보 관리 클릭 이벤트
    public void menuAccountSettingOnClick() {
        binding.menuEditAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity() , ProfileAccountSettingActivity.class);
                startActivity(intent);
            }
        });
    }
}