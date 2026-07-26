package com.example.honbabapp.activity.payment;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.payment.PaymentHistoryResponse;
import com.example.honbabapp.databinding.PagePaymentHistoryBinding;
import com.example.honbabapp.ui.adapter.PaymentHistoryAdapter;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 등록권 구매내역 Activity
 * - 전자상거래법상 구매내역 확인 수단 (DONE/FAILED 결제 이력 최신순)
 * - ChatRoomPhotosActivity와 동일한 무한 스크롤 패턴
 */
public class PaymentHistoryActivity extends AppCompatActivity {

    private static final String TAG = "PaymentHistoryActivity";
    private static final int PAGE_SIZE = 20;
    private static final int LOAD_MORE_THRESHOLD = 5;

    private PagePaymentHistoryBinding binding;
    private ApiService apiService;
    private PaymentHistoryAdapter adapter;

    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PagePaymentHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getClient().create(ApiService.class);

        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        initRecyclerView();
        loadPage(1, true);
    }

    private void initRecyclerView() {
        adapter = new PaymentHistoryAdapter();
        binding.rvPaymentHistory.setAdapter(adapter);

        // 무한 스크롤 리스너 (ChatRoomPhotosActivity 동일 패턴)
        binding.rvPaymentHistory.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                super.onScrolled(rv, dx, dy);
                if (dy <= 0) return;

                LinearLayoutManager llm = (LinearLayoutManager) rv.getLayoutManager();
                if (llm == null) return;

                int total = llm.getItemCount();
                int lastVisible = llm.findLastVisibleItemPosition();

                if (!isLoading && hasMore && lastVisible >= total - LOAD_MORE_THRESHOLD) {
                    loadPage(currentPage + 1, false);
                }
            }
        });
    }

    private void loadPage(int page, boolean isFirst) {
        if (isLoading) return;
        isLoading = true;

        apiService.getPaymentHistory(page, PAGE_SIZE)
                .enqueue(new Callback<ApiResponse<PaymentHistoryResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<PaymentHistoryResponse>> call,
                                           Response<ApiResponse<PaymentHistoryResponse>> response) {
                        isLoading = false;
                        if (binding == null) return;

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()
                                && response.body().getData() != null) {

                            PaymentHistoryResponse data = response.body().getData();

                            currentPage = data.getPage();
                            hasMore = data.isHas_more();

                            if (isFirst) {
                                adapter.setItems(data.getPayments());
                                showEmptyState(data.getPayments() == null
                                        || data.getPayments().isEmpty());
                            } else {
                                adapter.addItems(data.getPayments());
                            }

                        } else {
                            Log.e(TAG, "구매내역 로드 실패: " + response.code());
                            if (isFirst) showEmptyState(true);
                            Toast.makeText(PaymentHistoryActivity.this,
                                    "구매내역을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<PaymentHistoryResponse>> call,
                                          Throwable throwable) {
                        isLoading = false;
                        if (binding == null) return;
                        Log.e(TAG, "구매내역 네트워크 오류: " + throwable.getMessage());
                        if (isFirst) showEmptyState(true);
                        Toast.makeText(PaymentHistoryActivity.this,
                                "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showEmptyState(boolean isEmpty) {
        binding.rvPaymentHistory.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        binding.tvEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
