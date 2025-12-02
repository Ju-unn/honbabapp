package com.example.honbabapp.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.honbabapp.databinding.PageMapFilterFoodBinding;

public class MapFilterFoodFragment extends Fragment {

    private PageMapFilterFoodBinding binding;
    private OnFoodSelectedListener listener;

    /// 부모 프래그먼트 즉 MapFragment에게 전달할 interface
    public interface OnFoodSelectedListener {
        void onFoodSelected(String foodType , boolean isSelected);
    }

    public void setOnFoodSelectedListener (OnFoodSelectedListener listener) {
        this.listener = listener;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = PageMapFilterFoodBinding.inflate(inflater , container , false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupFoodButtons();
    }

    public void setupFoodButtons()  {
        setupFoodButton(binding.btnKorean, "한식");
        setupFoodButton(binding.btnChinese, "중식");
        setupFoodButton(binding.btnJapanese, "일식");
        setupFoodButton(binding.btnOmakase, "오마카세");
        setupFoodButton(binding.btnWestern, "양식");
        setupFoodButton(binding.btnBar, "술집");
        setupFoodButton(binding.btnCafe, "카페");
    }

    public void setupFoodButton(Button button , String foodType) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isSelected = !button.isSelected();
                button.setSelected(isSelected);

                if (listener != null) {
                    listener.onFoodSelected(foodType , isSelected);
                }
            }
        });
    }

    public void setButtonSelected(String foodType , boolean selected) {
        Button button = getButtonByFoodType(foodType);
        if (button != null) {
            button.setSelected(selected);
        }
    }

    public Button getButtonByFoodType(String foodType){
        switch (foodType) {
            case "한식": return binding.btnKorean;
            case "중식": return binding.btnChinese;
            case "일식": return binding.btnJapanese;
            case "오마카세": return binding.btnOmakase;
            case "양식": return binding.btnWestern;
            case "술집": return binding.btnBar;
            case "카페": return binding.btnCafe;
            default: return null;
        }
    }

    public void resetAllButtons() {
        binding.btnKorean.setSelected(false);
        binding.btnChinese.setSelected(false);
        binding.btnJapanese.setSelected(false);
        binding.btnOmakase.setSelected(false);
        binding.btnWestern.setSelected(false);
        binding.btnBar.setSelected(false);
        binding.btnCafe.setSelected(false);
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
