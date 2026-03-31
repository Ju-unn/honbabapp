package com.example.honbabapp.activity.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.UserProfileResponse;
import com.example.honbabapp.databinding.PageProfileAccountsettingBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileAccountSettingActivity extends AppCompatActivity {

    private static final String TAG = "ProfileAccountSetting";
    private PageProfileAccountsettingBinding binding;
    private ApiService apiService;

    private static final String PREF_NAME = "honbabapp_prefs";
    private static final String KEY_USER_ID = "user_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageProfileAccountsettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getClient().create(ApiService.class);

        // 유저 정보 로드
        loadAccountInfo();

        // 뒤로가기
        binding.btnBack.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed());

        // 비밀번호 변경 이동
        binding.btnChangePassword.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileChangePasswordActivity.class)));

        // 계정 탈퇴 이동
        binding.btnDeleteAccount.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileDeleteAccountActivity.class)));
    }

    private void loadAccountInfo() {
        int userId = getSharedPreferences(PREF_NAME , 0).getInt(KEY_USER_ID, -1);
        if (userId == -1) return;

        apiService.getUserProfile(userId)
                .enqueue(new Callback<ApiResponse<UserProfileResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<UserProfileResponse>> call, Response<ApiResponse<UserProfileResponse>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {

                            UserProfileResponse profile = response.body().getData();

                            // 이메일
                            binding.tvEmail.setText(profile.getUser_email()
                            );

                            // 생년월일
                            String birthday = profile.getUser_birthday();
                            binding.tvBirthdate.setText((birthday != null && !birthday.isEmpty()) ? birthday : "-"
                            );

                            // 성별
                            String gender = profile.getUser_gender();
                            binding.tvGender.setText(
                                    (gender != null && !gender.isEmpty()) ? gender : "-"
                            );

                        }

                        }

                    @Override
                    public void onFailure(Call<ApiResponse<UserProfileResponse>> call, Throwable t) {
                        Log.e(TAG, "계정 정보 로드 실패: " + t.getMessage());
                    }
                });
    }

}