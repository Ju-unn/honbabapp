package com.example.honbabapp.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.login.LoginData;
import com.example.honbabapp.api.data.request.LoginRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.databinding.ActivityLoginBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    /**
     * 로그인 Activity (최적화 버전)
     *
     * SharedPreferences 저장:
     * - user_id (int)         : API 호출 시 사용
     * - is_logged_in (boolean): 자동 로그인 체크
     *
     * 나머지 정보는 서버에서 가져오기:
     * - nickname, email, profile_image, location, manner_temp 등
     */
    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding binding;
    private ApiService apiService;
    private SharedPreferences sharedPreferences;

    // SharedPreferences 설정
    private static final String PREF_NAME = "honbabapp_prefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_ID = "user_id";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        // 초기화
        initializeComponents();

//        // 자동 로그인 확인
//        checkAutoLogin();

        // 이벤트 리스너 설정
        setupListeners();

    }

    /**
     * 컴포넌트 초기화
     */
    private void initializeComponents() {
        apiService = RetrofitClient.getClient().create(ApiService.class);
        sharedPreferences = getSharedPreferences(PREF_NAME , MODE_PRIVATE);

        Log.d(TAG, "LoginActivity 초기화 완료");
    }

//    /**
//     * 자동 로그인 확인
//     * SharedPreferences에 로그인 정보가 있다면 자동으로 메인 화면으로 이동
//     */
//    private void checkAutoLogin() {
//        boolean isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN , false);
//        int userId = sharedPreferences.getInt(KEY_USER_ID , -1);
//
//        if (isLoggedIn && userId != -1) {
//            Log.d(TAG, "자동 로그인: user_id = " + userId);
//
//            // 바로 메인 화면으로 이동
//            goToMainActivity();
//        } else {
//            Log.d(TAG, "자동 로그인 정보 없음 - 로그인 화면 표시");
//        }
//    }

    /**
     * 이벤트 리스너 설정
     */
    private void setupListeners() {
        // 로그인 버튼
        binding.btnLogin.setOnClickListener(v -> performLogin());

        // 회원가입 링크
        binding.tvSingupLink.setOnClickListener(v -> goToRegisterActivity());

        // TODO: 소셜 로그인 버튼들 (추후 구현)
        binding.btnKakaoLogin.setOnClickListener(v ->
                Toast.makeText(this, "카카오 로그인 준비 중입니다", Toast.LENGTH_SHORT).show()
        );

        binding.btnNaverLogin.setOnClickListener(v ->
                Toast.makeText(this, "네이버 로그인 준비 중입니다", Toast.LENGTH_SHORT).show()
        );

        binding.btnGoogleLogin.setOnClickListener(v ->
                Toast.makeText(this, "구글 로그인 준비 중입니다", Toast.LENGTH_SHORT).show()
        );
    }

    /**
     * 로그인 수행
     */
    private void performLogin(){
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        // 입력 검증
        if (!validateInput(email, password)) {
            return;
        }

        // APi 호출
        LoginRequest request = new LoginRequest(email , password);
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
    private boolean validateInput(String email , String password) {
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
     *  SharedPreferences에 최소 정보만 저장 (user_id, is_logged_in)
     */

    private void handleLoginSuccess(LoginData loginData) {
        Log.d(TAG, "로그인 성공");
        Log.d(TAG, "user_id: " + loginData.getUser_id());
        Log.d(TAG, "nickname: " + loginData.getUser_nickname());

        //SharedPreferences에 최소 정보만 저장
        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putBoolean(KEY_IS_LOGGED_IN , true);
        editor.putInt(KEY_USER_ID , loginData.getUser_id());
        editor.apply();
//
        Log.d(TAG, "SharedPreferences 저장 완료: user_id=" + loginData.getUser_id());

        // 환영 메시지 (서버에서 받은 데이터로 표시)
        Toast.makeText(this,
                loginData.getUser_nickname() + "님 환영합니다!",
                Toast.LENGTH_SHORT).show();

        // 메인 화면으로 이동
        goToMainActivity();
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