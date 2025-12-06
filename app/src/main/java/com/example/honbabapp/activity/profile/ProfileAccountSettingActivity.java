package com.example.honbabapp.activity.profile;

import android.content.Intent;
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
        // 비밀번호 변경 버튼
        onChangePaaswordButtonOnClick();
        // 계정 탈퇴 버튼
        onDeleteAccountButtonOnClick();
    }

    /// 뒤로가기 버튼
    public void onBackButtonClick(){
        binding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getOnBackPressedDispatcher().onBackPressed();
            }
        });
    }

    /// 비밀번호 변경 버튼
    public void onChangePaaswordButtonOnClick() {
        binding.btnChangePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileAccountSettingActivity.this , ProfileChangePasswordActivity.class);
                startActivity(intent);
            }
        });
    }

    /// 계정 탈퇴 버튼
    public void onDeleteAccountButtonOnClick() {
        binding.btnDeleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileAccountSettingActivity.this , ProfileDeleteAccountActivity.class);
                startActivity(intent);
            }
        });
    }
}