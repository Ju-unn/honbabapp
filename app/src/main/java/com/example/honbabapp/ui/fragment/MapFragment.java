package com.example.honbabapp.ui.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.honbabapp.MainActivity;
import com.example.honbabapp.R;
import com.example.honbabapp.databinding.FragmentMapBinding;
import com.example.honbabapp.databinding.PageMapBottomSheetFilterBinding;
import com.example.honbabapp.databinding.PageMapBottomSheetSortBinding;
import com.example.honbabapp.databinding.PageMapFilterFoodBinding;
import com.example.honbabapp.ui.adapter.MapFilterPagerAdapter;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.HashSet;

public class MapFragment extends Fragment {

    private FragmentMapBinding fragmentMapbinding;
    private PageMapBottomSheetSortBinding bottomSheetSortBinding;
    private PageMapBottomSheetFilterBinding bottomSheetFilterBinding;
    private MapFilterPagerAdapter adapter;
    private MapFilterFoodFragment foodFragment;
    private MapFilterPriceFragment priceFragment;

    // 선택한 foodtypes를 담는곳
    private HashSet<String> selectedFoodTypes = new HashSet<>();
    private String currentSortOption = "전체";

    // sliderRange price
    private int minPrice = 0;
    private int maxPrice = 50000;

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

        // 정렬 버튼 초기화
        initSortButton();

        // 필터 버튼 초기화
        initFilterButton();
    }

    public void onBackButtonClick() {
        fragmentMapbinding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity mainActivity = (MainActivity) requireActivity();
                mainActivity.navigationToHome();
            }
        });
    }

    /// 인기순 , 전체 , 거리순 필터 버튼
    public void initSortButton() {
        fragmentMapbinding.btnSort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSortButtonSheet();
            }
        });
    }

    /// 버튼 클릭 이벤트
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
            bottomSheetSortBinding.checkPopular.setVisibility(View.VISIBLE);
        } else if (currentSortOption.equals("거리순")) {
            bottomSheetSortBinding.checkDistance.setVisibility(View.VISIBLE);
        }

    }

    /// fragment_map.xml textview수정
    public void updateSortButtonText() {
        if (fragmentMapbinding != null & fragmentMapbinding.tvSortOption != null) {
            fragmentMapbinding.tvSortOption.setText(currentSortOption);
        }
    }


    /// 필터 버튼 클릭 이벤트
    public void initFilterButton() {
        fragmentMapbinding.btnFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFilterBottomSheet();
            }
        });
    }

    public void showFilterBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        bottomSheetFilterBinding = PageMapBottomSheetFilterBinding.inflate(LayoutInflater.from(getContext()));
        bottomSheetDialog.setContentView(bottomSheetFilterBinding.getRoot());

        setupViewPager();

        /// 쓰레기통 아이콘 클릭시
        bottomSheetFilterBinding.icDeleteAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetAllFilters();
            }
        });

        /// close 아이콘 클릭시
        bottomSheetFilterBinding.btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
            }
        });

        /// 초기화 버튼 클릭시
        bottomSheetFilterBinding.btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetAllFilters();
            }
        });

        /// 결과 보기 버튼 클릭시
        bottomSheetFilterBinding.btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
            }
        });

        bottomSheetDialog.show();
    }

    public void setupViewPager() {
        adapter = new MapFilterPagerAdapter(this);
        bottomSheetFilterBinding.viewPager.setAdapter(adapter);

        bottomSheetFilterBinding.viewPager.post(new Runnable() {
            @Override
            public void run() {
                new TabLayoutMediator(
                        bottomSheetFilterBinding.tabLayout ,
                        bottomSheetFilterBinding.viewPager ,
                        (tab, position) -> tab.setText(position == 0 ? "음식 종류" : "가격")
                ).attach();
            }
        });

        bottomSheetFilterBinding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == 0 && foodFragment == null) {
                    foodFragment = (MapFilterFoodFragment) getChildFragmentManager().findFragmentByTag("f0" );
                    if (foodFragment != null) setupFoodFragmentListener();
                }
                else if (position == 1 && priceFragment == null) {
                    priceFragment = (MapFilterPriceFragment) getChildFragmentManager().findFragmentByTag("f1");
                    if (priceFragment != null) setupPriceFragmentListener();
                }
            }
        });
    }

    public void setupFoodFragmentListener() {
        foodFragment.setOnFoodSelectedListener(new MapFilterFoodFragment.OnFoodSelectedListener() {
            @Override
            public void onFoodSelected(String foodType, boolean isSelected) {
                if (isSelected) selectedFoodTypes.add(foodType);
                else selectedFoodTypes.remove(foodType);
                updateSelectedChips();
            }
        });
    }

    public void setupPriceFragmentListener() {
        priceFragment.setOnPriceSelectedListener(new MapFilterPriceFragment.OnPriceSelectedListener() {
            @Override
            public void onPriceSelected(int min, int max) {
                updateSelectedChips();
            }
        });
    }

    /// 선택된 필터 chip
    public void updateSelectedChips() {
        //chip 초기화
        bottomSheetFilterBinding.chipGroupSelected.removeAllViews();
        boolean has = !selectedFoodTypes.isEmpty() || !(minPrice == 0 && maxPrice == 50000);
        bottomSheetFilterBinding.selectedFiltersScroll.setVisibility(has ? View.VISIBLE : View.GONE);

        if (!has) return;
        for (String food : selectedFoodTypes) {
            bottomSheetFilterBinding.chipGroupSelected.addView(createFilterChip(food , "food"));
            if (!(minPrice == 0 && maxPrice == 50000))
                bottomSheetFilterBinding.chipGroupSelected.addView(createFilterChip(formatPrice(minPrice) + "~" + formatPrice(maxPrice), "price"));
        }
    }


    /// 필터 chip 생성
    public Chip createFilterChip(String text , String type) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setCloseIconVisible(true);
        chip.setChipBackgroundColorResource(R.color.primary_100);
        chip.setTextColor(getResources().getColor(android.R.color.white , null));
        chip.setCloseIconTintResource(android.R.color.white);

        chip.setOnCloseIconClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (type.equals("food")) {
                    selectedFoodTypes.remove(text);
                    if (foodFragment != null) foodFragment.setButtonSelected(text , false);
                } else {
                    minPrice = 0;
                    maxPrice = 50000;
                    if (priceFragment != null) priceFragment.resetPrice();
                }
                updateSelectedChips();
            }
        });
        return chip;
    }

    /// 모든 음식 버튼 초기화
    public void resetAllFilters() {
        selectedFoodTypes.clear();
        minPrice = 0;
        maxPrice = 50000;
        if (foodFragment != null) foodFragment.resetAllButtons();
        if (priceFragment != null) priceFragment.resetPrice();
        updateSelectedChips();
    }

    public String formatPrice(int price) {
        if (price >= 10000) return (price / 10000) + "만원";
        if (price >= 1000) return (price / 1000) + "천원";
        return price + "원";

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        fragmentMapbinding = null;
        bottomSheetSortBinding = null;
        bottomSheetFilterBinding = null;
    }

}