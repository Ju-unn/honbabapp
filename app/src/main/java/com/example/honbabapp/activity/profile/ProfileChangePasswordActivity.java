package com.example.honbabapp.activity.profile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.example.honbabapp.R;
import com.example.honbabapp.activity.LoginActivity;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.request.user.ChangePasswordRequest;
import com.example.honbabapp.api.data.request.user.CheckCurrentPasswordRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.databinding.PageProfileChangePasswordBinding;
import com.example.honbabapp.util.InsetsUtils;
import com.example.honbabapp.util.ToastUtil;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileChangePasswordActivity extends AppCompatActivity {


    private static final String TAG = "ProfileChangePassword";
    private PageProfileChangePasswordBinding binding;
    private ApiService apiService;

    private static final String PREF_NAME = "honbabapp_prefs";
    private static final String KEY_USER_ID = "user_id";

    // 현재 비밀번호 검증상태
    private boolean isCurrentPasswordVerified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageProfileChangePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        InsetsUtils.applyRootInsetsWithIme(binding.getRoot());

        apiService = RetrofitClient.getClient().create(ApiService.class);

        // 뒤로가기
        binding.btnBack.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed());

        // 현재 비밀번호 포커스 잃을때 서버 검증
        setupCurrentPasswordVerification();

        // 새 비밀번호 확인 - 실시간 일치 여부 체크
        setupPasswordMatchWatcher();

        // 변경 버튼
        binding.btnChangePassword.setOnClickListener(v -> changePassword());
    }

    // 현재 비밀번호 입력란 검사
    private void setupCurrentPasswordVerification() {

        // 입력값이 바뀌면 검증 상태 리셋
        binding.etCurrentPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //  타이핑 시작하면 이전 검증 결과 초기화
                isCurrentPasswordVerified = false;
                binding.tvCurrentPasswordStatus.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


        // 포커스 잃을때 서버에 확인
        binding.etCurrentPassword.setOnFocusChangeListener((v , hasFocus) -> {
            if (!hasFocus) {
                String pw = binding.etCurrentPassword.getText().toString().trim();
                if (!pw.isEmpty()) {
                    checkCurrentPassword(pw);
                }
            }
        });
    }

    // 현재 비밀번호 서버에서 검사
    private void checkCurrentPassword(String currentPassword) {
        CheckCurrentPasswordRequest request = new CheckCurrentPasswordRequest(currentPassword);

        apiService.checkCurrentPassword(request)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {

                            isCurrentPasswordVerified = true;
                            binding.tvCurrentPasswordStatus.setVisibility(View.VISIBLE);
                            binding.tvCurrentPasswordStatus.setText("현재 비밀번호가 확인되었습니다");
                            binding.tvCurrentPasswordStatus.setTextColor(
                                    ContextCompat.getColor(ProfileChangePasswordActivity.this, R.color.success_green));
                        } else {
                            isCurrentPasswordVerified = false;
                            binding.tvCurrentPasswordStatus.setVisibility(View.VISIBLE);
                            binding.tvCurrentPasswordStatus.setText("현재 비밀번호가 일치하지 않습니다");
                            binding.tvCurrentPasswordStatus.setTextColor(
                                    ContextCompat.getColor(ProfileChangePasswordActivity.this,
                                            R.color.error_red));
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        Log.e(TAG, "현재 비밀번호 확인 실패: " + t.getMessage());
                    }
                });
    }

    private void setupPasswordMatchWatcher() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateNewPassword(binding.etNewPassword.getText().toString().trim());
                checkPasswordMatch();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };

        // 두 EditText 모두 같은 watcher 달기
        binding.etNewPassword.addTextChangedListener(watcher);
        binding.etConfirmPassword.addTextChangedListener(watcher);
    }

    private void checkPasswordMatch() {
        String newPw = binding.etNewPassword.getText().toString();
        String confirmPw = binding.etConfirmPassword.getText().toString();

        // 둘 다 입력 중일 때만 표시
        if (confirmPw.isEmpty()) {
            binding.tvPasswordMatch.setVisibility(View.GONE);
            return;
        }

        binding.tvPasswordMatch.setVisibility(android.view.View.VISIBLE);

        if (newPw.equals(confirmPw)) {
            binding.btnChangePassword.setEnabled(true);
            binding.tvPasswordMatch.setVisibility(View.GONE);
        }  else {
            binding.tvPasswordMatch.setText("비밀번호가 일치하지 않습니다");
            binding.tvPasswordMatch.setTextColor(
                    ContextCompat.getColor(this, R.color.error_red));
            binding.btnChangePassword.setEnabled(false);
        }
    }

    private void changePassword() {

        String currentPw = binding.etCurrentPassword.getText().toString().trim();
        String newPw = binding.etNewPassword.getText().toString().trim();
        String confirmPw = binding.etConfirmPassword.getText().toString().trim();

        if (currentPw.equals(newPw)) {
            ToastUtil.makeText(this, "현재 비밀번호와 동일 합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentPw.isEmpty()) {
            binding.etCurrentPassword.setError("현재 비밀번호를 입력해주세요");
            return;
        }

        // 현재 비밀번호 검증 완료 여부 체크
        if (!isCurrentPasswordVerified) {
            ToastUtil.makeText(this, "현재 비밀번호를 먼저 확인해주세요", Toast.LENGTH_SHORT).show();
            binding.etCurrentPassword.requestFocus(); // 현재 비밀번호 칸으로 포커스 이동
            return;
        }

        // 비밀번호 검증
        if (!validateNewPassword(newPw)) return;
        binding.btnChangePassword.setEnabled(false);
        
        // 2차 검증 1차비번 2차비번 맞는지 확인
        if (!newPw.equals(confirmPw)) {
            binding.etConfirmPassword.setError("비밀번호가 일치하지 않습니다");
            return;
        }


        ChangePasswordRequest request = new ChangePasswordRequest(currentPw , newPw);

        apiService.changePassword(request)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        binding.btnChangePassword.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            ToastUtil.makeText(ProfileChangePasswordActivity.this, "비밀번호가 변경되었습니다. 다시 로그인해주세요", Toast.LENGTH_SHORT).show();
                            redirectToLogin();
                        } else {
                            // 현재 비밀번호 틀림 등 서버 에러 메시지 표시
                            String msg = response.body() != null
                                    ? response.body().getMessage() : "변경 실패";
                            binding.etCurrentPassword.setError(msg);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        binding.btnChangePassword.setEnabled(true);
                        Log.e(TAG, "비밀번호 변경 실패: " + t.getMessage());
                        ToastUtil.makeText(ProfileChangePasswordActivity.this,
                                "네트워크 오류", Toast.LENGTH_SHORT).show();
                    }
                });

    }

    // 비밀번호 변경 성공 시 세션을 비우고 로그인 화면으로 이동 (변경된 비밀번호로 재로그인 유도)
    private void redirectToLogin() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().clear().apply();

        Intent loginIntent = new Intent(this, LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
    }

    private boolean validateNewPassword(String password) {
        if (password.length() < 8) {
            binding.etNewPassword.setError("최소 8자 이상");
            return false;
        }
        if (password.contains(" ")) {
            binding.etNewPassword.setError("공백 사용 불가");
            return false;
        }
        if (!password.matches(".*[a-zA-Z].*")) {
            binding.etNewPassword.setError("영문 포함 필수");
            return false;
        }
        if (!password.matches(".*\\d.*")) {
            binding.etNewPassword.setError("숫자 포함 필수");
            return false;
        }
        String validPattern = "^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?]+$";
        if (!password.matches(validPattern)) {
            binding.etNewPassword.setError("영문, 숫자, 특수문자만 사용 가능");
            return false;
        }
        return true;
    }



}
