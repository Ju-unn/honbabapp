package com.example.honbabapp.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.honbabapp.api.data.meeting.Meeting;
import com.example.honbabapp.databinding.BottomsheetClusterMeetingsBinding;
import com.example.honbabapp.ui.adapter.MapAllMeetingListAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class MapClusterBottomSheetDialog extends BottomSheetDialogFragment {

    // 외부에서 주입할 모임 리스트
    public List<Meeting> meetings;

    private BottomsheetClusterMeetingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = BottomsheetClusterMeetingsBinding.inflate(inflater, container, false);

        binding.rvClusterMeetings.setLayoutManager(new LinearLayoutManager(requireContext()));

        // 기존 어댑터 재사용
        MapAllMeetingListAdapter adapter = new MapAllMeetingListAdapter();
        adapter.updateMeetings(meetings);
        binding.rvClusterMeetings.setAdapter(adapter);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;  // 메모리 누수 방지
    }
}