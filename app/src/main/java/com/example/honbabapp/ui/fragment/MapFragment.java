package com.example.honbabapp.ui.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.example.honbabapp.activity.MainActivity;
import com.example.honbabapp.activity.SearchViewActivity;
import com.example.honbabapp.databinding.FragmentMapBinding;
import com.example.honbabapp.databinding.PageMapBottomSheetSortBinding;
import com.example.honbabapp.ui.adapter.HomeAllMeetingListAdapter;
import com.example.honbabapp.ui.adapter.HomeHotMeetingListAdapter;
import com.example.honbabapp.ui.adapter.MapAllMeetingListAdapter;
import com.example.honbabapp.ui.adapter.MapHotMeetingListAdapter;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class MapFragment extends Fragment {

    private FragmentMapBinding fragmentMapbinding;
    private PageMapBottomSheetSortBinding bottomSheetSortBinding;
    private MapHotMeetingListAdapter hotMeetingListAdapter;
    private MapAllMeetingListAdapter allMeetingListAdapter;
    private String currentSortOption = "전체";

    // BottomSheet 관련 변수 추가
    private BottomSheetBehavior<View> bottomSheetBehavior;

    public MapFragment() {
        Log.i("MapFragment" , "생성");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i("MapFragment" , "onCreateView");

        fragmentMapbinding = FragmentMapBinding.inflate(inflater , container , false);
        return fragmentMapbinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i("MapFragment", "onViewCreated");

        // 뒤로가기 버튼
        onBackButtonClick();

        // 정렬 버튼 클릭 이벤트
        initSortButton();

        // 검색버튼 클릭 이벤트
        setupSearchBar();

        initRecyclerView();
    }

    /// recyclerview
    public void initRecyclerView() {
        allMeetingListAdapter = new MapAllMeetingListAdapter();
        fragmentMapbinding.rvMeetingCard.setAdapter(allMeetingListAdapter);
    }


    /// 검색창 클릭시 searchViewActivity로 이동
    public void setupSearchBar() {
        EditText etSearchMap = fragmentMapbinding.etSearchMap;

        // editText 클릭시
        etSearchMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SearchViewActivity.class);
                startActivity(intent);
            }
        });
    }

    /// 뒤로가기 버튼
    public void onBackButtonClick() {
        fragmentMapbinding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity mainActivity = (MainActivity) requireActivity();
                mainActivity.navigationToHome();
            }
        });
    }

    public void initSortButton() {
        fragmentMapbinding.btnSort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSortButtonSheet();
            }
        });
    }

    /// bottomsheet 필터 버튼 클릭 이벤트
    public void showSortButtonSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        bottomSheetSortBinding = PageMapBottomSheetSortBinding.inflate(LayoutInflater.from(getContext()));
        bottomSheetDialog.setContentView(bottomSheetSortBinding.getRoot());

        updateCheckMarksWithBinding(bottomSheetSortBinding);

        /// 닫기 버튼 클릭
        bottomSheetSortBinding.btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
            }
        });

        /// 전체 버튼 클릭
        bottomSheetSortBinding.sortAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentSortOption = "전체";
                updateCheckMarksWithBinding(bottomSheetSortBinding);
                updateSortButtonText();
                bottomSheetDialog.dismiss();
            }
        });

        /// 인기순 버튼 클릭
        bottomSheetSortBinding.sortPopular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentSortOption = "인기순";
                updateCheckMarksWithBinding(bottomSheetSortBinding);
                updateSortButtonText();
                bottomSheetDialog.dismiss();
            }
        });

        /// 거리순 클릭
        bottomSheetSortBinding.sortDistance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentSortOption = "거리순";
                updateCheckMarksWithBinding(bottomSheetSortBinding);
                updateSortButtonText();
                bottomSheetDialog.dismiss();
            }
        });

        bottomSheetDialog.show();
    }

    /// 체크표시 여부
    public void updateCheckMarksWithBinding(PageMapBottomSheetSortBinding bottomSheetSortBinding) {
        /// 모든 체크 숨기기
        bottomSheetSortBinding.checkAll.setVisibility(View.GONE);
        bottomSheetSortBinding.checkPopular.setVisibility(View.GONE);
        bottomSheetSortBinding.checkDistance.setVisibility(View.GONE);

        /// 현재 선택된 정렬 옵션에만 체크 표시
        if (currentSortOption.equals("전체")) {
            bottomSheetSortBinding.checkAll.setVisibility(View.VISIBLE);
        } else if (currentSortOption.equals("인기순")) {
            bottomSheetSortBinding.sortPopular.setVisibility(View.VISIBLE);
            bottomSheetSortBinding.checkPopular.setVisibility(View.VISIBLE);
        } else if (currentSortOption.equals("거리순")) {
            bottomSheetSortBinding.sortDistance.setVisibility(View.VISIBLE);
            bottomSheetSortBinding.checkDistance.setVisibility(View.VISIBLE);
        }
    }

    /// fragment_map.xml textview수정
    public void updateSortButtonText() {
        if (fragmentMapbinding != null && fragmentMapbinding.tvSortOption != null) {
            fragmentMapbinding.tvSortOption.setText(currentSortOption);
        }
    }
}