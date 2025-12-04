package com.example.honbabapp.activity.profile;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.honbabapp.databinding.PageProfileProfilesettingBinding;

public class ProfileProfileSettingActivity extends AppCompatActivity {

    private PageProfileProfilesettingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageProfileProfilesettingBinding.inflate(getLayoutInflater());
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