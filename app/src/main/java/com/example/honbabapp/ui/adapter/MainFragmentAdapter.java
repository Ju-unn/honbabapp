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

/**
 * 바텀 네비게이션 Fragment 어댑터
 *
 * 인덱스 배치 (계획서 기준):
 *   0 - HomeFragment
 *   1 - MapFragment
 *   2 - CampaignFragment
 *   3 - ChatFragment
 *   4 - ProfileFragment
 *
 * FragmentStateAdapter의 계약: createFragment(position)은 호출될 때마다
 * 항상 새로운 Fragment 인스턴스를 반환해야 한다. 화면 밖으로 밀려난 탭은
 * onDestroy까지 완전히 파괴되었다가, 복귀 시 createFragment가 다시 호출되어
 * 새 인스턴스를 요구하기 때문이다. 이전처럼 미리 만들어둔 인스턴스를
 * 재사용하면, 파괴된 옛 인스턴스의 필드(예: ActivityResultLauncher)가
 * 재등록되지 않은 채로 다시 쓰이게 되어 크래시가 발생한다.
 */
public class MainFragmentAdapter extends FragmentStateAdapter {

    private static final int TAB_COUNT = 5;

    public MainFragmentAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new HomeFragment();
            case 1:
                return new MapFragment();
            case 2:
                return new CampaignFragment();
            case 3:
                return new ChatFragment();
            case 4:
                return new ProfileFragment();
            default:
                throw new IllegalArgumentException("정의되지 않은 탭 위치입니다: " + position);
        }
    }

    @Override
    public int getItemCount() {
        return TAB_COUNT;
    }
}