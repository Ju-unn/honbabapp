package com.example.honbabapp.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.activity.profile.ProfileAccountSettingActivity;
import com.example.honbabapp.activity.profile.ProfileMyMeetingsActivity;
import com.example.honbabapp.activity.profile.ProfileNotificationSettingActivity;
import com.example.honbabapp.activity.profile.ProfileProfileSettingActivity;
import com.example.honbabapp.activity.profile.ProfileTicketActivity;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.user.UserProfileResponse;
import com.example.honbabapp.databinding.FragmentProfileBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 프로필 탭 Fragment
 */
public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private FragmentProfileBinding binding;
    private ApiService apiService;

    private static final String PREF_NAME = "honbabapp_prefs";
    private static final String KEY_USER_ID = "user_id";

    // 프로필 수정 화면 결과 수신용 런처
    private final ActivityResultLauncher<Intent> profileSettingLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            loadProfile(); // 프로필 변경 즉시 반영
                        }
                    }
            );

    public ProfileFragment() {
        Log.i("ProfileFragment" , "생성");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentProfileBinding.inflate(inflater , container , false);
        apiService = RetrofitClient.getClient().create(ApiService.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // API 호출
        loadProfile();

        // 체험단 등록권 클릭 이벤트
        menuTicketOnClick();
        // 내 모임 관리 클릭 이벤트
        menuMyMeetingsOnClick();
        // 알림 설정 클릭 이벤트
        menuNotificationOnclikc();
        // 프로필 수정 클릭 이벤트
        menuProfileSettingOnClick();
        // 계정 / 정보 관리 클릭 이벤트
        menuAccountSettingOnClick();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfile();
    }

    private void loadProfile() {
        // SharedPreferences에서 로그인시 저장해둔 user_id 읽기
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(PREF_NAME , 0);
        int userId = prefs.getInt(KEY_USER_ID , -1);

        if (userId == -1) {
            Log.e(TAG , "user_id 없음 - 로그인 필요");
            return;
        }


        apiService.getUserProfile(userId).enqueue(new Callback<ApiResponse<UserProfileResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserProfileResponse>> call, Response<ApiResponse<UserProfileResponse>> response) {
                if (response.isSuccessful() &&
                        response.body() != null &&
                        response.body().isSuccess()) {

                    UserProfileResponse profile = response.body().getData();
                    bindProfie(profile);
                } else {
                    Log.e(TAG, "프로필 로드 실패");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UserProfileResponse>> call, Throwable t) {
                Log.e(TAG, "프로필 로드 실패: " + t.getMessage());
            }
        });
    }

    private void bindProfie(UserProfileResponse profile) {
        // 닉네임
        binding.tvProfileNickname.setText(profile.getUser_nickname());

        // 상태 메세지
        String status = profile.getUser_status_message();
        binding.tvProfileStatus.setText(
                (status != null && !status.isEmpty()) ? status : "상태 메세지를 입력해주세요"
        );

        // 매너 점수
        binding.tvMannerScore.setText(String.valueOf(profile.getUser_manner_score()));

        // 참여 횟수
        binding.tvParticipationCount.setText(profile.getUser_participation_count() + "회");

        // 이번달 요약
        binding.tvMonthlySummary.setText(profile.getUser_participation_count() + "회 모임 참여 , 매너점수 " + profile.getUser_manner_score() + " 유지 중!");

        // 프로필 이미지
        String imageUrl = profile.getUser_profile_image();
        Log.i(TAG , "이미지 : " + imageUrl);

        Glide.with(requireContext())
                .load(imageUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_user)
                .into(binding.ivProfileAvatar);
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
    /// ProfileProfileSettingActivity의 setResult(RESULT_OK)를 받아
    /// 저장 직후 프로필 화면에 변경사항을 즉시 반영한다 (저장 후 갱신 버그 수정)
    public void menuProfileSettingOnClick(){
        binding.menuEditProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity() , ProfileProfileSettingActivity.class);
                profileSettingLauncher.launch(intent);
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

    /// 체험단 등록권 클릭 이벤트
    public void menuTicketOnClick() {
        binding.menuTicket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity() , ProfileTicketActivity.class);
                startActivity(intent);
            }
        });
    }
}