package com.example.honbabapp.activity;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.VerificationData;
import com.example.honbabapp.api.data.request.ResetPasswordRequest;
import com.example.honbabapp.api.data.request.SendPasswordResetRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.databinding.ActivityForgotPasswordBinding;
import com.example.honbabapp.util.InsetsUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 비밀번호 찾기 Activity
 * 1단계: 가입 이메일 입력 → 인증코드 발송
 * 2단계: 인증코드 입력 → 임시 비밀번호가 메일로 발급됨 (화면에는 표시하지 않음)
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgotPasswordActivity";

    private ActivityForgotPasswordBinding binding;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        InsetsUtils.applyRootInsets(binding.getRoot());

        apiService = RetrofitClient.getClient().create(ApiService.class);

        // 뒤로가기
        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // 1단계: 인증코드 발송
        binding.btnSendCode.setOnClickListener(v -> sendResetCode());

        // 2단계: 임시 비밀번호 발급
        binding.btnResetPassword.setOnClickListener(v -> resetPassword());
    }

    /**
     * 비밀번호 재설정 인증코드 발송
     */
    private void sendResetCode() {
        String email = binding.etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "이메일을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSendCode.setEnabled(false);

        SendPasswordResetRequest request = new SendPasswordResetRequest(email);
        apiService.sendPasswordReset(request).enqueue(new Callback<ApiResponse<VerificationData>>() {
            @Override
            public void onResponse(Call<ApiResponse<VerificationData>> call,
                                   Response<ApiResponse<VerificationData>> response) {
                if (binding == null) return;
                binding.btnSendCode.setEnabled(true);

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().isSuccess()) {

                    Toast.makeText(ForgotPasswordActivity.this,
                            "인증 코드가 발송되었습니다\n메일함을 확인해주세요",
                            Toast.LENGTH_LONG).show();

                    // 재발송 대비 버튼 문구 변경 + 2단계 활성화
                    binding.btnSendCode.setText("재발송");
                    binding.etCode.setEnabled(true);
                    binding.btnResetPassword.setEnabled(true);
                } else {
                    String message = response.body() != null
                            ? response.body().getMessage() : "인증코드 발송에 실패했습니다";
                    Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<VerificationData>> call, Throwable t) {
                if (binding == null) return;
                binding.btnSendCode.setEnabled(true);
                Log.e(TAG, "인증코드 발송 네트워크 오류: " + t.getMessage());
                Toast.makeText(ForgotPasswordActivity.this,
                        "네트워크 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 인증코드 검증 + 임시 비밀번호 발급 (임시 비밀번호는 메일로만 전달됨)
     */
    private void resetPassword() {
        String email = binding.etEmail.getText().toString().trim();
        String code = binding.etCode.getText().toString().trim();

        if (code.isEmpty()) {
            Toast.makeText(this, "인증코드를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnResetPassword.setEnabled(false);

        ResetPasswordRequest request = new ResetPasswordRequest(email, code);
        apiService.resetPassword(request).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call,
                                   Response<ApiResponse<Object>> response) {
                if (binding == null) return;

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().isSuccess()) {

                    Toast.makeText(ForgotPasswordActivity.this,
                            "임시 비밀번호가 이메일로 발송되었습니다\n로그인 후 비밀번호를 변경해주세요",
                            Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    binding.btnResetPassword.setEnabled(true);
                    String message = response.body() != null
                            ? response.body().getMessage() : "임시 비밀번호 발급에 실패했습니다";
                    Toast.makeText(ForgotPasswordActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                if (binding == null) return;
                binding.btnResetPassword.setEnabled(true);
                Log.e(TAG, "임시 비밀번호 발급 네트워크 오류: " + t.getMessage());
                Toast.makeText(ForgotPasswordActivity.this,
                        "네트워크 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
