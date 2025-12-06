package com.example.honbabapp.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.honbabapp.databinding.PageMapBottomSheetSortBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class MapSortBottomSheetDialog extends BottomSheetDialogFragment {

    PageMapBottomSheetSortBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = PageMapBottomSheetSortBinding.inflate(inflater , container , false);
        return binding.getRoot();
    }

}
