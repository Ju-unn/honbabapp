package com.example.honbabapp.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.example.honbabapp.BuildConfig;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.login.LoginData;
import com.example.honbabapp.api.data.login.SocialLoginData;
import com.example.honbabapp.api.data.request.LoginRequest;
import com.example.honbabapp.api.data.request.SocialLoginRequest;
import com.example.honbabapp.api.data.request.fcm.RegisterFcmTokenRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.user.OnboardingStatusResponse;
import com.example.honbabapp.databinding.ActivityLoginBinding;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.messaging.FirebaseMessaging;
import com.kakao.sdk.auth.model.OAuthToken;
import com.kakao.sdk.common.model.ClientError;
import com.kakao.sdk.common.model.ClientErrorCause;
import com.kakao.sdk.user.UserApiClient;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    /**
     * 로그인 Activity (최적화 버전)
     * <p>
     * SharedPreferences 저장:
     * - user_id (int)         : API 호출 시 사용
     * - is_logged_in (boolean): 자동 로그인 체크
     * <p>
     * 나머지 정보는 서버에서 가져오기:
     * - nickname, email, profile_image, location, manner_temp 등
     */
    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding binding;
    private ApiService apiService;

    private CredentialManager credentialManager;
    private SharedPreferences sharedPreferences;

    // SharedPreferences 설정
    private static final String PREF_NAME = "honbabapp_prefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_SIGUNGU = "user_sigungu";
    private static final String KEY_DONG = "user_dong";
    private static final String KEY_FCM_TOKEN = "fcm_token";
    private static final String KEY_JWT_TOKEN = "jwt_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        apiService = RetrofitClient.getClient().create(ApiService.class);

        if (checkAutoLogin()) {
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeComponents();
        setupListeners();

    }

    /**
     * 컴포넌트 초기화
     */
    private void initializeComponents() {
        apiService = RetrofitClient.getClient().create(ApiService.class);
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        credentialManager = CredentialManager.create(this);

        Log.d(TAG, "LoginActivity 초기화 완료");
    }

    /**
     * 자동 로그인 확인
     * SharedPreferences에 로그인 정보가 있다면 자동으로 메인 화면으로 이동
     */
    private boolean checkAutoLogin() {
        boolean isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
        int userId = sharedPreferences.getInt(KEY_USER_ID, -1);

        if (isLoggedIn && userId != -1) {
            Log.d(TAG, "자동 로그인 시도: user_id=" + userId + " (온보딩 완료 여부 확인 중)");
            verifyOnboardingThenProceed(userId);
            return true;
        }

        Log.d(TAG, "자동 로그인 정보 없음 — 로그인 화면 표시");
        return false;
    }

    private void verifyOnboardingThenProceed(int userId) {
        apiService.checkOnboardingStatus().enqueue(new Callback<ApiResponse<OnboardingStatusResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<OnboardingStatusResponse>> call, Response<ApiResponse<OnboardingStatusResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    OnboardingStatusResponse status = response.body().getData();

                    if (status.isNeeds_onboarding()) {
                        Log.w(TAG, "온보딩 미완료 계정 - OnboardingActivity로 이동");
                        goToOnboardingActivityFromAutoLogin(status.getMissing());
                    } else {
                        goToMainActivity();
                    }
                } else {
                    // 프로필 조회 실패(탈퇴/존재하지 않는 계정 등) - 안전하게 로그인 화면 유지
                    Log.e(TAG, "자동 로그인 온보딩 상태 조회 실패 - 로그인 화면 표시");
                    showLoginScreen();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<OnboardingStatusResponse>> call, Throwable t) {
                // 네트워크 오류 - 일단 메인으로 보냄 (오프라인 상태에서도 앱 사용 가능하도록)
                Log.e(TAG, "자동 로그인 온보딩 상태 조회 네트워크 오류: " + t.getMessage(), t);
                goToMainActivity();
            }
        });
    }

    /**
     * 자동 로그인 과정에서 온보딩 미완료가 확인된 경우 사용.
     * 서버가 정확히 어떤 필드가 비어있는지 알려주므로, 그대로 전달한다.
     */
    private void goToOnboardingActivityFromAutoLogin(OnboardingStatusResponse.MissingFields missing) {
        Intent intent = new Intent(LoginActivity.this, OnboardingActivity.class);
        intent.putExtra("needs_nickname", missing.isNickname());
        intent.putExtra("needs_phone", missing.isPhone_number());
        intent.putExtra("needs_location", missing.isLocation());
        intent.putExtra("needs_gender", missing.isGender());
        intent.putExtra("needs_birthday", missing.isBirthday());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * 자동 로그인이 실패로 판명된 경우, 로그인 화면 UI를 정상적으로 그린다.
     * (checkAutoLogin이 onCreate 최상단에서 호출되어 binding이 아직 없으므로 직접 구성)
     */
    private void showLoginScreen() {
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initializeComponents();
        setupListeners();
    }

    /**
     * 이벤트 리스너 설정
     */
    private void setupListeners() {
        // 로그인 버튼
        binding.btnLogin.setOnClickListener(v -> performLogin());

        // 회원가입 링크
        binding.tvSingupLink.setOnClickListener(v -> goToRegisterActivity());

        binding.btnKakaoLogin.setOnClickListener(v -> performKakaoLogin());

        binding.btnGoogleLogin.setOnClickListener(v -> performGoogleLogin());
    }

    /**
     * 구글 로그인 수행 (Credential Manager 방식)
     * 시스템이 제공하는 계정 선택 UI를 띄우고, 선택된 계정의 id_token을 받아 서버로 전송
     */
    private void performGoogleLogin() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        Executor executor = Executors.newSingleThreadExecutor();

        credentialManager.getCredentialAsync(
                this,
                request,
                new android.os.CancellationSignal(),
                executor,
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        handleGoogleCredentialResult(result);
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        Log.e(TAG, "구글 로그인 실패", e);
                        runOnUiThread(() ->
                                Toast.makeText(LoginActivity.this, "구글 로그인에 실패했습니다", Toast.LENGTH_SHORT).show()
                        );
                    }
                }
        );
    }

    /**
     * Credential Manager 결과에서 구글 id_token 추출
     * 백그라운드 스레드(executor)에서 호출되므로, UI 작업은 runOnUiThread로 감싸야 함
     */
    private void handleGoogleCredentialResult(GetCredentialResponse result) {
        Credential credential = result.getCredential();

        if (credential instanceof androidx.credentials.CustomCredential) {
            androidx.credentials.CustomCredential customCredential = (androidx.credentials.CustomCredential) credential;

            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(customCredential.getType())) {
                try {
                    GoogleIdTokenCredential googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(customCredential.getData());

                    String idToken = googleIdTokenCredential.getIdToken();
                    Log.d(TAG, "구글 로그인 성공, id_token 획득");

                    sendSocialLoginToServer("google", null, idToken);

                } catch (Exception e) {
                    Log.e(TAG, "구글 id_token 파싱 실패", e);
                    runOnUiThread(() ->
                            Toast.makeText(this, "구글 로그인에 실패했습니다", Toast.LENGTH_SHORT).show()
                    );
                }
            } else {
                Log.e(TAG, "예상치 못한 credential 타입: " + customCredential.getType());
            }
        } else {
            Log.e(TAG, "예상치 못한 credential 타입");
        }
    }


    /**
     * 카카오 로그인 수행
     * 카카오톡 앱이 설치되어 있으면 앱으로 시도, 실패(취소 제외)하면 카카오계정(웹) 로그인으로 폴백
     */
    private void performKakaoLogin() {
        // KakaoTalk 앱이 설치된 경우 앱 전환 방식 사용 (깜빡임 없음)
        // 미설치 또는 실패 시 웹 기반 로그인으로 폴백
        Function2<OAuthToken, Throwable, Unit> accountCallback = (token, error) -> {
            if (error != null) {
                Log.e(TAG, "카카오계정으로 로그인 실패", error);
                Toast.makeText(this, "카카오 로그인에 실패했습니다", Toast.LENGTH_SHORT).show();
                return Unit.INSTANCE;
            }

            if (token != null) {
                Log.d(TAG, "카카오계정으로 로그인 성공, access_token 획득");
                sendSocialLoginToServer("kakao", token.getAccessToken(), null);
            }

            return Unit.INSTANCE;
        };

        if (UserApiClient.getInstance().isKakaoTalkLoginAvailable(this)) {
            UserApiClient.getInstance().loginWithKakaoTalk(this, (token, error) -> {
                if (error != null) {
                    Log.e(TAG, "카카오톡으로 로그인 실패", error);

                    // 사용자가 의도적으로 취소한 경우 폴백 없이 그대로 취소 처리
                    if (error instanceof ClientError
                            && ((ClientError) error).getReason() == ClientErrorCause.Cancelled) {
                        return Unit.INSTANCE;
                    }

                    // 카카오톡에 연결된 계정 없음 등 - 웹 로그인으로 폴백
                    UserApiClient.getInstance().loginWithKakaoAccount(this, accountCallback);
                    return Unit.INSTANCE;
                }

                if (token != null) {
                    Log.d(TAG, "카카오톡으로 로그인 성공, access_token 획득");
                    sendSocialLoginToServer("kakao", token.getAccessToken(), null);
                }

                return Unit.INSTANCE;
            });
        } else {
            UserApiClient.getInstance().loginWithKakaoAccount(this, accountCallback);
        }
    }

    /**
     * 소셜 로그인 토큰을 우리 서버로 전송 (카카오/구글 공통)
     *
     * @param provider    "kakao" 또는 "google"
     * @param accessToken 카카오 access_token (구글이면 null)
     * @param idToken     구글 id_token (카카오면 null)
     */
    private void sendSocialLoginToServer(String provider, String accessToken, String idToken) {
        SocialLoginRequest request = new SocialLoginRequest(provider, accessToken, idToken);

        apiService.socialLogin(request).enqueue(new Callback<ApiResponse<SocialLoginData>>() {
            @Override
            public void onResponse(Call<ApiResponse<SocialLoginData>> call, Response<ApiResponse<SocialLoginData>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<SocialLoginData> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        handleSocialLoginSuccess(apiResponse.getData());
                    } else {
                        Toast.makeText(LoginActivity.this,
                                apiResponse.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "dd소셜 로그인 실패: " + apiResponse.getMessage());
                    }
                } else {
                    Toast.makeText(LoginActivity.this,
                            "소셜 로그인에 실패했습니다. 다시 시도해주세요.",
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "HTTP 오류: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<SocialLoginData>> call, Throwable t) {
                Toast.makeText(LoginActivity.this,
                        "네트워크 오류가 발생했습니다. 인터넷 연결을 확인해주세요.",
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "네트워크 오류: " + t.getMessage(), t);
            }
        });
    }

    /**
     * 소셜 로그인 성공 처리
     * - JWT 토큰 저장
     * - needs_onboarding에 따라 온보딩 화면 또는 메인 화면으로 분기
     */
    private void handleSocialLoginSuccess(SocialLoginData socialLoginData) {
        SocialLoginData.SocialUserData user = socialLoginData.getUser();

        Log.d(TAG, "소셜 로그인 성공");
        Log.d(TAG, "user_id: " + user.getUser_id());
        Log.d(TAG, "needs_onboarding: " + socialLoginData.isNeeds_onboarding());

        // SharedPreferences에 로그인 정보 + JWT 토큰 저장
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, user.getUser_id());
        editor.putString(KEY_JWT_TOKEN, user.getToken());
        editor.putString(KEY_REFRESH_TOKEN, user.getRefresh_token());

        // user_location 파싱해서 저장 (있을 경우만 - 온보딩 전이면 null일 수 있음)
        String userLocation = user.getUser_location();
        if (userLocation != null && !userLocation.isEmpty()) {
            String[] parsed = parseLocation(userLocation);
            editor.putString(KEY_SIGUNGU, parsed[0]);
            editor.putString(KEY_DONG, parsed[1]);
        }

        editor.apply();

        Log.d(TAG, "SharedPreferences 저장 완료 (JWT 토큰 포함): user_id=" + user.getUser_id());

        // FCM 디바이스 토큰 발급 후 서버에 등록
        registerFcmToken(user.getUser_id());

        if (socialLoginData.isNeeds_onboarding()) {
            // 추가 정보(닉네임/위치 등)가 필요 - 온보딩 화면으로 이동
            Toast.makeText(this, "추가 정보를 입력해주세요", Toast.LENGTH_SHORT).show();
            goToOnboardingActivity(user);
        } else {
            // 모든 정보가 있는 기존 회원 - 바로 메인으로
            String nickname = user.getUser_nickname() != null ? user.getUser_nickname() : "회원";
            Toast.makeText(this, nickname + "님 환영합니다!", Toast.LENGTH_SHORT).show();
            goToMainActivity();
        }
    }

    /**
     * 온보딩(추가정보 입력) Activity로 이동
     */
    private void goToOnboardingActivity(SocialLoginData.SocialUserData user) {
        Intent intent = new Intent(LoginActivity.this, OnboardingActivity.class);
        // 비어있는 필드만 온보딩에서 입력받는다 (복원 계정은 성별/생일/지역이 남아있음)
        intent.putExtra("needs_nickname", user.getUser_nickname() == null || user.getUser_nickname().isEmpty());
        intent.putExtra("needs_phone", user.getUser_phone_number() == null || user.getUser_phone_number().isEmpty());
        intent.putExtra("needs_location", user.getUser_location() == null || user.getUser_location().isEmpty());
        intent.putExtra("needs_gender", user.getUser_gender() == null || user.getUser_gender().isEmpty());
        intent.putExtra("needs_birthday", user.getUser_birthday() == null || user.getUser_birthday().isEmpty());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    /**
     * 로그인 수행
     */
    private void performLogin() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        // 입력 검증
        if (!validateInput(email, password)) {
            return;
        }

        // APi 호출
        LoginRequest request = new LoginRequest(email, password);
        Call<ApiResponse<LoginData>> call = apiService.login(request);

        call.enqueue(new Callback<ApiResponse<LoginData>>() {
            @Override
            public void onResponse(Call<ApiResponse<LoginData>> call, Response<ApiResponse<LoginData>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<LoginData> apiResponse = response.body();

                    if (apiResponse.isSuccess()) {
                        // 로그인 성공
                        handleLoginSuccess(apiResponse.getData());
                    } else {
                        // 로그인 실패 (서버에서 success: false)
                        Toast.makeText(LoginActivity.this,
                                apiResponse.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "로그인 실패: " + apiResponse.getMessage());
                    }
                } else {
                    ApiResponse<LoginData> apiResponse = response.body();
                    // HTTP 오류
                    Toast.makeText(LoginActivity.this,
                            "로그인에 실패했습니다. 다시 시도해주세요.",
                            Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "HTTP 오류: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<LoginData>> call, Throwable t) {
                Toast.makeText(LoginActivity.this,
                        "네트워크 오류가 발생했습니다. 인터넷 연결을 확인해주세요.",
                        Toast.LENGTH_SHORT).show();
                Log.e(TAG, "네트워크 오류: " + t.getMessage(), t);
            }
        });
    }

    /**
     * 입력 검증
     */
    private boolean validateInput(String email, String password) {
        // 이메일 빈 값 체크
        if (email.isEmpty()) {
            binding.etEmail.setError("이메일을 입력해주세요");
            binding.etEmail.requestFocus();
            return false;
        }

        // 이메일 형식 체크
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("올바른 이메일 형식이 아닙니다");
            binding.etEmail.requestFocus();
            return false;
        }

        // 비밀번호 빈 값 체크
        if (password.isEmpty()) {
            binding.etPassword.setError("비밀번호를 입력해주세요");
            binding.etPassword.requestFocus();
            return false;
        }

        return true;
    }

    /**
     * 로그인 성공 처리
     * SharedPreferences에 로그인 정보 + JWT 토큰 저장
     * <p>
     * 수정 내역 (이슈 #4):
     * - 기존에는 user_id, is_logged_in, 위치 정보만 저장하고 JWT 토큰(KEY_JWT_TOKEN)을
     * 저장하지 않았다. handleSocialLoginSuccess()와 동일한 패턴으로 토큰 저장 라인을
     * 추가한다.
     */
    private void handleLoginSuccess(LoginData loginData) {
        Log.d(TAG, "로그인 성공");
        Log.d(TAG, "user_id: " + loginData.getUser_id());
        Log.d(TAG, "nickname: " + loginData.getUser_nickname());

        // SharedPreferences에 로그인 정보 + JWT 토큰 저장
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putInt(KEY_USER_ID, loginData.getUser_id());
        editor.putString(KEY_JWT_TOKEN, loginData.getToken());
        editor.putString(KEY_REFRESH_TOKEN, loginData.getRefresh_token());

        // user_location 파싱해서 저장
        String userLocation = loginData.getUser_location();
        String[] parsedLocation = parseLocation(userLocation);
        editor.putString(KEY_SIGUNGU, parsedLocation[0]);
        editor.putString(KEY_DONG, parsedLocation[1]);

        editor.apply();

        Log.d(TAG, "위치 저장: sigungu=" + parsedLocation[0] + ", dong=" + parsedLocation[1]);
        Log.d(TAG, "SharedPreferences 저장 완료 (JWT 토큰 포함): user_id=" + loginData.getUser_id());

        // FCM 디바이스 토큰 발급 후 서버에 등록
        registerFcmToken(loginData.getUser_id());

        // 환영 메시지 (서버에서 받은 데이터로 표시)
        Toast.makeText(this,
                loginData.getUser_nickname() + "님 환영합니다!",
                Toast.LENGTH_SHORT).show();

        // 메인 화면으로 이동
        goToMainActivity();
    }

    /**
     * FCM 디바이스 토큰을 발급받아 SharedPreferences에 저장하고 서버에 등록한다.
     * <p>
     * 로그인 성공 직후 1회 호출되며, 토큰 발급/등록이 실패해도 로그인 자체
     * 흐름(goToMainActivity)에는 영향을 주지 않는다 (이미 호출됨).
     */
    private void registerFcmToken(int userId) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.e(TAG, "FCM 토큰 발급 실패", task.getException());
                        return;
                    }

                    String token = task.getResult();
                    Log.d(TAG, "FCM 토큰 발급 완료: " + token);

                    sharedPreferences.edit().putString(KEY_FCM_TOKEN, token).apply();

                    RegisterFcmTokenRequest request = new RegisterFcmTokenRequest(token);
                    apiService.registerFcmToken(request).enqueue(new Callback<ApiResponse<Void>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                Log.d(TAG, "FCM 토큰 서버 등록 완료 (user_id=" + userId + ")");
                            } else {
                                Log.e(TAG, "FCM 토큰 서버 등록 실패: HTTP " + response.code());
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                            Log.e(TAG, "FCM 토큰 서버 등록 네트워크 오류: " + t.getMessage(), t);
                        }
                    });
                });
    }

    /**
     * user_location 문자열을 [sigungu, dong] 배열로 파싱
     * 예시: "관악구" → ["관악구", "관악구"]
     * 예시: "서울특별시 관악구 대학동" → ["관악구", "대학동"]
     */
    private String[] parseLocation(String location) {
        if (location == null || location.isEmpty()) {
            return new String[]{"전체", "전체"};
        }

        String[] parts = location.trim().split(" ");

        if (parts.length >= 2) {
            // "관악구 서림동" → ["관악구", "서림동"]
            return new String[]{parts[0], parts[1]};
        } else {
            // 혹시 "관악구" 만 있는 경우
            return new String[]{parts[0], parts[0]};
        }
    }

    /**
     * 메인 Activity로 이동
     */
    private void goToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * 회원가입 Activity로 이동
     */
    private void goToRegisterActivity() {
        Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
        startActivity(intent);
    }

    /**
     * 뒤로가기 버튼 처리
     */
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity(); // 앱 완전 종료
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}