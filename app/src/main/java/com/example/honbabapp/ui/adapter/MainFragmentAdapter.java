package com.example.honbabapp.ui.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.honbabapp.ui.fragment.CampaignFragment;
import com.example.honbabapp.ui.fragment.ChatFragment;
import com.example.honbabapp.ui.fragment.HomeFragment;
import com.example.honbabapp.ui.fragment.MapFragment;
import com.example.honbabapp.ui.fragment.ProfileFragment;

import java.util.ArrayList;

/**
 * 바텀 네비게이션 Fragment 어댑터
 *
 * 인덱스 배치 (계획서 기준):
 *   0 - HomeFragment
 *   1 - MapFragment
 *   2 - CampaignFragment
 *   3 - ChatFragment
 *   4 - ProfileFragment
 */
public class MainFragmentAdapter extends FragmentStateAdapter {

    public ArrayList<Fragment> fragmentList = new ArrayList<>();

    public MainFragmentAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);

        fragmentList.add(new HomeFragment());       // index 0 - 홈
        fragmentList.add(new MapFragment());         // index 1 - 지도
        fragmentList.add(new CampaignFragment());    // index 2 - 체험단
        fragmentList.add(new ChatFragment());        // index 3 - 채팅
        fragmentList.add(new ProfileFragment());     // index 4 - 프로필
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
