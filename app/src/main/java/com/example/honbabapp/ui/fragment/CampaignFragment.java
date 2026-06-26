package com.example.honbabapp.ui.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.honbabapp.activity.campaign.CampaignWritingActivity;
import com.example.honbabapp.databinding.FragmentCampaignBinding;
import com.example.honbabapp.ui.adapter.CampaignListAdapter;
import com.example.honbabapp.ui.dialog.CampaignFilterBottomSheetDialog;
import com.example.honbabapp.ui.dialog.CampaignSortBottomSheetDialog;

import java.util.HashSet;
import java.util.Set;

public class CampaignFragment extends Fragment {

    private static final String TAG = "CampaignFragment";

    private FragmentCampaignBinding binding;
    private CampaignListAdapter campaignListAdapter;

    // 현재 적용된 정렬/필터 상태
    private String currentSortOption = "최신순";
    private Set<String> currentChannels = new HashSet<>();
    private Set<String> currentCategories = new HashSet<>();
    private int currentMinPrice = 0;
    private int currentMaxPrice = 100000;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentCampaignBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initRecyclerView();
        initSortButton();
        initFilterButton();
        initFabButton();
    }

    // ─────────────────────────────────────────────────────────────
    // RecyclerView
    // ─────────────────────────────────────────────────────────────

    private void initRecyclerView() {
        campaignListAdapter = new CampaignListAdapter();
        binding.rvCampaignList.setAdapter(campaignListAdapter);

        // TODO: 로직 단계 - API 호출 (meeting_type="체험단" + 정렬/필터 파라미터)
        showEmptyState(true);
    }

    // ─────────────────────────────────────────────────────────────
    // 정렬 버튼 → CampaignSortBottomSheetDialog
    // MapFragment.showSortButtonSheet() 동일 패턴
    // ─────────────────────────────────────────────────────────────

    private void initSortButton() {
        binding.btnSort.setOnClickListener(v -> showSortDialog());
    }

    private void showSortDialog() {
        CampaignSortBottomSheetDialog dialog = new CampaignSortBottomSheetDialog();
        dialog.currentSortOption = currentSortOption;
        dialog.listener = sortOption -> {
            currentSortOption = sortOption;
            updateSortButtonText();
            // TODO: 로직 단계 - loadCampaignList() 재호출
        };
        dialog.show(getChildFragmentManager(), "campaignSortDialog");
    }

    private void updateSortButtonText() {
        binding.tvSortOption.setText(currentSortOption);
    }

    // ─────────────────────────────────────────────────────────────
    // 필터 버튼 → CampaignFilterBottomSheetDialog
    // MapFragment.showFilterDialog() 동일 패턴
    // ─────────────────────────────────────────────────────────────

    private void initFilterButton() {
        binding.btnFilter.setOnClickListener(v -> showFilterDialog());
    }

    private void showFilterDialog() {
        CampaignFilterBottomSheetDialog dialog = new CampaignFilterBottomSheetDialog();

        // 이전 필터 상태 복원
        dialog.initialChannels = new HashSet<>(currentChannels);
        dialog.initialCategories = new HashSet<>(currentCategories);
        dialog.initialMinPrice = currentMinPrice;
        dialog.initialMaxPrice = currentMaxPrice;

        dialog.listener = (channels, categories, minPrice, maxPrice) -> {
            currentChannels = new HashSet<>(channels);
            currentCategories = new HashSet<>(categories);
            currentMinPrice = minPrice;
            currentMaxPrice = maxPrice;
            updateFilterButtonState();
            // TODO: 로직 단계 - loadCampaignList() 재호출
        };

        dialog.show(getChildFragmentManager(), "campaignFilterDialog");
    }

    /**
     * 필터가 하나라도 적용된 경우 버튼 활성 색상으로 변경
     * MapFragment의 category_button_active_background 패턴
     */
    private void updateFilterButtonState() {
        boolean isFilterActive = !currentChannels.isEmpty()
                || !currentCategories.isEmpty()
                || currentMinPrice != 0
                || currentMaxPrice != 100000;

        binding.btnFilter.setBackgroundResource(
                isFilterActive
                        ? com.example.honbabapp.R.drawable.category_button_active_background
                        : com.example.honbabapp.R.drawable.category_button_background
        );

        binding.tvFilterLabel.setTextColor(
                isFilterActive
                        ? requireContext().getColor(com.example.honbabapp.R.color.primary_100)
                        : requireContext().getColor(com.example.honbabapp.R.color.text_200)
        );
    }

    // ─────────────────────────────────────────────────────────────
    // FAB
    // ─────────────────────────────────────────────────────────────

    private void initFabButton() {
        binding.fabCreateCampaign.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), CampaignWritingActivity.class);
            startActivity(intent);
        });
    }

    // ─────────────────────────────────────────────────────────────
    // 빈 상태
    // ─────────────────────────────────────────────────────────────

    private void showEmptyState(boolean isEmpty) {
        if (isEmpty) {
            binding.rvCampaignList.setVisibility(View.GONE);
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
            binding.tvFabHint.setVisibility(View.VISIBLE);
        } else {
            binding.rvCampaignList.setVisibility(View.VISIBLE);
            binding.layoutEmptyState.setVisibility(View.GONE);
            binding.tvFabHint.setVisibility(View.GONE);
        }
    }

    public void refreshCampaignList() {
        // TODO: 로직 단계 - API 재호출
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}