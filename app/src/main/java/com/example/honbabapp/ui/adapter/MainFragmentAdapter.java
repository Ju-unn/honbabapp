package com.example.honbabapp.ui.adapter;

import android.view.Menu;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.honbabapp.ui.fragment.ChatFragment;
import com.example.honbabapp.ui.fragment.HomeFragment;
import com.example.honbabapp.ui.fragment.MapFragment;
import com.example.honbabapp.ui.fragment.ProfileFragment;

import java.util.ArrayList;

public class MainFragmentAdapter extends FragmentStateAdapter {

    public ArrayList<Fragment> fragmentList = new ArrayList<>();
    public MainFragmentAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);

        fragmentList.add(new HomeFragment());
        fragmentList.add(new MapFragment());
        fragmentList.add(new ChatFragment());
        fragmentList.add(new ProfileFragment());

    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragmentList.get(position);
    }

    @Override
    public int getItemCount() {
        return fragmentList.size();
    }
}
