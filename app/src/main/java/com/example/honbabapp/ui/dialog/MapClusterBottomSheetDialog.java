package com.example.honbabapp.ui.dialog;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.honbabapp.api.data.meeting.Meeting;
import com.example.honbabapp.databinding.BottomsheetClusterMeetingsBinding;
import com.example.honbabapp.ui.adapter.MapAllMeetingListAdapter;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.R;

import java.util.List;

public class MapClusterBottomSheetDialog extends BottomSheetDialogFragment {

    // MapFragment에서 직접 세팅하는 모임 목록
    public List<Meeting> meetings;

    private BottomsheetClusterMeetingsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomsheetClusterMeetingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupTitle();
        setupRecyclerView();
    }

    /** 타이틀에 모임 개수 표시: "이 근처 모임 (3개)" */
    private void setupTitle() {
        int count = (meetings != null) ? meetings.size() : 0;
        binding.tvClusterTitle.setText("이 근처 모임 (" + count + "개)");
    }

    /** 모임 목록 세팅 */
    private void setupRecyclerView() {
        if (meetings == null || meetings.isEmpty()) {
            // 모임 없음 → 빈 상태 텍스트 표시
            binding.tvEmpty.setVisibility(View.VISIBLE);
            binding.rvClusterMeetings.setVisibility(View.GONE);
            return;
        }

        binding.tvEmpty.setVisibility(View.GONE);
        binding.rvClusterMeetings.setVisibility(View.VISIBLE);
        binding.rvClusterMeetings.setLayoutManager(new LinearLayoutManager(requireContext()));

        MapAllMeetingListAdapter adapter = new MapAllMeetingListAdapter();
        adapter.updateMeetings(meetings);
        binding.rvClusterMeetings.setAdapter(adapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        // BottomSheet 최대 높이를 화면의 65%로 제한
        BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
        if (dialog == null) return;

        FrameLayout bottomSheet = dialog.findViewById(
                com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet == null) return;

        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
        DisplayMetrics metrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        behavior.setPeekHeight((int) (metrics.heightPixels * 0.65));
        behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // 메모리 누수 방지
    }
}