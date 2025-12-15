package com.example.honbabapp.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.AvailabilityData;
import com.example.honbabapp.api.data.User;
import com.example.honbabapp.api.data.VerificationData;
import com.example.honbabapp.api.data.VerifiedData;
import com.example.honbabapp.api.data.request.RegisterRequest;
import com.example.honbabapp.api.data.request.SendVerificationRequest;
import com.example.honbabapp.api.data.request.VerifyCodeRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.databinding.PageRegisterBinding;

import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * 회원가입 Activity
 *
 * 검증 항목:
 * 1. 닉네임 중복 확인
 * 2. 이메일 중복 확인
 * 3. 이메일 인증 코드 발송
 * 4. 인증 코드 검증
 * 5. 비밀번호 확인
 * 6. 전화번호 중복 확인
 * 7. 최종 회원가입
 */

public class RegisterActivity extends AppCompatActivity {
    private PageRegisterBinding registerBinding;
    private ApiService apiService;
    private static final String TAG = "RegisterActivity";


    // 검증 완료 플래그
    private boolean isNicknameChecked = false;
    private boolean isEmailChecked = false;
    private boolean isEmailVerified = false;
    private boolean isPasswordConfirmed = false;
    private boolean isPhoneChecked = false;

    // 입력 데이터
    private String selectedGender = "남성"; // 기본값
    private String selectedLocation = "";
    private String verificationCode = ""; // 서버에서 받은 코드 (개발용)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerBinding = PageRegisterBinding.inflate(getLayoutInflater());
        setContentView(registerBinding.getRoot());

        // API 서비스 초기화
        apiService = RetrofitClient.getClient().create(ApiService.class);

        // 뒤로가기 버튼
        registerBinding.btnBack.setOnClickListener(v -> finish());

        // 성별 선택 버튼
        setupGenderButtons();

        // 생년월일 선택
        setupBirthdayPicker();

        // 비밀번호 실시간 검증
        setupPasswordValidation();

        // 전화번호 중복확인
        registerBinding.btnCheckPhone.setOnClickListener(v -> checkPhoneDuplicate());

        // 위치 선택
        registerBinding.layoutLocationSelect.setOnClickListener(v -> {
            // TODO: 위치 선택 BottomSheet 또는 다이얼로그
            Toast.makeText(this , "위치 선택 기능 구현 예정" , Toast.LENGTH_SHORT).show();
        });

        // 닉네임 입력 감지 (중복 확인 초기화)
        registerBinding.etNickname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isNicknameChecked = false; // 닉네임 변경시 재확인 필요
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // 이메일 입력 감지
        registerBinding.etEmail.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isEmailChecked = false;
                isEmailVerified = false;
                registerBinding.btnSendVerification.setText("중복확인");
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        // 전화번호 입력 감지
        registerBinding.etPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isPhoneChecked = false;
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 이메일 중복확인 / 코드 전송 버튼
        registerBinding.btnSendVerification.setOnClickListener(v -> {
            String buttonText = registerBinding.btnSendVerification.getText().toString();

            if (buttonText.equals("중복확인")) {
                checkEmailDuplicate();
            } else if (buttonText.equals("코드 전송")){
                sendVerificationCode();
            }
        });

        // 인증 코드 확인 버튼
        registerBinding.btnVerifyCode.setOnClickListener(v -> verifyEmailCode());

        // 비밀번호 확인 버튼
        registerBinding.btnVerifyPassword.setOnClickListener(v -> checkPasswordMatch());

        // 회원가입 완료 버튼
        registerBinding.btnSignupComplete.setOnClickListener(v -> performRegister());

    }

    /**
     * 성별 선택 버튼 설정 (Selector 사용 - XML에서 처리)
     */
    private void setupGenderButtons() {
        // 기본값 : 남성 선택
        registerBinding.btnGenderMale.setSelected(true);
        registerBinding.btnGenderFemale.setSelected(false);
        selectedGender = "남성";

        // 남성 클릭
        registerBinding.btnGenderMale.setOnClickListener(v -> {
            selectedGender = "남성";
            registerBinding.btnGenderMale.setSelected(true);
            registerBinding.btnGenderFemale.setSelected(false);
        });

        // 여성 클릭
        registerBinding.btnGenderFemale.setOnClickListener(v -> {
            selectedGender = "여성";
            registerBinding.btnGenderFemale.setSelected(true);
            registerBinding.btnGenderMale.setSelected(false);
        });
    }

    /**
     * 생년월일 선택 DatePicker 설정
     */
    private void setupBirthdayPicker() {
        registerBinding.etBirthdate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    RegisterActivity.this ,
                    (view , selectedYear , selectedMonth , selectedDay) -> {
                        String birthday = String.format(
                                "%04d-%02d-%02d",
                                selectedYear,
                                selectedMonth + 1,
                                selectedDay);
                        registerBinding.etBirthdate.setText(birthday);
                    } ,
                    year , month , day
            );
            datePickerDialog.show();
        });
    }

    /**
     * 이메일 중복 확인
     */
    private void checkEmailDuplicate() {
        String email = registerBinding.etEmail.getText().toString().trim();

        // 이메일 형식 검증
        if (email.isEmpty()) {
            Toast.makeText(this, "이메일을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "올바른 이메일 형식이 아닙니다", Toast.LENGTH_SHORT).show();
            return;
        }

        // API 호출
        Call<ApiResponse<AvailabilityData>> call = apiService.checkEmail(email);

        call.enqueue(new Callback<ApiResponse<AvailabilityData>>() {
            @Override
            public void onResponse(Call<ApiResponse<AvailabilityData>> call, Response<ApiResponse<AvailabilityData>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<AvailabilityData> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.getData().isAvailable()) {
                        Toast.makeText(RegisterActivity.this,
                                "사용 가능한 이메일입니다",
                                Toast.LENGTH_SHORT).show();
                        isEmailChecked = true;
                        registerBinding.btnSendVerification.setText("코드 전송");
                    } else {
                        Toast.makeText(RegisterActivity.this,
                                apiResponse.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AvailabilityData>> call, Throwable t) {
                Toast.makeText(RegisterActivity.this,
                        "네트워크 오류: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 이메일 인증 코드 발송
     */
    private void sendVerificationCode() {
        String email = registerBinding.etEmail.getText().toString().trim();

        SendVerificationRequest request = new SendVerificationRequest(email);
        Call<ApiResponse<VerificationData>> call = apiService.sendVerification(request);

        call.enqueue(new Callback<ApiResponse<VerificationData>>() {
            @Override
            public void onResponse(Call<ApiResponse<VerificationData>> call, Response<ApiResponse<VerificationData>> response) {
                if (response.isSuccessful() && response.body() !=  null) {
                    ApiResponse<VerificationData> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        // 개발 단계 : 서버에서 코드를 응답으로 보내줌
                        verificationCode = apiResponse.getData().getCode();

                        Toast.makeText(RegisterActivity.this,
                                "인증 코드가 발송되었습니다\n코드: " + verificationCode,
                                Toast.LENGTH_LONG).show();

                        // 인증 코드 입력란 활성화
                        registerBinding.etVerificationCode.setEnabled(true);
                        registerBinding.btnVerifyCode.setEnabled(true);
                    } else {
                        Toast.makeText(RegisterActivity.this,
                                apiResponse.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }

                }
            }

            @Override
            public void onFailure(Call<ApiResponse<VerificationData>> call, Throwable t) {
                Toast.makeText(RegisterActivity.this,
                        "네트워크 오류: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 인증 코드 검증
     */
    private void verifyEmailCode() {
        String email = registerBinding.etEmail.getText().toString().trim();
        String code = registerBinding.etVerificationCode.getText().toString().trim();

        VerifyCodeRequest request = new VerifyCodeRequest(email , code);
        Call<ApiResponse<VerifiedData>> call = apiService.verifyCode(request);

        call.enqueue(new Callback<ApiResponse<VerifiedData>>() {
            @Override
            public void onResponse(Call<ApiResponse<VerifiedData>> call, Response<ApiResponse<VerifiedData>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<VerifiedData> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.getData().isVerified()) {
                        Toast.makeText(RegisterActivity.this,
                                "이메일 인증이 완료되었습니다",
                                Toast.LENGTH_SHORT).show();

                        isEmailVerified = true;
                        registerBinding.etVerificationCode.setEnabled(false);
                        registerBinding.btnVerifyCode.setEnabled(false);
                    } else {
                        Toast.makeText(RegisterActivity.this,
                                apiResponse.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<VerifiedData>> call, Throwable t) {
                Toast.makeText(RegisterActivity.this,
                        "네트워크 오류: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });

    }

    /**
     * 비밀번호 실시간 검증
     */
    private void setupPasswordValidation() {
        // 비밀번호 입력 시 실시간 검증
        registerBinding.etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String password = s.toString();
                isPasswordConfirmed = false;

                if (!password.isEmpty()) {
                    validatePasswordRules(password);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 비밀번호 확인 실시간 일치 검증
        registerBinding.etPasswordConfirm.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isPasswordConfirmed = false;

                String password = registerBinding.etPassword.getText().toString();
                String passwordConfirm = s.toString();

                if (!password.isEmpty() && !passwordConfirm.isEmpty()) {
                    if (password.equals(passwordConfirm)) {
                        registerBinding.etPasswordConfirm.setError(null);
                    } else {
                        registerBinding.etPasswordConfirm.setError("비밀번호가 일치하지 않습니다");
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void validatePasswordRules(String password) {
        // 길이 확인
        if (password.length() < 8) {
            registerBinding.etPassword.setError("최소 8자 이상");
            return;
        }

        // 공백 확인
        if (password.contains(" ")) {
            registerBinding.etPassword.setError("공백 사용 불가");
            return;
        }

        // 영문 포함 확인 (필수)
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        if (!hasLetter) {
            registerBinding.etPassword.setError("영문 포함 필수");
            return;
        }

        // 숫자 포함 확인 (필수)
        boolean hasDigit = password.matches(".*\\d.*");
        if (!hasDigit) {
            registerBinding.etPassword.setError("숫자 포함 필수");
            return;
        }

        // 유효한 문자 확인
        String validPattern = "^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{}|;:,.<>?]+$";
        if (!password.matches(validPattern)) {
            registerBinding.etPassword.setError("영문, 숫자, 특수문자만 사용 가능");
            return;
        }

        // 모든 규칙 통과
        registerBinding.etPassword.setError(null);
    }

    /**
     * 비밀번호 확인
     */
    private void checkPasswordMatch() {
        String password = registerBinding.etPassword.getText().toString();
        String passwordConfirm = registerBinding.etPasswordConfirm.getText().toString();

        // 비밀번호 일치 여부만 확인
        if (password.equals(passwordConfirm)) {
            Toast.makeText(this, "비밀번호가 일치합니다", Toast.LENGTH_SHORT).show();
            isPasswordConfirmed = true;
        } else {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다", Toast.LENGTH_SHORT).show();
            isPasswordConfirmed = false;
        }
    }

    /**
     * 전화번호 중복 확인
     */
    private void checkPhoneDuplicate() {
        String phone = registerBinding.etPhoneNumber.getText().toString().trim();

        // 1. 전화번호 입력 확인
        if (phone.isEmpty()) {
            Toast.makeText(this, "전화번호를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2. 전화번호 형식 검증 (010-1234-5678 또는 01012345678)
        // 하이픈 제거
        String phoneDigits = phone.replaceAll("-", "");

        // 숫자만 있는지 확인
        if (!phoneDigits.matches("^[0-9]+$")) {
            Toast.makeText(this, "전화번호는 숫자만 입력 가능합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        // 010으로 시작하는지 확인
        if (!phoneDigits.startsWith("010")) {
            Toast.makeText(this, "010으로 시작하는 전화번호를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 길이 확인 (11자리)
        if (phoneDigits.length() != 11) {
            Toast.makeText(this, "전화번호는 11자리를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. API 호출 (서버에 중복 확인 요청)
        Call<ApiResponse<AvailabilityData>> call = apiService.checkPhone(phoneDigits);

        call.enqueue(new Callback<ApiResponse<AvailabilityData>>() {
            @Override
            public void onResponse(Call<ApiResponse<AvailabilityData>> call,
                                   Response<ApiResponse<AvailabilityData>> response) {

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<AvailabilityData> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        if (apiResponse.getData().isAvailable()) {
                            // 사용 가능한 전화번호
                            Toast.makeText(RegisterActivity.this,
                                    "사용 가능한 전화번호입니다",
                                    Toast.LENGTH_SHORT).show();
                            isPhoneChecked = true;

                        } else {
                            // 이미 사용중인 전화번호
                            Toast.makeText(RegisterActivity.this,
                                    apiResponse.getMessage(), // "이미 가입된 전화번호입니다"
                                    Toast.LENGTH_SHORT).show();
                            isPhoneChecked = false;
                        }

                    } else {
                        // API 처리 실패
                        Toast.makeText(RegisterActivity.this,
                                apiResponse.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        isPhoneChecked = false;
                    }

                } else {
                    // 서버 응답 실패
                    Toast.makeText(RegisterActivity.this,
                            "서버 응답 오류: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                    isPhoneChecked = false;
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AvailabilityData>> call, Throwable t) {
                Toast.makeText(RegisterActivity.this,
                        "네트워크 오류: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
                isPhoneChecked = false;
            }
        });
    }

    /**
     * 최종 회원가입
     */
    private void performRegister() {
        // 모든 검증 완료 확인
        if (!isEmailVerified) {
            Toast.makeText(this, "이메일 인증이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isPasswordConfirmed) {
            Toast.makeText(this, "비밀번호 확인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        // 입력값 가져오기
        String nickname = registerBinding.etNickname.getText().toString().trim();
        String email = registerBinding.etEmail.getText().toString().trim();
        String password = registerBinding.etPassword.getText().toString();
        String phoneNumber = registerBinding.etPhoneNumber.getText().toString().trim();
        String birthday = registerBinding.etBirthdate.getText().toString().trim();
        String location = selectedLocation.isEmpty() ? "미설정" : selectedLocation;

        // 필수 입력 확인
        if (nickname.isEmpty()) {
            Toast.makeText(this, "닉네임을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (email.isEmpty()) {
            Toast.makeText(this, "이메일을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (phoneNumber.isEmpty()) {
            Toast.makeText(this, "전화번호를 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (birthday.isEmpty()) {
            Toast.makeText(this, "생년월일을 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // registerRequest 생성
        RegisterRequest request = new RegisterRequest(
                nickname ,
                email ,
                password ,
                phoneNumber ,
                birthday ,
                selectedGender ,
                location
        );

        // API 호출
        Call<ApiResponse<User>> call = apiService.register(request);

        call.enqueue(new Callback<ApiResponse<User>>() {
            @Override
            public void onResponse(Call<ApiResponse<User>> call, Response<ApiResponse<User>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<User> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        Toast.makeText(RegisterActivity.this,
                                "회원가입이 완료되었습니다!",
                                Toast.LENGTH_SHORT).show();

                        // LoginActivity로 돌아가기
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this,
                                apiResponse.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(RegisterActivity.this,
                            "서버 응답 오류: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<User>> call, Throwable t) {
                Log.e(TAG, "회원가입 실패: " + t.getMessage());
                Toast.makeText(RegisterActivity.this,
                        "네트워크 오류: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }














































}