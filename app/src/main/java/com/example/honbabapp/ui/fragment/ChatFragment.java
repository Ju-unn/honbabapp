package com.example.honbabapp.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.honbabapp.R;
import com.example.honbabapp.activity.chat.ChatRoomActivity;
import com.example.honbabapp.api.data.chat.ChatRoomData;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.databinding.FragmentChatBinding;
import com.example.honbabapp.repository.ChatRepository;
import com.example.honbabapp.ui.adapter.ChatRoomListAdapter;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatFragment extends Fragment {
    private FragmentChatBinding binding;
    private ChatRoomListAdapter adapter;

    // 젠체 목록 (탭 / 검색 필터링)
    private List<ChatRoomData> allRooms = new ArrayList<>();
    private int myUserId = -1;

    public ChatFragment() {
        Log.i("ChatFragment" , "생성");
    }

    // ─────────────────────────────────────────
    // 생명주기
    // ─────────────────────────────────────────

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i("ChatFragment" , "onCreateView");

        binding = FragmentChatBinding.inflate(inflater , container , false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 사용자 ID 읽기
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("honbabapp_prefs" , Context.MODE_PRIVATE);
        myUserId = prefs.getInt("user_id" , -1);

        initRecyclerView();
        initTabLayout();
        initSearchBar();
        loadChatRooms();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // 메모리 누수 방지
    }

    // ─────────────────────────────────────────
    // 초기화
    // ─────────────────────────────────────────

    /**
     * RecyclerView 및 Adapter 초기화
     */
    private void initRecyclerView() {
        adapter = new ChatRoomListAdapter(room -> {
            // 채팅방 아이템 클릭 -> ChatRoomActivity 이동
            Intent intent = new Intent(requireContext(), ChatRoomActivity.class);
            intent.putExtra("room_id", room.getRoom_id());
            intent.putExtra("room_name", room.getRoom_name());
            intent.putExtra("room_type", room.getRoom_type());
            startActivity(intent);
        });

        binding.recyclerChatRooms.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerChatRooms.setAdapter(adapter);
    }

    /**
     * 탭 레이아웃 (전체 / 1:1 / 모임) 초기화
     */
    private void initTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                applyFilter();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    /**
     * 검색바 TextWatcher 초기화
     */
    private void initSearchBar() {
        binding.etSearchChat.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    // ─────────────────────────────────────────
    // 데이터 로드
    // ─────────────────────────────────────────

    /**
     * 채팅방 목록 API 호출
     */

    private void loadChatRooms() {
        if (myUserId == -1) return;
        Log.d("ChatFragment", "user_id: " + myUserId);

        ChatRepository.getInstance().getChatRooms(myUserId,
                new Callback<ApiResponse<List<ChatRoomData>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<ChatRoomData>>> call, Response<ApiResponse<List<ChatRoomData>>> response) {

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {
                            allRooms = response.body().getData();

                            if (allRooms == null) allRooms = new ArrayList<>();
                            Log.d("ChatFragment", "success: " + response.body().isSuccess());
                            Log.d("ChatFragment", "rooms size: " + allRooms.size());
                            applyFilter();
                        } else {
                            showEmpty();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<ChatRoomData>>> call, Throwable t) {
                        Log.e("ChatFragment", "실패: " + t.getMessage());
                        showEmpty();
                    }
                });
    }

    // ─────────────────────────────────────────
    // 필터링 (탭 + 검색어)
    // ─────────────────────────────────────────

    /**
     * 탭 선택 + 검색어를 동시에 적용해서 RecyclerView 갱신
     */

    private void applyFilter() {
        int tabPos = binding.tabLayout.getSelectedTabPosition();
        String keyword = binding.etSearchChat.getText().toString().trim().toLowerCase();

        List<ChatRoomData> filtered = new ArrayList<>();

        for (ChatRoomData room : allRooms) {
            // 탭 필터 ( 0 = 전체 , 1 = 1대1 , 2 = 모임)
            boolean tabMatch = true;
            if (tabPos == 1) tabMatch = "direct".equals(room.getRoom_type());
            if (tabPos == 2) tabMatch = "group".equals(room.getRoom_type());

            // 검색어 필터
            boolean keywordMatch  = keyword.isEmpty() ||
                    (room.getRoom_name() != null && room.getRoom_name().toLowerCase().contains(keyword));

            if (tabMatch && keywordMatch) filtered.add(room);
        }

        adapter.setRooms(filtered);

        // 빈 상태 뷰 토글
        if (filtered.isEmpty()) {
            showEmpty();
        } else {
            binding.recyclerChatRooms.setVisibility(View.VISIBLE);
            binding.layoutEmpty.setVisibility(View.GONE);
        }
    }

    /**
     * 빈 상태 표시
     */
    private void showEmpty() {
        binding.recyclerChatRooms.setVisibility(View.GONE);
        binding.layoutEmpty.setVisibility(View.VISIBLE);
    }



}