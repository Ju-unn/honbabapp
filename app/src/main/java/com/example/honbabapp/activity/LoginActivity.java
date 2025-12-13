package com.example.honbabapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.LoginRequest;
import com.example.honbabapp.api.data.LoginResponse;
import com.example.honbabapp.databinding.ActivityLoginBinding;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getClient().create(ApiService.class);

        // 로그인 버튼 클릭시
        onLoginButtonClick();

        // 회원가입 버튼 클릭시
        onRegisterButtonClick();

        loginTest();
    }
    public void loginTest() {
        Call<LoginResponse> call = apiService.loginUser(new LoginRequest("test@example.com" , "1234"));

        //Retrofit을 이용해서 Call객체를 전달받고
        //Call객체가 서버로부터 응답을 바으면 enqueue함수 안에있는 Callback의 onResponse,onFailure가 호출된다 (interface호출되듯이)
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {

                Log.i("Test_log" , "login_user_issuccessful" + response.isSuccessful());
                if (response.isSuccessful()) {
                    //서버의 응답이 온 형태대로 전달받았음 LoginResponse객체 형태대로
                    LoginResponse loginResponse = response.body();
                    Log.i("Test_log" , "dd");


                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Log.i("Test_log" , "Onfailure");
            }
        });

    }

    public void onLoginButtonClick(){
        binding.btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this , MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    public void onRegisterButtonClick() {
        binding.tvSingupLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this , RegisterActivity.class);
                startActivity(intent);
            }
        });
    }
}