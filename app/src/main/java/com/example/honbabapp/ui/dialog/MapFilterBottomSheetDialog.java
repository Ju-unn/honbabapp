package com.example.honbabapp.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.honbabapp.databinding.BottomsheetMapFilterBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

//BottomSheetDialogFragment 클래스를 상속받아서 사용하기 간편한 형태로 자식 클래스 생성 (MapFilterBottomSheetDialog)
public class MapFilterBottomSheetDialog extends BottomSheetDialogFragment {

    BottomsheetMapFilterBinding binding;

    //BottomSheetDialog형태로 show될때 그려질 View를 초기화하는 코드
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //bottomsheet_map_filter.xml을 binding으로 가져와서 사용
        binding = BottomsheetMapFilterBinding.inflate(inflater , container , false);
        return binding.getRoot();

    }
}
