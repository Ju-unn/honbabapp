package com.example.honbabapp;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.honbabapp.databinding.PageRegisterBinding;

public class RegisterActivity extends AppCompatActivity {
    private PageRegisterBinding registerBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registerBinding = PageRegisterBinding.inflate(getLayoutInflater());
        setContentView(registerBinding.getRoot());

        /// 뒤로가기 버튼
        onBackButtonClick();

    }

    public void onBackButtonClick(){
        registerBinding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }
}