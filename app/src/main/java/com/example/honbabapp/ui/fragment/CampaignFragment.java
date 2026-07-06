package com.example.honbabapp.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.honbabapp.activity.campaign.CampaignWritingActivity;
import com.example.honbabapp.activity.payment.TicketPurchaseActivity;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.meeting.Meeting;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.meeting.MeetingListResponse;
import com.example.honbabapp.api.data.response.payment.MyTicketsResponse;
import com.example.honbabapp.databinding.FragmentCampaignBinding;
import com.example.honbabapp.ui.adapter.CampaignListAdapter;
import com.example.honbabapp.ui.dialog.CampaignFilterBottomSheetDialog;
import com.example.honbabapp.ui.dialog.CampaignSortBottomSheetDialog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CampaignFragment extends Fragment {

    private static final String TAG = "CampaignFragment";

    private FragmentCampaignBinding binding;
    private ApiService apiService;
    private CampaignListAdapter campaignListAdapter;

    // 현재 적용된 정렬/필터 상태
    private String currentSortOption = "최신순";
    private Set<String> currentChannels = new HashSet<>();
    private Set<String> currentCategories = new HashSet<>();
    private int currentMinPrice = 0;
    private int currentMaxPrice = 100000;

    // 지역 필터 상태 (시/도 → 구)
    private String currentSido = null;
    private String currentSigungu = null;

    // ─────────────────────────────────────────────────────────────
    // 생명주기
    // ─────────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentCampaignBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = RetrofitClient.getClient().create(ApiService.class);

        initRecyclerView();
        initSortButton();
        initFilterButton();
        initFabButton();
        loadCampaignList();
    }

    @Override
    public void onResume() {
        super.onResume();
        // CampaignWritingActivity에서 등록 후 돌아왔을 때 목록 갱신
        if (binding != null) loadCampaignList();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // 등록권 결제 화면 결과 수신용 런처 — 결제 성공 시 자동으로 글쓰기 화면 진입
    private final ActivityResultLauncher<Intent> ticketPurchaseLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            startActivity(new Intent(requireActivity(), CampaignWritingActivity.class));
                        }
                    }
            );

    // ─────────────────────────────────────────────────────────────
    // RecyclerView 초기화 (기존 그대로)
    // ─────────────────────────────────────────────────────────────

    private void initRecyclerView() {
        campaignListAdapter = new CampaignListAdapter();
        binding.rvCampaignList.setAdapter(campaignListAdapter);
    }

    // ─────────────────────────────────────────────────────────────
    // API 호출 — getCampaignList
    // HomeFragment.loadMeetings() / MapFragment.loadMeetingsOnMap() 동일 패턴
    // ─────────────────────────────────────────────────────────────

    private void loadCampaignList() {
        // 정렬 옵션 변환 (서버 파라미터)
        String orderBy = convertSortOption(currentSortOption);

        // 채널 필터: Set → 쉼표 구분 문자열 (없으면 null)
        String channels = currentChannels.isEmpty()
                ? null
                : String.join(",", currentChannels);

        // 카테고리 필터
        String categories = currentCategories.isEmpty()
                ? null
                : String.join(",", currentCategories);

        // 가격 필터: 기본값이면 null → 서버 필터 비활성
        Integer minPrice = (currentMinPrice > 0) ? currentMinPrice : null;
        Integer maxPrice = (currentMaxPrice < 100000) ? currentMaxPrice : null;

        Call<ApiResponse<MeetingListResponse>> call = apiService.getCampaignList(
                "체험단",
                "모집중",
                1,
                30,
                orderBy,
                channels,
                categories,
                minPrice,
                maxPrice,
                currentSido,
                currentSigungu
        );

        call.enqueue(new Callback<ApiResponse<MeetingListResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MeetingListResponse>> call,
                                   Response<ApiResponse<MeetingListResponse>> response) {
                if (binding == null) return;

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().isSuccess()) {

                    List<Meeting> rawList = response.body().getData().getMeetings();

                    // 만료된 체험단 클라이언트 필터링
                    List<Meeting> campaignList = new ArrayList<>();
                    for (Meeting meeting : rawList) {
                        if (!meeting.isExpired()) {
                            campaignList.add(meeting);
                        }
                    }

                    Log.i(TAG, "체험단 목록 로드 성공: " + campaignList.size() + "개");
                    campaignListAdapter.updateCampaignList(campaignList);
                    showEmptyState(campaignList.isEmpty());

                } else {
                    Log.e(TAG, "API 실패: " + (response.body() != null
                            ? response.body().getMessage() : response.code()));
                    showEmptyState(true);
                    Toast.makeText(getContext(),
                            "체험단 목록을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MeetingListResponse>> call, Throwable throwable) {
                if (binding == null) return;
                Log.e(TAG, "네트워크 오류: " + throwable.getMessage());
                showEmptyState(true);
                Toast.makeText(getContext(),
                        "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 정렬 옵션 → 서버 order_by 파라미터 변환
     * "최신순" → null (서버 기본값), "마감임박순" → "deadline", "인기순" → null (클라이언트 정렬)
     */
    private String convertSortOption(String sortOption) {
        switch (sortOption) {
            case "마감임박순": return "deadline";
            default: return null; // 최신순 / 인기순은 서버 기본 정렬 사용
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 정렬 버튼
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
            loadCampaignList(); // 정렬 변경 시 재호출
        };
        dialog.show(getChildFragmentManager(), "campaignSortDialog");
    }

    private void updateSortButtonText() {
        binding.tvSortOption.setText(currentSortOption);
    }

    // ─────────────────────────────────────────────────────────────
    // 필터 버튼 (기존 그대로 + loadCampaignList 연결 + 지역 필터 추가)
    // ─────────────────────────────────────────────────────────────

    private void initFilterButton() {
        binding.btnFilter.setOnClickListener(v -> showFilterDialog());
    }

    private void showFilterDialog() {
        CampaignFilterBottomSheetDialog dialog = new CampaignFilterBottomSheetDialog();
        dialog.initialChannels   = new HashSet<>(currentChannels);
        dialog.initialCategories = new HashSet<>(currentCategories);
        dialog.initialMinPrice   = currentMinPrice;
        dialog.initialMaxPrice   = currentMaxPrice;
        dialog.initialSido       = currentSido;
        dialog.initialSigungu    = currentSigungu;

        dialog.listener = (channels, categories, minPrice, maxPrice, sido, sigungu) -> {
            currentChannels    = new HashSet<>(channels);
            currentCategories  = new HashSet<>(categories);
            currentMinPrice    = minPrice;
            currentMaxPrice    = maxPrice;
            currentSido        = sido;
            currentSigungu     = sigungu;
            updateFilterButtonState();
            loadCampaignList(); // 필터 변경 시 재호출
        };

        dialog.show(getChildFragmentManager(), "campaignFilterDialog");
    }

    /**
     * 필터 버튼 활성 상태 표시
     * 채널/카테고리/가격/지역 중 하나라도 기본값과 다르면 필터 버튼을 selected 상태로 표시
     */
    private void updateFilterButtonState() {
        boolean hasActiveFilter =
                !currentChannels.isEmpty()
                        || !currentCategories.isEmpty()
                        || currentMinPrice > 0
                        || currentMaxPrice < 100000
                        || currentSido != null
                        || currentSigungu != null;

        binding.btnFilter.setSelected(hasActiveFilter);
    }

    // ─────────────────────────────────────────────────────────────
    // FAB
    // ─────────────────────────────────────────────────────────────

    private void initFabButton() {
        binding.fabCreateCampaign.setOnClickListener(v -> checkTicketBalanceAndProceed());
    }

    /**
     * FAB 클릭 시 등록권 잔액을 먼저 확인한다.
     * - 잔액 있음 → 바로 글쓰기 화면
     * - 잔액 0 → 결제 화면으로 직행 (다이얼로그 없이)
     * - 네트워크 오류 → 잔액을 확정할 수 없으므로 화면 전환하지 않고 토스트만 노출
     *   (CampaignWritingActivity.registerCampaign()의 NO_TICKET 처리가
     *    동시성 케이스에 대한 최종 안전망 역할을 하므로 이중 안전망 확보됨)
     */
    private void checkTicketBalanceAndProceed() {
        apiService.getMyTickets().enqueue(new Callback<ApiResponse<MyTicketsResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MyTicketsResponse>> call,
                                   Response<ApiResponse<MyTicketsResponse>> response) {
                if (binding == null) return;

                if (response.isSuccessful()
                        && response.body() != null
                        && response.body().isSuccess()
                        && response.body().getData() != null) {

                    int ticketBalance = response.body().getData().getTicket_balance();

                    if (ticketBalance > 0) {
                        startActivity(new Intent(requireActivity(), CampaignWritingActivity.class));
                    } else {
                        ticketPurchaseLauncher.launch(
                                new Intent(requireActivity(), TicketPurchaseActivity.class));
                    }
                } else {
                    Toast.makeText(getContext(),
                            "등록권 확인에 실패했습니다. 잠시 후 다시 시도해주세요",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MyTicketsResponse>> call, Throwable throwable) {
                if (binding == null) return;
                Log.e(TAG, "등록권 잔액 확인 네트워크 오류: " + throwable.getMessage());
                Toast.makeText(getContext(),
                        "등록권 확인에 실패했습니다. 잠시 후 다시 시도해주세요",
                        Toast.LENGTH_SHORT).show();
            }
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

    /** MainActivity 등 외부에서 강제 갱신 시 호출 */
    public void refreshCampaignList() {
        loadCampaignList();
    }
}