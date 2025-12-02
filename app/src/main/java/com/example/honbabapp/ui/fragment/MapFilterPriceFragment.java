package com.example.honbabapp.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.honbabapp.databinding.PageMapFilterPriceBinding;
import com.google.android.material.slider.RangeSlider;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class MapFilterPriceFragment extends Fragment {

    private PageMapFilterPriceBinding binding;
    private OnPriceSelectedListener listener;

    public interface OnPriceSelectedListener {
        void onPriceSelected(int minPrice , int maxPrice);
    }

    public void setOnPriceSelectedListener(OnPriceSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = PageMapFilterPriceBinding.inflate(inflater , container , false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupRangeSlider();
        setupQuickButtons();
    }

    public void setupRangeSlider() {
        binding.priceRangeSlider.addOnChangeListener(new RangeSlider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull RangeSlider slider, float value, boolean fromUser) {
                List<Float> values = slider.getValues();
                int minPrice = values.get(0).intValue();
                int maxPrice = values.get(1).intValue();

                /// 텍스트 가격 변경
                updatePriceText(minPrice , maxPrice);

                /// 부모에게 전달
                if (listener != null && fromUser) {
                    listener.onPriceSelected(minPrice , maxPrice);
                }
            }
        });

        /// 초기 텍스트 설정
        List<Float> values = binding.priceRangeSlider.getValues();
        updatePriceText(values.get(0).intValue(), values.get(1).intValue());
    }

    public void setupQuickButtons() {
        binding.btnPrice10k20k.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPriceRange(0, 10000);
            }
        });
        binding.btnPrice10k20k.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPriceRange(10000, 20000);
            }
        });
        binding.btnPrice10k20k.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPriceRange(20000, 30000);
            }
        });
    }

    public void setPriceRange(int min , int max){
        binding.priceRangeSlider.setValues((float) min ,(float) max);
        updatePriceText(min , max);

        if (listener != null) {
            listener.onPriceSelected(min , max);
        }
    }

    public void updatePriceText(int minPrice , int maxPrice) {
        NumberFormat formatter = NumberFormat.getInstance(Locale.KOREA);
        binding.tvMinPrice.setText(formatter.format(minPrice + "원"));
        binding.tvMaxPrice.setText(formatter.format(maxPrice + "원"));
    }

    public void setPriceRangeFromOutside(int min , int max) {
        binding.priceRangeSlider.setValues((float) min , (float) max);
        updatePriceText(min , max);
    }

    public void resetPrice() {
        setPriceRange(0, 50000);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
