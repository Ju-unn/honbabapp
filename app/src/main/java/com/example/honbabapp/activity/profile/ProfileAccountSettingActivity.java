package com.example.honbabapp.activity.profile;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.honbabapp.databinding.PageProfileAccountsettingBinding;

public class ProfileAccountSettingActivity extends AppCompatActivity {

    private PageProfileAccountsettingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageProfileAccountsettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 뒤로가기 버튼
        onBackButtonClick();
    }

    public void onBackButtonClick(){
        binding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }
}