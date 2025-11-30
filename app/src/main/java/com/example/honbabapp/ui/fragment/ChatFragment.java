package com.example.honbabapp.ui.fragment;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.honbabapp.R;
import com.example.honbabapp.databinding.FragmentChatBinding;

public class ChatFragment extends Fragment {
    private FragmentChatBinding binding;

    public ChatFragment() {
        Log.i("ChatFragment" , "생성");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i("ChatFragment" , "onCreateView");

        binding = FragmentChatBinding.inflate(inflater , container , false);
        return binding.getRoot();
    }
}