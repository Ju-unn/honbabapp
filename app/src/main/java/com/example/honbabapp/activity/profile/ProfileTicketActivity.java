package com.example.honbabapp.activity.profile;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.honbabapp.activity.payment.TicketPurchaseActivity;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.payment.MyTicketsResponse;
import com.example.honbabapp.databinding.PageProfileTicketBinding;
import com.example.honbabapp.util.InsetsUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 체험단 등록권 조회 Activity
 * - 내 보유 등록권 수량 표시
 * - "구매하기" 클릭 → TicketPurchaseActivity(토스 결제) 실행
 * - 결제 완료(RESULT_OK) 시 보유 수량 즉시 갱신
 */
public class ProfileTicketActivity extends AppCompatActivity {

    private static final String TAG = "ProfileTicketActivity";

    private PageProfileTicketBinding binding;
    private ApiService apiService;

    // 등록권 구매 화면 결과 수신용 런처
    private final ActivityResultLauncher<Intent> ticketPurchaseLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            loadTicketBalance(); // 구매 완료 즉시 보유 수량 반영
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageProfileTicketBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        InsetsUtils.applyRootInsets(binding.getRoot());

        apiService = RetrofitClient.getClient().create(ApiService.class);

        // 뒤로가기
        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // 구매하기 → 토스 결제 화면
        binding.btnBuyTicket.setOnClickListener(v ->
                ticketPurchaseLauncher.launch(new Intent(this, TicketPurchaseActivity.class)));

        loadTicketBalance();
    }

    /// 체험단 등록권 잔액 조회 후 표시
    private void loadTicketBalance() {
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
                    binding.tvTicketCount.setText(ticketBalance + "장");
                } else {
                    Log.e(TAG, "등록권 잔액 로드 실패");
                    Toast.makeText(ProfileTicketActivity.this,
                            "등록권 확인에 실패했습니다. 잠시 후 다시 시도해주세요",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MyTicketsResponse>> call, Throwable t) {
                if (binding == null) return;
                Log.e(TAG, "등록권 잔액 로드 네트워크 오류: " + t.getMessage());
                Toast.makeText(ProfileTicketActivity.this,
                        "등록권 확인에 실패했습니다. 잠시 후 다시 시도해주세요",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
