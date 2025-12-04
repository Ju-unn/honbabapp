package com.example.honbabapp.activity;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.honbabapp.databinding.ActivitySearchViewBinding;

public class SearchViewActivity extends AppCompatActivity {

    private ActivitySearchViewBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 뒤로가기 버튼
        onBackButtonClick();
    }

    public void onBackButtonClick() {
        binding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}