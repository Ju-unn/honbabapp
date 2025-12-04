package com.example.honbabapp.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.honbabapp.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 로그인 버튼 클릭시
        onLoginButtonClick();

        // 회원가입 버튼 클릭시
        onRegisterButtonClick();


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