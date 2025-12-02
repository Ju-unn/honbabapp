package com.example.honbabapp.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.honbabapp.ui.fragment.MapFilterFoodFragment;
import com.example.honbabapp.ui.fragment.MapFilterPriceFragment;

import java.util.ArrayList;

public class MapFilterPagerAdapter extends FragmentStateAdapter {

    ArrayList<Fragment> filterList = new ArrayList<>();
    public MapFilterPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
        this.filterList = filterList;
        filterList.add(new MapFilterFoodFragment());
        filterList.add(new MapFilterPriceFragment());
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return filterList.get(position);
    }

    @Override
    public int getItemCount() {
        return filterList.size();
    }
}
