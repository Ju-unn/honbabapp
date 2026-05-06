package com.example.honbabapp.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.Tag;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.honbabapp.activity.chat.ChatRoomActivity;
import com.example.honbabapp.api.data.chat.ChatRoomData;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.databinding.FragmentChatBinding;
import com.example.honbabapp.network.SocketManager;
import com.example.honbabapp.repository.ChatRepository;
import com.example.honbabapp.ui.adapter.ChatRoomListAdapter;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.socket.emitter.Emitter;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatFragment extends Fragment {
    private FragmentChatBinding binding;
    private ChatRoomListAdapter adapter;

    // 젠체 목록 (탭 / 검색 필터링)
    private List<ChatRoomData> allRooms = new ArrayList<>();
    private int myUserId = -1;
    private Emitter.Listener roomMessageListener;

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

        // 채팅 목록 화면에서도 실시간 last_message 갱신
        SocketManager.getInstance().connect();

        // 변경 후
        roomMessageListener = args -> {
            try {
                JSONObject json    = (JSONObject) args[0];
                int roomId         = json.getInt("room_id");
                String content     = json.getString("content");
                String time        = json.getString("created_at");
                int senderId       = json.optInt("user_id", -1);

                requireActivity().runOnUiThread(() -> {
                    // allRooms 직접 수정
                    for (ChatRoomData room : allRooms) {
                        if (room.getRoom_id() == roomId) {
                            room.setLast_message(content);
                            room.setLast_msg_time(time);
                            // 내가 보낸 메시지가 아닐 때만 뱃지 +1
                            if (senderId != myUserId) {
                                Log.i("ChatFragment" , "플러스전" + room.getRoom_id() + "방번호 : " + room.getUnread_count() + "");
                                room.setUnread_count(room.getUnread_count() + 1);
                                Log.i("ChatFragment" , "플러스 후 " +  room.getRoom_id() + "방번호 : " + room.getUnread_count() + "");
                            }
                            break;
                        }
                    }
                    // 최신 메시지 방을 위로 정렬 후 화면 갱신
                    Collections.sort(allRooms, (a, b) -> {
                        String tA = a.getLast_msg_time();
                        String tB = b.getLast_msg_time();
                        if (tA == null) return 1;
                        if (tB == null) return -1;
                        return tB.compareTo(tA); // 내림차순 (최신이 위)
                    });
                    applyFilter();
                });
            } catch (Exception e) { /* 파싱 실패 무시 */ }
        };
        SocketManager.getInstance().getSocket().on("message", roomMessageListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (roomMessageListener != null)
            SocketManager.getInstance().getSocket().off("message", roomMessageListener);
        binding = null; // 메모리 누수 방지
    }

    // 채팅방에서 나왔을 때 목록 갱신
    // onResume이 없으면 ChatRoomActivity에서 돌아와도 목록이 그대로 (새 메시지 미반영)
    @Override
    public void onResume() {
        super.onResume();
        loadChatRooms();
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
            intent.putExtra("member_count", room.getParticipants() != null ? room.getParticipants().size() : 0);
            startActivity(intent);
        }, myUserId); // 1:1 방에서 상대방 프로필 찾으려면 내 userId 필요

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
            // keyword가 공백이거나 , roomName이 null이 아니고 roomName이 keyword를 포함하고 있다면 true
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