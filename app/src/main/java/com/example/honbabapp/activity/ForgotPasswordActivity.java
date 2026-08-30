package com.example.honbabapp.activity;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.example.honbabapp.R;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.VerificationData;
import com.example.honbabapp.api.data.VerifiedData;
import com.example.honbabapp.api.data.request.ResetPasswordRequest;
import com.example.honbabapp.api.data.request.SendPasswordResetRequest;
import com.example.honbabapp.api.data.request.VerifyCodeRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.databinding.ActivityForgotPasswordBinding;
import com.example.honbabapp.util.InsetsUtils;
import com.example.honbabapp.util.ToastUtil;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 비밀번호 찾기 Activity (RegisterActivity의 이메일 인증 플로우와 동일 패턴)
 * 1단계: 가입 이메일 입력 → 인증코드 발송 (발송 후 3분간 재발송 버튼 비활성화)
 * 2단계: 인증코드 확인 → 성공 시 새 비밀번호 입력란 활성화
 * 3단계: 새 비밀번호 실시간 검증 + 일치 확인 → 비밀번호 재설정
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    private static final long VERIFICATION_TIMEOUT_MS = 3 * 60 * 1000L;
    private static final String PURPOSE_PASSWORD_RESET = "password_reset";

    private ActivityForgotPasswordBinding binding;
    private ApiService apiService;

    private boolean isCodeVerified = false;
    private boolean isPasswordConfirmed = false;

    private CountDownTimer verificationTimer;

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

        // 이메일을 다시 수정하면 이전 발송/인증 결과를 초기화
        binding.etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tvEmailCheckResult.setVisibility(View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 1단계: 인증코드 발송
        binding.btnSendCode.setOnClickListener(v -> sendResetCode());

        // 2단계: 인증코드 확인
        binding.btnVerifyCode.setOnClickListener(v -> verifyResetCode());

        // 새 비밀번호 실시간 검증
        setupPasswordValidation();

        // 3단계: 비밀번호 재설정
        binding.btnResetPassword.setOnClickListener(v -> resetPassword());
    }

    /**
     * 비밀번호 재설정 인증코드 발송
     */
    private void sendResetCode() {
        String email = binding.etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            showEmailCheckResult(false, "이메일을 입력해주세요");
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

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    showEmailCheckResult(true, "인증 코드가 발송되었습니다. \n메일함을 확인해주세요");

                    // 인증 코드 입력란 활성화
                    binding.btnSendCode.setText("재발송");
                    binding.etCode.setEnabled(true);
                    binding.btnVerifyCode.setEnabled(true);
                    binding.tvVerifyCodeResult.setVisibility(View.GONE);

                    // 재발송 버튼은 타이머(3분) 동안 비활성화 — 만료 시 onFinish에서 재활성화
                    binding.btnSendCode.setEnabled(false);
                    startVerificationTimer();
                } else {
                    String message = response.body() != null
                            ? response.body().getMessage() : "인증코드 발송에 실패했습니다";
                    showEmailCheckResult(false, message);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<VerificationData>> call, Throwable t) {
                if (binding == null) return;
                binding.btnSendCode.setEnabled(true);
                showEmailCheckResult(false, "네트워크 오류: " + t.getMessage());
            }
        });
    }

    // 코드 발송 결과 TextView 표시 헬퍼
    private void showEmailCheckResult(boolean success, String message) {
        binding.tvEmailCheckResult.setVisibility(View.VISIBLE);
        binding.tvEmailCheckResult.setText(message);
        binding.tvEmailCheckResult.setTextColor(
                ContextCompat.getColor(this, success ? R.color.success_green : R.color.error_red));
    }

    /**
     * 인증코드 검증
     */
    private void verifyResetCode() {
        String email = binding.etEmail.getText().toString().trim();
        String code = binding.etCode.getText().toString().trim();

        if (code.isEmpty()) {
            showVerifyCodeResult(false, "인증코드를 입력해주세요");
            return;
        }

        binding.btnVerifyCode.setEnabled(false);

        VerifyCodeRequest request = new VerifyCodeRequest(email, code, PURPOSE_PASSWORD_RESET);
        apiService.verifyCode(request).enqueue(new Callback<ApiResponse<VerifiedData>>() {
            @Override
            public void onResponse(Call<ApiResponse<VerifiedData>> call, Response<ApiResponse<VerifiedData>> response) {
                if (binding == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<VerifiedData> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.getData() != null && apiResponse.getData().isVerified()) {
                        showVerifyCodeResult(true, "인증이 완료되었습니다. \n새 비밀번호를 입력해주세요");

                        isCodeVerified = true;
                        binding.etCode.setEnabled(false);
                        binding.btnVerifyCode.setEnabled(false);
                        cancelVerificationTimer();

                        binding.etNewPassword.setEnabled(true);
                        binding.etConfirmPassword.setEnabled(true);
                        updateResetButtonState();
                    } else {
                        binding.btnVerifyCode.setEnabled(true);
                        showVerifyCodeResult(false, apiResponse.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<VerifiedData>> call, Throwable t) {
                if (binding == null) return;
                binding.btnVerifyCode.setEnabled(true);
                showVerifyCodeResult(false, "네트워크 오류: " + t.getMessage());
            }
        });
    }

    // 인증번호 확인 결과 TextView 표시 헬퍼
    private void showVerifyCodeResult(boolean success, String message) {
        binding.tvVerifyCodeResult.setVisibility(View.VISIBLE);
        binding.tvVerifyCodeResult.setText(message);
        binding.tvVerifyCodeResult.setTextColor(
                ContextCompat.getColor(this, success ? R.color.success_green : R.color.error_red));
    }

    /**
     * 인증번호 만료 타이머 시작 (3분)
     */
    private void startVerificationTimer() {
        cancelVerificationTimer();
        binding.tvVerificationTimer.setVisibility(View.VISIBLE);
        binding.tvVerificationTimer.setTextColor(ContextCompat.getColor(this, R.color.primary_200));

        verificationTimer = new CountDownTimer(VERIFICATION_TIMEOUT_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long totalSeconds = millisUntilFinished / 1000;
                long minutes = totalSeconds / 60;
                long seconds = totalSeconds % 60;
                binding.tvVerificationTimer.setText(String.format("남은 시간 %d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                binding.tvVerificationTimer.setText("인증번호가 만료되었습니다. 다시 전송해주세요");
                binding.tvVerificationTimer.setTextColor(ContextCompat.getColor(ForgotPasswordActivity.this, R.color.error_red));
                binding.etCode.setEnabled(false);
                binding.btnVerifyCode.setEnabled(false);
                binding.btnSendCode.setEnabled(true);
            }
        }.start();
    }

    private void cancelVerificationTimer() {
        if (verificationTimer != null) {
            verificationTimer.cancel();
            verificationTimer = null;
        }
        binding.tvVerificationTimer.setVisibility(View.GONE);
    }

    /**
     * 새 비밀번호 실시간 검증 (RegisterActivity.setupPasswordValidation과 동일 패턴)
     */
    private void setupPasswordValidation() {
        binding.etNewPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String password = s.toString();
                if (!password.isEmpty()) {
                    validatePasswordRules(password);
                }
                checkPasswordMatch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.etConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkPasswordMatch();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void validatePasswordRules(String password) {
        if (password.length() < 8) {
            binding.etNewPassword.setError("최소 8자 이상");
            return;
        }
        if (password.contains(" ")) {
            binding.etNewPassword.setError("공백 사용 불가");
            return;
        }
        if (!password.matches(".*[a-zA-Z].*")) {
            binding.etNewPassword.setError("영문 포함 필수");
            return;
        }
        if (!password.matches(".*\\d.*")) {
            binding.etNewPassword.setError("숫자 포함 필수");
            return;
        }
        String validPattern = "^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?]+$";
        if (!password.matches(validPattern)) {
            binding.etNewPassword.setError("영문, 숫자, 특수문자만 사용 가능");
            return;
        }
        binding.etNewPassword.setError(null);
    }

    private boolean isNewPasswordValid() {
        String password = binding.etNewPassword.getText().toString();
        return password.length() >= 8
                && !password.contains(" ")
                && password.matches(".*[a-zA-Z].*")
                && password.matches(".*\\d.*")
                && password.matches("^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?]+$");
    }

    private void checkPasswordMatch() {
        String newPw = binding.etNewPassword.getText().toString();
        String confirmPw = binding.etConfirmPassword.getText().toString();

        if (confirmPw.isEmpty()) {
            binding.tvPasswordMatch.setVisibility(View.GONE);
            isPasswordConfirmed = false;
            updateResetButtonState();
            return;
        }

        binding.tvPasswordMatch.setVisibility(View.VISIBLE);

        if (newPw.equals(confirmPw)) {
            binding.tvPasswordMatch.setVisibility(View.GONE);
            isPasswordConfirmed = true;
        } else {
            binding.tvPasswordMatch.setText("비밀번호가 일치하지 않습니다");
            binding.tvPasswordMatch.setTextColor(ContextCompat.getColor(this, R.color.error_red));
            isPasswordConfirmed = false;
        }
        updateResetButtonState();
    }

    // 인증 완료 + 새 비밀번호 형식 통과 + 일치 확인까지 되어야 재설정 버튼 활성화
    private void updateResetButtonState() {
        binding.btnResetPassword.setEnabled(isCodeVerified && isPasswordConfirmed && isNewPasswordValid());
    }

    /**
     * 인증코드 검증 + 새 비밀번호로 즉시 변경
     */
    private void resetPassword() {
        String email = binding.etEmail.getText().toString().trim();
        String code = binding.etCode.getText().toString().trim();
        String newPw = binding.etNewPassword.getText().toString().trim();

        binding.btnResetPassword.setEnabled(false);

        ResetPasswordRequest request = new ResetPasswordRequest(email, code, newPw);
        apiService.resetPassword(request).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call,
                                    Response<ApiResponse<Object>> response) {
                if (binding == null) return;

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    showResetResult(true, "비밀번호가 변경되었습니다. 새 비밀번호로 로그인해주세요");
                    ToastUtil.makeText(ForgotPasswordActivity.this,
                            "비밀번호가 변경되었습니다", Toast.LENGTH_SHORT).show();
                    binding.etCode.postDelayed(ForgotPasswordActivity.this::finish, 1200);
                } else {
                    binding.btnResetPassword.setEnabled(true);
                    String message = response.body() != null
                            ? response.body().getMessage() : "비밀번호 변경에 실패했습니다";
                    showResetResult(false, message);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                if (binding == null) return;
                binding.btnResetPassword.setEnabled(true);
                showResetResult(false, "네트워크 오류: " + t.getMessage());
            }
        });
    }

    // 최종 재설정 결과 메시지 (비밀번호 재설정 버튼 바로 위 tv_password_match 재사용)
    private void showResetResult(boolean success, String message) {
        binding.tvPasswordMatch.setVisibility(View.VISIBLE);
        binding.tvPasswordMatch.setText(message);
        binding.tvPasswordMatch.setTextColor(
                ContextCompat.getColor(this, success ? R.color.success_green : R.color.error_red));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelVerificationTimer();
        binding = null;
    }
}
