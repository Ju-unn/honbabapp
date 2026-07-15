package com.example.honbabapp.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.example.honbabapp.R;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.NaverMapsApiService;
import com.example.honbabapp.api.NaverMapsRetrofitClient;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.AvailabilityData;
import com.example.honbabapp.api.data.request.user.CompleteSocialProfileRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.databinding.ActivityOnboardingBinding;
import com.example.honbabapp.ui.dialog.LocationSelectionBottomSheetDialog;
import com.example.honbabapp.util.InsetsUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.Calendar;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 소셜 로그인 온보딩(추가정보 입력) Activity
 *
 * 카카오/구글이 제공하지 않은 정보(위치, 성별, 생일)와
 * 중복으로 비어있는 정보(닉네임, 전화번호)만 조건부로 입력받는다.
 *
 */
public class OnboardingActivity extends AppCompatActivity {

    private static final String TAG = "OnboardingActivity";
    private static final String PREF_NAME = "honbabapp_prefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_SIGUNGU = "user_sigungu";
    private static final String KEY_DONG = "user_dong";

    private ActivityOnboardingBinding binding;
    private ApiService apiService;
    private SharedPreferences sharedPreferences;

    private FusedLocationProviderClient fusedLocationClient;
    private NaverMapsApiService naverMapsApiService;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    private boolean needsNickname;
    private boolean needsPhone;
    private boolean needsLocation;
    private boolean needsGender;
    private boolean needsBirthday;
    private boolean isNicknameChecked = false;
    private boolean isPhoneChecked = false;

    private String selectedGender = null;
    private String selectedBirthday = null;
    private String selectedLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        InsetsUtils.applyRootInsets(binding.getRoot());

        apiService = RetrofitClient.getClient().create(ApiService.class);
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // 온보딩 도중 앱이 강제종료되어도 checkAutoLogin()이 로그인됨으로 판단하지 않도록 방어.
        sharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, false).apply();

        needsNickname = getIntent().getBooleanExtra("needs_nickname", false);
        needsPhone = getIntent().getBooleanExtra("needs_phone", false);
        // 복원된 계정 등 기존 값이 남아있는 필드는 다시 입력받지 않는다
        // (extra를 안 넘기는 기존 호출부와의 호환을 위해 기본값 true = 기존 동작 유지)
        needsLocation = getIntent().getBooleanExtra("needs_location", true);
        needsGender = getIntent().getBooleanExtra("needs_gender", true);
        needsBirthday = getIntent().getBooleanExtra("needs_birthday", true);

        setupNicknameSection();
        setupPhoneSection();
        setupLocationSection();
        setupGenderButtons();
        setupBirthdayPicker();

        binding.btnCompleteOnboarding.setOnClickListener(v -> performComplete());
    }

    /**
     * 닉네임 입력 섹션 - 필요할 때만 표시
     */
    private void setupNicknameSection() {
        if (!needsNickname) {
            binding.layoutNickname.setVisibility(View.GONE);
            return;
        }

        binding.layoutNickname.setVisibility(View.VISIBLE);

        binding.etNickname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isNicknameChecked = false;
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnCheckNickname.setOnClickListener(v -> checkNicknameDuplicate());
    }

    private void checkNicknameDuplicate() {
        String nickname = binding.etNickname.getText().toString().trim();

        if (nickname.isEmpty() || nickname.length() < 2) {
            showNicknameResult(false, "닉네임은 2자 이상 입력해주세요");
            return;
        }
        if (nickname.length() > 10) {
            showNicknameResult(false, "닉네임은 10자 이하로 입력해주세요");
            return;
        }
        if (!nickname.matches("^[가-힣a-zA-Z0-9]+$")) {
            showNicknameResult(false, "특수문자는 사용할 수 없습니다");
            return;
        }

        apiService.checkNickname(nickname).enqueue(new Callback<ApiResponse<AvailabilityData>>() {
            @Override
            public void onResponse(Call<ApiResponse<AvailabilityData>> call, Response<ApiResponse<AvailabilityData>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean available = response.body().isSuccess()
                            && response.body().getData() != null
                            && response.body().getData().isAvailable();

                    if (available) {
                        isNicknameChecked = true;
                        showNicknameResult(true, "사용 가능한 닉네임입니다");
                    } else {
                        isNicknameChecked = false;
                        showNicknameResult(false, "이미 사용중인 닉네임입니다");
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AvailabilityData>> call, Throwable t) {
                Log.e(TAG, "닉네임 중복확인 실패: " + t.getMessage());
                Toast.makeText(OnboardingActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showNicknameResult(boolean success, String message) {
        binding.tvNicknameCheckResult.setVisibility(View.VISIBLE);
        binding.tvNicknameCheckResult.setText(message);
        binding.tvNicknameCheckResult.setTextColor(
                ContextCompat.getColor(this, success ? R.color.primary_100 : android.R.color.holo_red_dark));
    }

    /**
     * 010-1234-5678 형태로 자동 하이폰이 삽입되도록 한다.
     */
    private void setupPhoneSection() {
        if (!needsPhone) {
            binding.layoutPhone.setVisibility(View.GONE);
            return;
        }

        binding.layoutPhone.setVisibility(View.VISIBLE);

        //자동 하이폰 포맷터
        binding.etPhoneNumber.addTextChangedListener(new PhoneNumberFormattingTextWatcher());

        // isPhoneChecked 리셋 (텍스트가 변경되면 재확인 필요)
        binding.etPhoneNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isPhoneChecked = false;
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnCheckPhone.setOnClickListener(v -> checkPhoneDuplicate());
    }

    private void checkPhoneDuplicate() {
        String phone = binding.etPhoneNumber.getText().toString().trim();

        if (!phone.matches("^010-\\d{4}-\\d{4}$")) {
            showPhoneResult(false, "010-0000-0000 형식으로 입력해주세요");
            return;
        }

        // 온보딩은 이미 로그인된 계정의 추가 정보 입력이므로 복원 판정용 이메일은 없음("")
        apiService.checkPhone(phone, "").enqueue(new Callback<ApiResponse<AvailabilityData>>() {
            @Override
            public void onResponse(Call<ApiResponse<AvailabilityData>> call, Response<ApiResponse<AvailabilityData>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean available = response.body().isSuccess()
                            && response.body().getData() != null
                            && response.body().getData().isAvailable();

                    if (available) {
                        isPhoneChecked = true;
                        showPhoneResult(true, "사용 가능한 전화번호입니다");
                    } else {
                        isPhoneChecked = false;
                        // 탈퇴 계정 번호 안내 등 서버가 내려주는 사유를 그대로 표시
                        String message = response.body().getMessage() != null
                                ? response.body().getMessage() : "이미 사용중인 전화번호입니다";
                        showPhoneResult(false, message);
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AvailabilityData>> call, Throwable t) {
                Log.e(TAG, "전화번호 중복확인 실패: " + t.getMessage());
                Toast.makeText(OnboardingActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showPhoneResult(boolean success, String message) {
        binding.tvPhoneCheckResult.setVisibility(View.VISIBLE);
        binding.tvPhoneCheckResult.setText(message);
        binding.tvPhoneCheckResult.setTextColor(
                ContextCompat.getColor(this, success ? R.color.primary_100 : android.R.color.holo_red_dark));
    }

    /**
     * 위치 선택 섹션 - 기존 LocationSelectionBottomSheetDialog 재사용
     */
    private void setupLocationSection() {
        if (!needsLocation) {
            binding.layoutLocationSection.setVisibility(View.GONE);
            return;
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        naverMapsApiService = NaverMapsRetrofitClient.getInstance().getApiService();

        binding.layoutLocationSelect.setOnClickListener(v -> openLocationBottomSheet());
    }

    private void openLocationBottomSheet() {
        LocationSelectionBottomSheetDialog dialog = new LocationSelectionBottomSheetDialog();
        dialog.currentLocationText = selectedLocation;
        dialog.listener = (sigungu, dong) -> {
            selectedLocation = sigungu + " " + dong;
            binding.tvSelectedLocation.setText(selectedLocation);
            binding.tvSelectedLocation.setTextColor(ContextCompat.getColor(this, R.color.text_100));
        };
        dialog.show(getSupportFragmentManager(), "LocationSelectionBottomSheet");
    }

    /**
     * 성별 선택 섹션
     */
    private void setupGenderButtons() {
        if (!needsGender) {
            binding.layoutGenderSection.setVisibility(View.GONE);
            return;
        }

        binding.btnGenderMale.setOnClickListener(v -> selectGender("남성"));
        binding.btnGenderFemale.setOnClickListener(v -> selectGender("여성"));
    }

    private void selectGender(String gender) {
        selectedGender = gender;

        binding.btnGenderMale.setSelected(gender.equals("남성"));
        binding.btnGenderFemale.setSelected(gender.equals("여성"));
    }

    /**
     * 생일 선택 섹션
     */
    private void setupBirthdayPicker() {
        if (!needsBirthday) {
            binding.layoutBirthdaySection.setVisibility(View.GONE);
            return;
        }

        binding.layoutBirthdaySelect.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        selectedBirthday = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                        binding.tvSelectedBirthday.setText(selectedBirthday);
                        binding.tvSelectedBirthday.setTextColor(ContextCompat.getColor(this, R.color.text_100));
                    },
                    calendar.get(Calendar.YEAR) - 20,
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            // 만 10세 미만 가입 불가 (오늘로부터 -10년 이후 날짜는 선택 불가)
            Calendar maxDate = Calendar.getInstance();
            maxDate.add(Calendar.YEAR, -10);
            datePickerDialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());

            // 너무 오래된 날짜 선택 불가
            Calendar minDate = Calendar.getInstance();
            minDate.add(Calendar.YEAR, -100);
            datePickerDialog.getDatePicker().setMinDate(minDate.getTimeInMillis());

            datePickerDialog.show();
        });
    }

    /**
     * 최종 완료 처리
     */
    private void performComplete() {
        String nickname = needsNickname ? binding.etNickname.getText().toString().trim() : null;
        String phone = needsPhone ? binding.etPhoneNumber.getText().toString().trim() : null;

        // 필수 입력값 검증
        if (needsNickname) {
            if (nickname.isEmpty()) {
                Toast.makeText(this, "닉네임을 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isNicknameChecked) {
                Toast.makeText(this, "닉네임 중복확인을 해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (needsPhone) {
            if (phone.isEmpty()) {
                Toast.makeText(this, "전화번호를 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isPhoneChecked) {
                Toast.makeText(this, "전화번호 중복확인을 해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (needsLocation && selectedLocation == null) {
            Toast.makeText(this, "활동 지역을 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (needsGender && selectedGender == null) {
            Toast.makeText(this, "성별을 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        if (needsBirthday && selectedBirthday == null) {
            Toast.makeText(this, "생년월일을 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 숨긴 필드는 null로 전달 → 서버(complete_social_profile.php)가 요청에 포함된 필드만 갱신
        CompleteSocialProfileRequest request = new CompleteSocialProfileRequest(
                nickname, phone, selectedLocation, selectedGender, selectedBirthday
        );

        // 인터셉터가 자동으로 JWT 토큰을 헤더에 첨부함
        apiService.completeSocialProfile(request).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // 위치 정보 및 로그인 완료 플래그를 한 번의 edit()으로 저장
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    if (selectedLocation != null) { // 위치 섹션이 숨겨진 경우(복원 계정) null
                        String[] parts = selectedLocation.split(" ");
                        editor.putString(KEY_SIGUNGU, parts.length > 0 ? parts[0] : "전체");
                        editor.putString(KEY_DONG, parts.length > 1 ? parts[1] : parts[0]);
                    }
                    //온보딩 모든 입력 성공 시 비로소 is_logged_in = true
                    editor.putBoolean(KEY_IS_LOGGED_IN, true);
                    editor.apply();

                    Toast.makeText(OnboardingActivity.this, "환영합니다!", Toast.LENGTH_SHORT).show();
                    goToMainActivity();
                } else {
                    String message = response.body() != null ? response.body().getMessage() : "처리에 실패했습니다";
                    Toast.makeText(OnboardingActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                Log.e(TAG, "온보딩 완료 실패: " + t.getMessage(), t);
                Toast.makeText(OnboardingActivity.this, "네트워크 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goToMainActivity() {
        Intent intent = new Intent(OnboardingActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // 온보딩은 필수 단계이므로 뒤로가기 비활성화 (로그인 화면으로 못 돌아가게)
        Toast.makeText(this, "추가 정보 입력을 완료해주세요", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}