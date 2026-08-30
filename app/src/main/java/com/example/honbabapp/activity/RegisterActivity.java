package com.example.honbabapp.activity;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.example.honbabapp.BuildConfig;
import com.example.honbabapp.R;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.NaverMapsApiService;
import com.example.honbabapp.api.NaverMapsRetrofitClient;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.AvailabilityData;
import com.example.honbabapp.api.data.ImageUploadData;
import com.example.honbabapp.api.data.VerificationData;
import com.example.honbabapp.api.data.VerifiedData;
import com.example.honbabapp.api.data.request.RegisterRequest;
import com.example.honbabapp.api.data.request.SendVerificationRequest;
import com.example.honbabapp.api.data.request.VerifyCodeRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.naver.NaverReverseGeoResponse;
import com.example.honbabapp.databinding.PageRegisterBinding;
import com.example.honbabapp.util.InsetsUtils;
import com.example.honbabapp.util.ToastUtil;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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

    // 위치 관련 멤버 변수
    private FusedLocationProviderClient fusedLocationClient;
    private NaverMapsApiService naverMapsApiService;
    private ActivityResultLauncher<String[]> locationPermissionLauncher;


    // 검증 완료 플래그
    private boolean isNicknameChecked = false;
    private boolean isEmailChecked = false;
    private boolean isEmailVerified = false;
    private boolean isPasswordConfirmed = false;
    private boolean isPhoneChecked = false;
    private boolean isRestored = false;

    // 입력 데이터
    private String selectedGender = "남성"; // 기본값
    private String selectedLocation = "";

    // 프로필 이미지 관련 변수
    private String uploadedImageUrl = null; // S3 URL
    private boolean isImageUploading = false;

    // 가입 제출 중 플래그 — 버튼 연타로 register API가 중복 호출되는 것 방지
    private boolean isRegistering = false;

    // 이메일 중복확인/인증코드 전송, 인증코드 검증 진행 중 플래그 — 버튼 연타 방지
    private boolean isSendVerificationRequesting = false;
    private boolean isVerifyCodeRequesting = false;

    // 이미지 선택 Launcher
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    // 이메일 인증번호 만료 타이머 (3분)
    private static final long VERIFICATION_TIMEOUT_MS = 3 * 60 * 1000L;
    private CountDownTimer verificationTimer;

    // 닉네임·전화번호 입력 멈춤 후 자동 중복확인 (debounce 400ms)
    private static final long CHECK_DEBOUNCE_MS = 400L;
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private final Runnable nicknameCheckRunnable = this::checkNicknameDuplicate;
    private final Runnable phoneCheckRunnable = this::checkPhoneDuplicate;

    // 멀티스텝 마법사: 스텝 수 (0=이메일 1=비번 2=닉네임 3=전화 4=프로필)
    private static final int STEP_COUNT = 5;
    private static final String[] STEP_LABELS = {"이메일 인증", "비밀번호 설정", "닉네임", "전화번호", "프로필"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerBinding = PageRegisterBinding.inflate(getLayoutInflater());
        setContentView(registerBinding.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        InsetsUtils.applyRootInsetsWithIme(registerBinding.getRoot());

        // API 서비스 초기화
        apiService = RetrofitClient.getClient().create(ApiService.class);

        // 멀티스텝 마법사 네비게이션(진행바 / 이전·다음 / 뒤로가기 정책)
        setupWizardNavigation();

        // 성별 선택 버튼
        setupGenderButtons();

        // 생년월일 선택
        setupBirthdayPicker();

        // 비밀번호 실시간 검증
        setupPasswordValidation();

        // 전화번호 실시간 하이폰 추가
        setupPhoneNumberAutoFormat();

        // 이미지 선택하기
        initProfileImagePicker();

        // 닉네임·전화번호는 debounce 자동검사(아래 TextWatcher). 수동 중복확인 버튼 없음

        // 위치 서비스 초기화 (onCreate에서 바로 실행)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        naverMapsApiService = NaverMapsRetrofitClient.getInstance().getApiService();

        // 권한 런처 등록 (onCreate에서 바로 등록)
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean fineGranted   = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                    boolean coarseGranted = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));

                    if (fineGranted || coarseGranted) {
                        getCurrentLocation();
                    } else {
                        boolean showRationale = shouldShowRequestPermissionRationale(
                                Manifest.permission.ACCESS_FINE_LOCATION);
                        if (!showRationale) {
                            ToastUtil.makeText(this, "설정에서 위치 권한을 허용해주세요.", Toast.LENGTH_LONG).show();
                        } else {
                            ToastUtil.makeText(this, "위치 권한을 허용해주세요.\n다시 클릭해서 진행해 주세요.", Toast.LENGTH_LONG).show();
                        }
                    }
                }
        );

        //  위치 선택 버튼 클릭
        registerBinding.layoutLocationSelect.setOnClickListener(v -> {
            boolean hasFine   = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            boolean hasCoarse = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

            if (hasFine || hasCoarse) {
                getCurrentLocation();
            } else {
                locationPermissionLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }
        });

        // 닉네임 입력 감지 (중복 확인 초기화)
        registerBinding.etNickname.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isNicknameChecked = false; // 닉네임 변경시 재확인 필요
                updateNextButton();

                // 입력 멈추면 자동 중복확인 (2자 미만이면 대기, 서버 스팸 방지)
                debounceHandler.removeCallbacks(nicknameCheckRunnable);
                if (s.toString().trim().length() < 2) {
                    registerBinding.tvNicknameCheckResult.setVisibility(View.GONE);
                    return;
                }
                debounceHandler.postDelayed(nicknameCheckRunnable, CHECK_DEBOUNCE_MS);
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
                // 코드 전송 직후엔 재전송 스팸 방지로 3분간 disabled 상태인데,
                // 그걸 풀어주는 유일한 통로(타이머 onFinish)를 아래 cancelVerificationTimer()가
                // 끊어버리므로 여기서 직접 다시 눌러지게 해줘야 함
                registerBinding.btnSendVerification.setEnabled(true);
                registerBinding.tvEmailCheckResult.setVisibility(View.GONE);

                // 이전 이메일로 보낸 인증코드는 새 이메일에 더 이상 유효하지 않으므로
                // 코드 입력창/인증확인 버튼/타이머를 전부 초기 상태로 되돌린다.
                // (안 그러면 인증 완료 후 이메일을 고치면 입력창이 disable된 채로 멈춰있음)
                registerBinding.etVerificationCode.setText("");
                registerBinding.etVerificationCode.setEnabled(false);
                registerBinding.btnVerifyCode.setEnabled(false);
                registerBinding.tvVerifyCodeResult.setVisibility(View.GONE);
                cancelVerificationTimer();
                updateNextButton();
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
                registerBinding.tvPhoneCheckResult.setVisibility(View.GONE);
                updateNextButton();

                // 형식(010-XXXX-XXXX)이 완성됐을 때만 자동 중복확인
                debounceHandler.removeCallbacks(phoneCheckRunnable);
                if (s.toString().trim().matches("^010-\\d{4}-\\d{4}$")) {
                    debounceHandler.postDelayed(phoneCheckRunnable, CHECK_DEBOUNCE_MS);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 이메일 중복확인 / 코드 전송 버튼
        registerBinding.btnSendVerification.setOnClickListener(v -> {
            // 요청이 끝나기 전 연타로 중복확인/코드 전송이 여러 번 발생하는 것 방지
            if (isSendVerificationRequesting) {
                return;
            }

            String buttonText = registerBinding.btnSendVerification.getText().toString();

            if (buttonText.equals("중복확인")) {
                checkEmailDuplicate();
            } else if (buttonText.equals("코드 전송")){
                sendVerificationCode();
            }
        });

        // 인증 코드 확인 버튼
        registerBinding.btnVerifyCode.setOnClickListener(v -> {
            // 응답 오기 전 연타로 인증 코드 검증이 여러 번 발생하는 것 방지
            if (isVerifyCodeRequesting) {
                return;
            }
            verifyEmailCode();
        });

    }

    // ==================== 멀티스텝 마법사 네비게이션 ====================

    private void setupWizardNavigation() {
        updateStepUI();

        // 다음 / 회원가입 완료
        registerBinding.btnNext.setOnClickListener(v -> {
            if (registerBinding.viewFlipper.getDisplayedChild() >= STEP_COUNT - 1) {
                performRegister();
            } else {
                goNext();
            }
        });

        // 이전
        registerBinding.btnPrev.setOnClickListener(v -> goPrev());

        // 헤더 뒤로가기 + 시스템 뒤로가기 동일 정책
        registerBinding.btnBack.setOnClickListener(v -> handleBack());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBack();
            }
        });
    }

    // 스텝N>0 → 이전 스텝, 스텝1(=0) → 가입 그만두기 확인
    private void handleBack() {
        if (registerBinding.viewFlipper.getDisplayedChild() > 0) {
            goPrev();
        } else {
            confirmQuit();
        }
    }

    private void goNext() {
        ViewFlipper flipper = registerBinding.viewFlipper;
        if (flipper.getDisplayedChild() >= STEP_COUNT - 1) return;
        flipper.showNext();
        updateStepUI();
    }

    private void goPrev() {
        ViewFlipper flipper = registerBinding.viewFlipper;
        if (flipper.getDisplayedChild() == 0) return;
        flipper.showPrevious();
        updateStepUI();
    }

    // 진행바·인디케이터·버튼 라벨/노출/활성 갱신
    private void updateStepUI() {
        int step = registerBinding.viewFlipper.getDisplayedChild(); // 0-based
        int stepNumber = step + 1;
        registerBinding.progressStep.setProgress(stepNumber);
        registerBinding.tvStepIndicator.setText(stepNumber + "/" + STEP_COUNT);
        registerBinding.tvStepLabel.setText(stepNumber + "단계 · " + STEP_LABELS[step]);
        registerBinding.btnPrev.setVisibility(step == 0 ? View.GONE : View.VISIBLE);
        registerBinding.btnNext.setText(step >= STEP_COUNT - 1 ? "회원가입 완료" : "다음");
        updateNextButton();
    }

    // 현재 스텝의 검증 통과 여부 (통과해야 '다음' 활성화)
    private boolean isCurrentStepValid() {
        switch (registerBinding.viewFlipper.getDisplayedChild()) {
            case 0: return isEmailVerified;      // 이메일 인증 완료
            case 1: return isPasswordConfirmed;  // 비밀번호 확인 일치
            case 2: return isNicknameChecked;    // 닉네임 중복확인 통과
            case 3: return isPhoneChecked;       // 전화번호 중복확인 통과
            default: return true;                // 스텝5는 performRegister에서 최종 검증
        }
    }

    private void updateNextButton() {
        boolean isStepValid = isCurrentStepValid();
        // 비활성 색은 backgroundTint selector(primary_button_bg: disabled→gray)가 처리
        registerBinding.btnNext.setEnabled(isStepValid);
    }

    private void confirmQuit() {
        new AlertDialog.Builder(this)
                .setTitle("가입을 그만두시겠어요?")
                .setMessage("지금까지 입력한 내용이 사라져요.")
                .setPositiveButton("그만두기", (d, w) -> finish())
                .setNegativeButton("계속하기", null)
                .show();
    }

    // 위치 조회 메서드

    private void getCurrentLocation() {
        registerBinding.tvSelectedLocation.setText("위치를 가져오는 중...");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_HIGH_ACCURACY, // GPS 최우선
                        null
                )
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        fetchAddressFromCoords(location.getLongitude(), location.getLatitude());
                    } else {
                        registerBinding.tvSelectedLocation.setText("위치를 가져오지 못했습니다");
                        ToastUtil.makeText(this,
                                "위치를 가져오지 못했습니다. 다시 시도해주세요.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    registerBinding.tvSelectedLocation.setText("위치를 가져오지 못했습니다");
                    ToastUtil.makeText(this, "위치를 가져오지 못했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchAddressFromCoords(double lng, double lat) {
        String coords = lng + "," + lat;
        Log.d(TAG, "좌표: " + coords);
        Log.d(TAG, "Client ID: " + BuildConfig.NAVER_MAPS_CLIENT_ID);

        Call<NaverReverseGeoResponse> call = naverMapsApiService.reverseGeocode(
                BuildConfig.NAVER_MAPS_CLIENT_ID,
                BuildConfig.NAVER_MAPS_CLIENT_SECRET,
                coords, "json", "admcode"
        );

        call.enqueue(new Callback<NaverReverseGeoResponse>() {
            @Override
            public void onResponse(Call<NaverReverseGeoResponse> call,
                                   Response<NaverReverseGeoResponse> response) {
                Log.d(TAG, "응답 코드: " + response.code());
                Log.d(TAG, "body null?: " + (response.body() == null));

                if (response.isSuccessful() && response.body() != null) {
                    String address = response.body().getFormattedAddress();
                    if (address != null && !address.isEmpty()) {
                        selectedLocation = address;
                        registerBinding.tvSelectedLocation.setText(address);
                        ToastUtil.makeText(RegisterActivity.this,
                                "동네 설정 완료: " + address, Toast.LENGTH_SHORT).show();
                    } else {
                        registerBinding.tvSelectedLocation.setText("주소를 가져오지 못했습니다");
                        ToastUtil.makeText(RegisterActivity.this,
                                "위치를 가져오지 못했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    registerBinding.tvSelectedLocation.setText("주소를 가져오지 못했습니다");
                    ToastUtil.makeText(RegisterActivity.this,
                            "위치를 가져오지 못했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<NaverReverseGeoResponse> call, Throwable t) {
                Log.e(TAG, "API 실패: " + t.getMessage());
                registerBinding.tvSelectedLocation.setText("네트워크 오류");
                ToastUtil.makeText(RegisterActivity.this,
                        "위치를 가져오지 못했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 프로필 이미지 선택
     */
    Bitmap selectedProfileBitmap;
    ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia() , uri -> {
               if (uri != null) {
                   Bitmap bitmap;
                   try {
                       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                           bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver() , uri));
                       } else {
                           bitmap = MediaStore.Images.Media.getBitmap(getContentResolver() , uri);
                       }
                   } catch (IOException e) {
                        throw new RuntimeException(e);
                   }

                   registerBinding.ivProfilePreview.setImageBitmap(bitmap);
                   selectedProfileBitmap = bitmap;

                   Log.d("PhotoPicker" , "Selected URI" + uri);
               } else {
                   Log.d("PhotoPicker" , "No media selected");
               }
            });

    private void initProfileImagePicker() {
        registerBinding.btnSelectProfileImage.setOnClickListener(v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });
    }



    /**
     * Bitmap을 File로 변환 (압축 포함)
     */
    private File createImageFileFromBitmap(Bitmap bitmap) throws IOException {
        // 비율 유지하면서 리사이즈 (최대 1024 1024)
        bitmap = resizeBitmap(bitmap , 1024 , 1024);

        // 임시 파일 생성
        File tempFile = File.createTempFile("profile_" , ".jpg" , getCacheDir());

        // jepg 압축
        FileOutputStream fos = new FileOutputStream(tempFile);
        bitmap.compress(Bitmap.CompressFormat.JPEG , 80 , fos);
        fos.close();

        return tempFile;
    }

    /**
     * Bitmap 리사이즈
     */
    private Bitmap resizeBitmap(Bitmap original , int maxWidth , int maxHeight) {
        int width = original.getWidth();
        int height = original.getHeight();

        float ratio = Math.min(
                (float) maxWidth / width ,
                (float) maxHeight / height
        );

        if (ratio >= 1) {
            return original;
        }

        int newWidth = Math.round(width * ratio);
        int newHeight = Math.round(height * ratio);

        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
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
            int currentYear = calendar.get(Calendar.YEAR);
            int currentMonth = calendar.get(Calendar.MONTH);
            int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

            // 기본 선택 날짜: 만 20세
            int defaultYear = currentYear - 20;
            int defaultMonth = currentMonth;
            int defaultDay = currentDay;

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
                    defaultYear  , defaultMonth , defaultDay
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
     * 이메일 중복 확인
     */
    private void checkEmailDuplicate() {
        String email = registerBinding.etEmail.getText().toString().trim();

        // 이메일 형식 검증
        if (email.isEmpty()) {
            showEmailCheckResult(false, "이메일을 입력해주세요");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showEmailCheckResult(false, "올바른 이메일 형식이 아닙니다");
            return;
        }

        // API 호출
        // 전화번호도 함께 전달: 탈퇴 계정의 번호 + 새 이메일 조합이면
        // 서버가 "기존에 사용하던 이메일을 입력해주세요"로 안내해줌
        isSendVerificationRequesting = true;
        registerBinding.btnSendVerification.setEnabled(false);
        Call<ApiResponse<AvailabilityData>> call = apiService.checkEmail(email,
                registerBinding.etPhoneNumber.getText().toString().trim());

        call.enqueue(new Callback<ApiResponse<AvailabilityData>>() {
            @Override
            public void onResponse(Call<ApiResponse<AvailabilityData>> call, Response<ApiResponse<AvailabilityData>> response) {
                isSendVerificationRequesting = false;
                registerBinding.btnSendVerification.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<AvailabilityData> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.getData() != null
                            && apiResponse.getData().isAvailable()) {
                        if (apiResponse.getData().isIs_restored()) {
                            // 탈퇴 계정의 원본 이메일 → 가입 완료 시 계정 복원
                            isRestored = true;
                            showRestoredAccountDialog();
                            // 탈퇴 번호라서 전화번호 확인이 실패해 있었다면,
                            // 복원 이메일이 확인된 지금 자동으로 재검증
                            // (서버가 이메일과 함께 판정하므로 이번엔 통과됨)
                            String phoneInput = registerBinding.etPhoneNumber.getText().toString().trim();
                            if (!isPhoneChecked && phoneInput.matches("^010-\\d{4}-\\d{4}$")) {
                                checkPhoneDuplicate();
                            }
                        } else {
                            isRestored = false;
                            // 복원 다이얼로그로 숨겼던 필드가 있으면 복구
                            registerBinding.layoutBirthday.setVisibility(View.VISIBLE);
                            registerBinding.layoutGender.setVisibility(View.VISIBLE);
                            showEmailCheckResult(true, "사용 가능한 이메일입니다");
                        }
                        isEmailChecked = true;
                        registerBinding.btnSendVerification.setText("코드 전송");
                    } else {
                        showEmailCheckResult(false, apiResponse.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AvailabilityData>> call, Throwable t) {
                isSendVerificationRequesting = false;
                registerBinding.btnSendVerification.setEnabled(true);
                showEmailCheckResult(false, "네트워크 오류: " + t.getMessage());
            }
        });
    }

    // 이메일 중복확인/코드전송 결과 TextView 표시 헬퍼
    private void showEmailCheckResult(boolean success, String message) {
        registerBinding.tvEmailCheckResult.setVisibility(View.VISIBLE);
        registerBinding.tvEmailCheckResult.setText(message);
        registerBinding.tvEmailCheckResult.setTextColor(
                ContextCompat.getColor(this, success ? R.color.success_green : R.color.error_red));
    }

    /**
     * 이메일 인증 코드 발송
     */
    private void sendVerificationCode() {
        String email = registerBinding.etEmail.getText().toString().trim();

        isSendVerificationRequesting = true;
        registerBinding.btnSendVerification.setEnabled(false);
        SendVerificationRequest request = new SendVerificationRequest(email);
        Call<ApiResponse<VerificationData>> call = apiService.sendVerification(request);

        call.enqueue(new Callback<ApiResponse<VerificationData>>() {
            @Override
            public void onResponse(Call<ApiResponse<VerificationData>> call, Response<ApiResponse<VerificationData>> response) {
                isSendVerificationRequesting = false;
                registerBinding.btnSendVerification.setEnabled(true);
                if (response.isSuccessful() && response.body() !=  null) {
                    ApiResponse<VerificationData> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        // 인증코드는 이메일로 발송됨 (응답에 포함되지 않음)
                        showEmailCheckResult(true, "인증 코드가 발송되었습니다.");

                        // 인증 코드 입력란 활성화
                        registerBinding.etVerificationCode.setEnabled(true);
                        registerBinding.btnVerifyCode.setEnabled(true);
                        registerBinding.tvVerifyCodeResult.setVisibility(View.GONE);
                        // 재전송 버튼은 타이머(3분) 동안 비활성화 — 만료 시 startVerificationTimer의 onFinish에서 재활성화
                        registerBinding.btnSendVerification.setEnabled(false);
                        startVerificationTimer();
                    } else {
                        showEmailCheckResult(false, apiResponse.getMessage());
                    }

                }
            }

            @Override
            public void onFailure(Call<ApiResponse<VerificationData>> call, Throwable t) {
                isSendVerificationRequesting = false;
                registerBinding.btnSendVerification.setEnabled(true);
                showEmailCheckResult(false, "네트워크 오류: " + t.getMessage());
            }
        });
    }

    /**
     * 인증 코드 검증
     */
    private void verifyEmailCode() {
        String email = registerBinding.etEmail.getText().toString().trim();
        String code = registerBinding.etVerificationCode.getText().toString().trim();

        isVerifyCodeRequesting = true;
        VerifyCodeRequest request = new VerifyCodeRequest(email , code);
        Call<ApiResponse<VerifiedData>> call = apiService.verifyCode(request);

        call.enqueue(new Callback<ApiResponse<VerifiedData>>() {
            @Override
            public void onResponse(Call<ApiResponse<VerifiedData>> call, Response<ApiResponse<VerifiedData>> response) {
                isVerifyCodeRequesting = false;
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<VerifiedData> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.getData() != null
                            && apiResponse.getData().isVerified()) {
                        showVerifyCodeResult(true, "이메일 인증이 완료되었습니다");

                        isEmailVerified = true;
                        registerBinding.etVerificationCode.setEnabled(false);
                        registerBinding.btnVerifyCode.setEnabled(false);
                        cancelVerificationTimer();
                        updateNextButton();
                    } else {
                        showVerifyCodeResult(false, apiResponse.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<VerifiedData>> call, Throwable t) {
                isVerifyCodeRequesting = false;
                showVerifyCodeResult(false, "네트워크 오류: " + t.getMessage());
            }
        });

    }

    // 인증번호 확인 결과 TextView 표시 헬퍼
    private void showVerifyCodeResult(boolean success, String message) {
        registerBinding.tvVerifyCodeResult.setVisibility(View.VISIBLE);
        registerBinding.tvVerifyCodeResult.setText(message);
        registerBinding.tvVerifyCodeResult.setTextColor(
                ContextCompat.getColor(this, success ? R.color.success_green : R.color.error_red));
    }

    /**
     * 인증번호 만료 타이머 시작 (3분, 재전송 시 재시작)
     */
    private void startVerificationTimer() {
        cancelVerificationTimer();
        registerBinding.tvVerificationTimer.setVisibility(View.VISIBLE);
        registerBinding.tvVerificationTimer.setTextColor(
                ContextCompat.getColor(this, R.color.primary_200));

        verificationTimer = new CountDownTimer(VERIFICATION_TIMEOUT_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long totalSeconds = millisUntilFinished / 1000;
                long minutes = totalSeconds / 60;
                long seconds = totalSeconds % 60;
                registerBinding.tvVerificationTimer.setText(
                        String.format("남은 시간 %d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                registerBinding.tvVerificationTimer.setText("인증번호가 만료되었습니다. 다시 전송해주세요");
                registerBinding.tvVerificationTimer.setTextColor(
                        ContextCompat.getColor(RegisterActivity.this, R.color.error_red));
                registerBinding.etVerificationCode.setEnabled(false);
                registerBinding.btnVerifyCode.setEnabled(false);
                registerBinding.btnSendVerification.setEnabled(true);
            }
        }.start();
    }

    private void cancelVerificationTimer() {
        if (verificationTimer != null) {
            verificationTimer.cancel();
            verificationTimer = null;
        }
        registerBinding.tvVerificationTimer.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancelVerificationTimer();
        debounceHandler.removeCallbacksAndMessages(null);
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

                if (!password.isEmpty()) {
                    validatePasswordRules(password);
                }
                checkPasswordMatch();
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
                checkPasswordMatch();
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
     * 비밀번호 확인 (버튼 없이 실시간으로 호출됨)
     */
    private void checkPasswordMatch() {
        String password = registerBinding.etPassword.getText().toString();
        String passwordConfirm = registerBinding.etPasswordConfirm.getText().toString();

        if (passwordConfirm.isEmpty()) {
            registerBinding.tvPasswordMatch.setVisibility(View.GONE);
            isPasswordConfirmed = false;
            updateNextButton();
            return;
        }

        registerBinding.tvPasswordMatch.setVisibility(View.VISIBLE);

        if (password.equals(passwordConfirm)) {
            registerBinding.tvPasswordMatch.setVisibility(View.GONE);
            isPasswordConfirmed = true;
        } else {
            registerBinding.tvPasswordMatch.setText("비밀번호가 일치하지 않습니다");
            registerBinding.tvPasswordMatch.setTextColor(
                    ContextCompat.getColor(this, R.color.error_red));
            isPasswordConfirmed = false;
        }
        updateNextButton();
    }

    /**
     * 전화번호 실시간 하이폰 추가
     */
    private void setupPhoneNumberAutoFormat() {
        // 국가코드 미지정 시 기기의 SIM/로케일에 따라 "010-1234-5678"이 아닌 다른 포맷으로
        // 하이픈이 들어가 정규식 검증에 실패할 수 있어 "KR"로 고정
        registerBinding.etPhoneNumber.addTextChangedListener(new PhoneNumberFormattingTextWatcher("KR"));
    }

    /**
     * 전화번호 중복 확인
     */
    private void checkPhoneDuplicate() {
        String phone = registerBinding.etPhoneNumber.getText().toString().trim();

        // 1. 전화번호 입력 확인
        if (phone.isEmpty()) {
            showPhoneCheckResult(false, "전화번호를 입력해주세요");
            return;
        }

        // 2. 전화번호 형식 검증 (010-1234-5678)
        if (!phone.matches("^010-\\d{4}-\\d{4}$")) {
            showPhoneCheckResult(false, "올바른 전화번호 형식이 아닙니다 (010-1234-5678)");
            return;
        }

        // 3. API 호출 (서버에 중복 확인 요청)
        // 이메일을 함께 보내는 이유: 탈퇴 계정의 번호라도 그 계정의 원본 이메일로
        // 가입(=복원) 중이면 서버가 허용해줌
        Call<ApiResponse<AvailabilityData>> call = apiService.checkPhone(phone,
                registerBinding.etEmail.getText().toString().trim());

        call.enqueue(new Callback<ApiResponse<AvailabilityData>>() {
            @Override
            public void onResponse(Call<ApiResponse<AvailabilityData>> call,
                                   Response<ApiResponse<AvailabilityData>> response) {

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<AvailabilityData> apiResponse = response.body();

                    if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                        if (apiResponse.getData().isAvailable()) {
                            // 복원 여부(isRestored)는 이메일 확인이 판정한다.
                            // 여기서 건드리면 "복원 이메일 확인 → 새 전화번호 확인" 순서일 때
                            // isRestored가 false로 덮여 숨겨진 생년월일 입력을 요구하게 됨
                            isPhoneChecked = true;
                            showPhoneCheckResult(true, apiResponse.getMessage());
                        } else {
                            // 이미 사용중인 전화번호
                            showPhoneCheckResult(false, apiResponse.getMessage()); // "이미 가입된 전화번호입니다"
                            isPhoneChecked = false;
                        }
                        updateNextButton();

                    } else {
                        // API 처리 실패
                        showPhoneCheckResult(false, apiResponse.getMessage());
                        isPhoneChecked = false;
                    }

                } else {
                    // 서버 응답 실패
                    showPhoneCheckResult(false, "서버 응답 오류: " + response.code());
                    isPhoneChecked = false;
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AvailabilityData>> call, Throwable t) {
                showPhoneCheckResult(false, "네트워크 오류: " + t.getMessage());
                isPhoneChecked = false;
            }
        });
    }

    // 전화번호 중복확인 결과 TextView 표시 헬퍼
    private void showPhoneCheckResult(boolean success, String message) {
        registerBinding.tvPhoneCheckResult.setVisibility(View.VISIBLE);
        registerBinding.tvPhoneCheckResult.setText(message);
        registerBinding.tvPhoneCheckResult.setTextColor(
                ContextCompat.getColor(this, success ? R.color.success_green : R.color.error_red));
    }

    private void showRestoredAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("탈퇴한 유저 입니다.")
                .setMessage("이전에 사용하시던 계정이 있습니다. \n" +
                        "복원을 진행하겠습니다\n" + "닉네임과 비밀번호를 새로 설정해주세요.")
                .setPositiveButton("확인" , (dialog, which) -> {
                    registerBinding.layoutBirthday.setVisibility(View.GONE);
                    registerBinding.layoutGender.setVisibility(View.GONE);
                })
                .setCancelable(false)
                .show();
    }

    /**
     * 닉네임 중복확인
     */
    private void checkNicknameDuplicate() {
        String nickname = registerBinding.etNickname.getText().toString().trim();

        // 1. 길이 검증
        if (nickname.isEmpty() || nickname.length() < 2) {
            showNicknameResult(false, "닉네임은 2자 이상 입력해주세요");
            return;
        }
        if (nickname.length() > 10) {
            showNicknameResult(false, "닉네임은 10자 이하로 입력해주세요");
            return;
        }

        // 2. 특수문자 검증 (한글/영문/숫자만 허용)
        if (!nickname.matches("^[가-힣a-zA-Z0-9]+$")) {
            showNicknameResult(false, "특수문자는 사용할 수 없습니다");
            return;
        }

        // 3. 숫자로만 이루어진 닉네임 금지
        if (nickname.matches("^[0-9]+$")) {
            showNicknameResult(false, "닉네임은 숫자로만 만들 수 없습니다");
            return;
        }

        // 4. 서버에 중복 확인
        apiService.checkNickname(nickname)
                .enqueue(new Callback<ApiResponse<AvailabilityData>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AvailabilityData>> call,
                                           Response<ApiResponse<AvailabilityData>> response) {
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
                            updateNextButton();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<AvailabilityData>> call, Throwable t) {
                        Log.e(TAG, "닉네임 중복확인 실패: " + t.getMessage());
                        ToastUtil.makeText(RegisterActivity.this,
                                "네트워크 오류", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 닉네임 결과 TextView 표시 헬퍼
    private void showNicknameResult(boolean success, String message) {
        registerBinding.tvNicknameCheckResult.setVisibility(
                android.view.View.VISIBLE);
        registerBinding.tvNicknameCheckResult.setText(message);
        registerBinding.tvNicknameCheckResult.setTextColor(
                ContextCompat.getColor(this, success ? R.color.success_green : R.color.error_red));
    }

    /**
     * 최종 회원가입
     */
    private void performRegister() {
        // 이미 제출 중이면 무시 (연타 방지)
        if (isRegistering) {
            return;
        }

        // 스텝0~3(이메일 인증·비번·닉네임·전화)은 위저드 게이팅(updateNextButton)이
        // 통과 전 '다음'을 막으므로 스텝5까지 왔으면 이미 완료·유효 상태 → 재검증 불필요.
        // 게이트가 없는 스텝5(프로필)만 아래 두 항목을 최종 확인한다.

        // 입력값 가져오기
        String nickname = registerBinding.etNickname.getText().toString().trim();
        String email = registerBinding.etEmail.getText().toString().trim();
        String password = registerBinding.etPassword.getText().toString();
        String phoneNumber = registerBinding.etPhoneNumber.getText().toString().trim();
        String birthday = registerBinding.etBirthdate.getText().toString().trim();
        String location = selectedLocation;

        if (location.isEmpty()) {
            ToastUtil.makeText(this, "활동 지역을 설정해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 복원 계정은 생년월일 입력을 받지 않음(서버가 기존값 유지)
        if (!isRestored && birthday.isEmpty()) {
            ToastUtil.makeText(this, "생년월일을 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        // 제출 시작 — 응답/실패 시 finishRegisterSubmission()으로 복구
        startRegisterSubmission();

        // 프로필 이미지가 있으면 먼저 업로드!
        if (selectedProfileBitmap != null && uploadedImageUrl == null) {
            uploadProfileImageThenRegister(nickname, email, password, phoneNumber, birthday, location);
        } else {
            // 이미지 없거나 이미 업로드됨 → 바로 회원가입
            registerWithData(nickname, email, password, phoneNumber, birthday, location, uploadedImageUrl);
        }
    }

    private void startRegisterSubmission() {
        isRegistering = true;
        registerBinding.btnNext.setEnabled(false);
    }

    private void finishRegisterSubmission() {
        isRegistering = false;
        registerBinding.btnNext.setEnabled(true);
    }

    /**
     * 이미지 업로드 후 회원가입
     */
    private void uploadProfileImageThenRegister(String nickname, String email, String password,
                                                String phoneNumber, String birthday, String location) {
        if (isImageUploading) {
            return;
        }

        isImageUploading = true;

        try {
            // Bitmap → File 변환
            File imageFile = createImageFileFromBitmap(selectedProfileBitmap);

            // RequestBody 생성
            RequestBody requestFile = RequestBody.create(
                    MediaType.parse("image/*"),
                    imageFile
            );

            // MultipartBody.Part 생성
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
                    "profile_image",
                    imageFile.getName(),
                    requestFile
            );

            // user_id
            RequestBody userIdBody = RequestBody.create(
                    MediaType.parse("text/plain"),
                    "0"
            );

            // API 호출
            Call<ApiResponse<ImageUploadData>> call = apiService.uploadProfileImage(imagePart, userIdBody);

            call.enqueue(new Callback<ApiResponse<ImageUploadData>>() {
                @Override
                public void onResponse(Call<ApiResponse<ImageUploadData>> call,
                                       Response<ApiResponse<ImageUploadData>> response) {
                    isImageUploading = false;

                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse<ImageUploadData> apiResponse = response.body();

                        if (apiResponse.isSuccess() && apiResponse.getData() != null) {
                            ImageUploadData data = apiResponse.getData();
                            uploadedImageUrl = data.getProfile_image_url();

                            Log.d(TAG, "이미지 업로드 성공! S3 URL: " + uploadedImageUrl);

                            // 이미지 업로드 성공 → 회원가입 진행!
                            registerWithData(nickname, email, password, phoneNumber, birthday, location, uploadedImageUrl);

                        } else {
                            ToastUtil.makeText(RegisterActivity.this,
                                    "이미지 업로드 실패: " + apiResponse.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            finishRegisterSubmission();
                        }
                    } else {
                        ToastUtil.makeText(RegisterActivity.this,
                                "이미지 업로드 실패: " + response.code(),
                                Toast.LENGTH_SHORT).show();
                        finishRegisterSubmission();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<ImageUploadData>> call, Throwable t) {
                    isImageUploading = false;
                    Log.e(TAG, "이미지 업로드 오류: " + t.getMessage());
                    ToastUtil.makeText(RegisterActivity.this,
                            "네트워크 오류: " + t.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finishRegisterSubmission();
                }
            });

        } catch (IOException e) {
            isImageUploading = false;
            Log.e(TAG, "파일 변환 오류: " + e.getMessage());
            ToastUtil.makeText(this, "이미지 처리 오류", Toast.LENGTH_SHORT).show();
            finishRegisterSubmission();
        }
    }

    /**
     * 회원가입 API 호출
     */
    private void registerWithData(String nickname, String email, String password,
                                  String phoneNumber, String birthday, String location,
                                  String profileImageUrl) {

        // 복원 계정은 birthday/gender 입력을 받지 않으므로 null 전송
        // 서버의 COALESCE(:birthday, ...), COALESCE(:gender, ...)가 기존 값을 유지
        // (여기서 selectedGender를 보내면 기본값 "남성"이 원래 성별을 덮어씀)
        RegisterRequest request = new RegisterRequest(
                nickname,
                email,
                password,
                phoneNumber,
                isRestored ? null : birthday,
                isRestored ? null : selectedGender,
                location,
                profileImageUrl  // S3 URL (null 가능)
        );

        // API 호출
        Call<ApiResponse<Void>> call = apiService.register(request);

        call.enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<Void> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        ToastUtil.makeText(RegisterActivity.this,
                                "회원가입이 완료되었습니다!",
                                Toast.LENGTH_SHORT).show();

                        // LoginActivity로 돌아가기
                        finish();
                    } else {
                        ToastUtil.makeText(RegisterActivity.this,
                                apiResponse.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        finishRegisterSubmission();
                    }
                } else {
                    ToastUtil.makeText(RegisterActivity.this,
                            "서버 응답 오류: " + response.code(),
                            Toast.LENGTH_SHORT).show();
                    finishRegisterSubmission();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e(TAG, "회원가입 실패: " + t.getMessage());
                ToastUtil.makeText(RegisterActivity.this,
                        "네트워크 오류: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
                finishRegisterSubmission();
            }
        });

    }

    /**
     * 입력창 바깥(다른 버튼/영역)을 터치하면 포커스를 해제하고 키보드를 내린다.
     * EditText가 포커스된 상태에서 그 영역 밖을 눌러도 키보드가 안 내려가던 문제 수정.
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View focused = getCurrentFocus();
            if (focused instanceof EditText) {
                Rect outRect = new Rect();
                focused.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) event.getRawX(), (int) event.getRawY())) {
                    focused.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(event);

    }





































}