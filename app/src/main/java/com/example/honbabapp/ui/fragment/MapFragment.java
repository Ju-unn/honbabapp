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
import com.example.honbabapp.ui.dialog.MapFilterBottomSheetDialog;
import com.example.honbabapp.ui.dialog.MapSortBottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.Map;

public class MapFragment extends Fragment {

    private FragmentMapBinding fragmentMapbinding;
    private PageMapBottomSheetSortBinding bottomSheetSortBinding;
    private MapHotMeetingListAdapter hotMeetingListAdapter;
    private MapAllMeetingListAdapter allMeetingListAdapter;
    private String currentSortOption = "전체";
    private String pendingCategory = null;

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
        Log.i("Test_Log","onBackButtonClick");
        fragmentMapbinding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("Test_Log","onClick in");
                MainActivity mainActivity = (MainActivity) requireActivity();
                mainActivity.navigationToHome();
            }
        });
        Log.i("Test_Log","onBackButtonClick Close");
    }

    public void initSortButton() {
        fragmentMapbinding.btnSort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSortButtonSheet();
            }
        });

        /// 필터 클릭시
        fragmentMapbinding.btnFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MapFilterBottomSheetDialog mapFilterBottomSheetDialog = new MapFilterBottomSheetDialog();
                mapFilterBottomSheetDialog.show(getActivity().getSupportFragmentManager(), "mapFilterBottomsheetDiaglog");
            }
        });
    }

    /// bottomsheet 필터 버튼 클릭 이벤트
    public void showSortButtonSheet() {
        MapSortBottomSheetDialog mapSortBottomSheetDialog = new MapSortBottomSheetDialog();

        mapSortBottomSheetDialog.listener = new MapSortBottomSheetDialog.OnSortSelectedListener() {
            @Override
            public void OnSortSelected(String sortOption) {
                currentSortOption = sortOption;
                updateSortButtonText();
            }
        };
        mapSortBottomSheetDialog.currentSortOption = currentSortOption;
        mapSortBottomSheetDialog.show(getActivity().getSupportFragmentManager(), "mapSortBottomSheetDialog");
    }

    /// fragment_map.xml textview수정
    public void updateSortButtonText() {
        if (fragmentMapbinding != null && fragmentMapbinding.tvSortOption != null) {
            fragmentMapbinding.tvSortOption.setText(currentSortOption);
        }
    }

    public void setFilter(String category) {
        MapAllMeetingListAdapter.OrderBy orderBy;

        if (currentSortOption.equals("인기순")) {
            orderBy = MapAllMeetingListAdapter.OrderBy.Hot;
        } else if (currentSortOption.equals("거리순")) {
            orderBy = MapAllMeetingListAdapter.OrderBy.Distance;
        } else {
            orderBy = MapAllMeetingListAdapter.OrderBy.All;
        }

        allMeetingListAdapter.setOrderByAndFilter(orderBy , category);
        allMeetingListAdapter.notifyDataSetChanged();
    }

}