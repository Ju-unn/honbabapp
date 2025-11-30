package com.example.honbabapp.ui.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.honbabapp.R;
import com.example.honbabapp.databinding.FragmentChatBinding;
import com.example.honbabapp.databinding.FragmentMapBinding;

public class MapFragment extends Fragment {

    private FragmentMapBinding binding;
    private String selectedCategory;

    public MapFragment() {
        Log.i("MapFragment" , "생성");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i("MapFragment" , "onCreateView");

        binding = FragmentMapBinding.inflate(inflater , container , false);
        return binding.getRoot();
    }

    public void setCategory(String category) {
        this.selectedCategory = category;
        if (binding != null) {
            filterByCategory(category);
        }
    }

    public void filterByCategory(String category) {
        binding.btnFoodType.setText(category);
    }
}