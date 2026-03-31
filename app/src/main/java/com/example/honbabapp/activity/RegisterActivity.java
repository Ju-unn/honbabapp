package com.example.honbabapp.activity;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.honbabapp.BuildConfig;
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
    // TODO: 닉네임 중복 확인 및 특수문자 불가 처리
    // TODO: 회원가입에 필수검증 조건 재확인 필요
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

    // 입력 데이터
    private String selectedGender = "남성"; // 기본값
    private String selectedLocation = "";
    private String verificationCode = ""; // 서버에서 받은 코드 (개발용)

    // 프로필 이미지 관련 변수
    private String uploadedImageUrl = null; // S3 URL
    private boolean isImageUploading = false;

    // 이미지 선택 Launcher
    private ActivityResultLauncher<Intent> imagePickerLauncher;

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

        // 전화번호 실시간 하이폰 추가
        setupPhoneNumberAutoFormat();

        // 이미지 선택하기
        initProfileImagePicker();

        // 전화번호 중복확인
        registerBinding.btnCheckPhone.setOnClickListener(v -> checkPhoneDuplicate());

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
                            Toast.makeText(this, "설정에서 위치 권한을 허용해주세요.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "위치 권한을 허용해주세요.\n다시 클릭해서 진행해 주세요.", Toast.LENGTH_LONG).show();
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
                        Toast.makeText(this,
                                "위치를 가져오지 못했습니다. 다시 시도해주세요.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    registerBinding.tvSelectedLocation.setText("위치를 가져오지 못했습니다");
                    Toast.makeText(this, "위치를 가져오지 못했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(RegisterActivity.this,
                                "동네 설정 완료: " + address, Toast.LENGTH_SHORT).show();
                    } else {
                        registerBinding.tvSelectedLocation.setText("주소를 가져오지 못했습니다");
                        Toast.makeText(RegisterActivity.this,
                                "위치를 가져오지 못했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    registerBinding.tvSelectedLocation.setText("주소를 가져오지 못했습니다");
                    Toast.makeText(RegisterActivity.this,
                            "위치를 가져오지 못했습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<NaverReverseGeoResponse> call, Throwable t) {
                Log.e(TAG, "API 실패: " + t.getMessage());
                registerBinding.tvSelectedLocation.setText("네트워크 오류");
                Toast.makeText(RegisterActivity.this,
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

                   Toast.makeText(RegisterActivity.this,
                           "이미지가 선택되었습니다",
                           Toast.LENGTH_SHORT).show();

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

            // 미래 날짜 선택 불가
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

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
                registerBinding.etPasswordConfirm.setError(null);
                isPasswordConfirmed = false;
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
     * 전화번호 실시간 하이폰 추가
     */
    private void setupPhoneNumberAutoFormat() {
        registerBinding.etPhoneNumber.addTextChangedListener(new PhoneNumberFormattingTextWatcher());
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

        // 2. 전화번호 형식 검증 (010-1234-5678)
        if (!phone.matches("^010-\\d{4}-\\d{4}$")) {
            Toast.makeText(this, "올바른 전화번호 형식이 아닙니다 (010-1234-5678)", Toast.LENGTH_SHORT).show();
            return;
        }

        // 3. API 호출 (서버에 중복 확인 요청)
        Call<ApiResponse<AvailabilityData>> call = apiService.checkPhone(phone);

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
        String location = selectedLocation.isEmpty() ? "서울특별시 관악구" : selectedLocation;

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

        // 프로필 이미지가 있으면 먼저 업로드!
        if (selectedProfileBitmap != null && uploadedImageUrl == null) {
            Toast.makeText(this, "프로필 이미지 업로드 중...", Toast.LENGTH_SHORT).show();
            uploadProfileImageThenRegister(nickname, email, password, phoneNumber, birthday, location);
        } else {
            // 이미지 없거나 이미 업로드됨 → 바로 회원가입
            registerWithData(nickname, email, password, phoneNumber, birthday, location, uploadedImageUrl);
        }
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

                        if (apiResponse.isSuccess()) {
                            ImageUploadData data = apiResponse.getData();
                            uploadedImageUrl = data.getProfile_image_url();

                            Log.d(TAG, "이미지 업로드 성공! S3 URL: " + uploadedImageUrl);

                            // 이미지 업로드 성공 → 회원가입 진행!
                            registerWithData(nickname, email, password, phoneNumber, birthday, location, uploadedImageUrl);

                        } else {
                            Toast.makeText(RegisterActivity.this,
                                    "이미지 업로드 실패: " + apiResponse.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this,
                                "이미지 업로드 실패: " + response.code(),
                                Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<ImageUploadData>> call, Throwable t) {
                    isImageUploading = false;
                    Log.e(TAG, "이미지 업로드 오류: " + t.getMessage());
                    Toast.makeText(RegisterActivity.this,
                            "네트워크 오류: " + t.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });

        } catch (IOException e) {
            isImageUploading = false;
            Log.e(TAG, "파일 변환 오류: " + e.getMessage());
            Toast.makeText(this, "이미지 처리 오류", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 회원가입 API 호출
     */
    private void registerWithData(String nickname, String email, String password,
                                  String phoneNumber, String birthday, String location,
                                  String profileImageUrl) {
        // RegisterRequest 생성
        RegisterRequest request = new RegisterRequest(
                nickname,
                email,
                password,
                phoneNumber,
                birthday,
                selectedGender,
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
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                Log.e(TAG, "회원가입 실패: " + t.getMessage());
                Toast.makeText(RegisterActivity.this,
                        "네트워크 오류: " + t.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });


    }





































}