package com.example.honbabapp.activity.payment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.example.honbabapp.BuildConfig;
import com.example.honbabapp.R;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.request.payment.ConfirmPaymentRequest;
import com.example.honbabapp.api.data.request.payment.PreparePaymentRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.payment.ConfirmPaymentResponse;
import com.example.honbabapp.api.data.response.payment.PreparePaymentResponse;
import com.example.honbabapp.databinding.PageTicketPurchaseBinding;
import com.example.honbabapp.util.InsetsUtils;
import com.example.honbabapp.util.ToastUtil;
import com.google.android.material.card.MaterialCardView;
import com.tosspayments.paymentsdk.PaymentWidget;
import com.tosspayments.paymentsdk.model.PaymentCallback;
import com.tosspayments.paymentsdk.model.PaymentWidgetStatusListener;
import com.tosspayments.paymentsdk.model.TossPaymentResult;
import com.tosspayments.paymentsdk.view.PaymentMethod;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 체험단 등록권 구매 Activity
 * 흐름:
 * 1) 상품(1회/5회/10회) 선택 → "다음" 클릭
 * 2) preparePayment() 호출 → 서버가 계산한 order_id/amount/order_name 수신
 * 3) 받은 정보로 토스페이먼츠 결제위젯을 화면에 렌더링
 * 4) "결제하기" 클릭 → PaymentWidget.requestPayment() 호출
 * 5) 결제 성공 콜백 → onTossPaymentSuccess() 단일 진입점 → confirmPayment() 호출
 * 6) 등록권 잔액 반영 완료 → 결과를 RESULT_OK로 돌려주고 종료
 */
public class TicketPurchaseActivity extends AppCompatActivity {

    private static final String TAG = "TicketPurchaseActivity";
    private static final String PREFERENCES_NAME = "honbabapp_prefs";
    private static final String PREFERENCE_KEY_TOSS_CUSTOMER_KEY = "toss_customer_key";

    private static final String PRODUCT_CODE_TICKET_1 = "TICKET_1";
    private static final String PRODUCT_CODE_TICKET_5 = "TICKET_5";
    private static final String PRODUCT_CODE_TICKET_10 = "TICKET_10";

    private PageTicketPurchaseBinding binding;
    private ApiService apiService;

    private String selectedProductCode = null;

    private PaymentWidget paymentWidget = null;

    // preparePayment() 응답으로 받은, 결제 요청에 사용할 주문 정보
    private String preparedOrderId = null;
    private String preparedOrderName = null;
    private int preparedAmount = 0;

    // 토스 결제 성공 콜백에서 받은 값 (confirmPayment 재시도를 위해 보관)
    private String lastPaymentKey = null;
    private String lastConfirmOrderId = null;
    private int lastConfirmAmount = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageTicketPurchaseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        InsetsUtils.applyRootInsets(binding.getRoot());

        apiService = RetrofitClient.getClient().create(ApiService.class);

        String tossClientKey = BuildConfig.TOSS_CLIENT_KEY;
        String customerKey = getOrCreateTossCustomerKey();

        paymentWidget = new PaymentWidget(this, tossClientKey, customerKey, null);

        setupUi();
    }

    private void setupUi() {
        binding.btnBack.setOnClickListener(view -> finish());

        binding.cardTicket1.setOnClickListener(view -> selectTicketProduct(PRODUCT_CODE_TICKET_1));
        binding.cardTicket5.setOnClickListener(view -> selectTicketProduct(PRODUCT_CODE_TICKET_5));
        binding.cardTicket10.setOnClickListener(view -> selectTicketProduct(PRODUCT_CODE_TICKET_10));

        binding.btnNext.setOnClickListener(view -> onNextButtonClicked());
        binding.btnRetryConfirm.setOnClickListener(view -> retryConfirmPayment());
    }

    // ─────────────────────────────────────────────────────────────
    // 상품 카드 선택
    // ─────────────────────────────────────────────────────────────

    /**
     * 등록권 상품 카드 선택 처리.
     * 선택된 상품 코드를 저장하고, RadioButton 체크 표시와 카드 테두리 색상을
     * 함께 갱신한다.
     *
     * @param productCode 선택된 상품 코드 (PRODUCT_CODE_TICKET_1/5/10 중 하나)
     */
    private void selectTicketProduct(String productCode) {
        selectedProductCode = productCode;

        boolean isTicket1Selected = PRODUCT_CODE_TICKET_1.equals(productCode);
        boolean isTicket5Selected = PRODUCT_CODE_TICKET_5.equals(productCode);
        boolean isTicket10Selected = PRODUCT_CODE_TICKET_10.equals(productCode);

        binding.radioTicket1.setChecked(isTicket1Selected);
        binding.radioTicket5.setChecked(isTicket5Selected);
        binding.radioTicket10.setChecked(isTicket10Selected);

        updateCardStrokeColor(binding.cardTicket1, isTicket1Selected);
        updateCardStrokeColor(binding.cardTicket5, isTicket5Selected);
        updateCardStrokeColor(binding.cardTicket10, isTicket10Selected);
    }

    /**
     * 카드 테두리 색상만 선택 상태에 맞춰 갱신한다.
     */
    private void updateCardStrokeColor(MaterialCardView card, boolean isSelected) {
        int strokeColorResourceId = isSelected ? R.color.primary_100 : R.color.border_100;
        card.setStrokeColor(ContextCompat.getColor(this, strokeColorResourceId));
    }

    // ─────────────────────────────────────────────────────────────
    // 1단계: 결제 준비
    // ─────────────────────────────────────────────────────────────

    private void onNextButtonClicked() {
        if (selectedProductCode == null) {
            ToastUtil.makeText(this, "구매할 등록권을 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnNext.setEnabled(false);
        binding.btnNext.setText("불러오는 중...");

        PreparePaymentRequest preparePaymentRequest = new PreparePaymentRequest(selectedProductCode);

        apiService.preparePayment(preparePaymentRequest)
                .enqueue(new Callback<ApiResponse<PreparePaymentResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<PreparePaymentResponse>> call,
                                           Response<ApiResponse<PreparePaymentResponse>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()
                                && response.body().getData() != null) {

                            PreparePaymentResponse preparePaymentResponse = response.body().getData();
                            preparedOrderId = preparePaymentResponse.getOrder_id();
                            preparedOrderName = preparePaymentResponse.getOrder_name();
                            preparedAmount = preparePaymentResponse.getAmount();

                            showPaymentWidget();

                        } else {
                            String message = response.body() != null
                                    ? response.body().getMessage() : "결제 준비에 실패했습니다.";
                            ToastUtil.makeText(TicketPurchaseActivity.this, message, Toast.LENGTH_SHORT).show();
                            resetNextButton();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<PreparePaymentResponse>> call, Throwable throwable) {
                        Log.e(TAG, "결제 준비 네트워크 오류: " + throwable.getMessage());
                        ToastUtil.makeText(TicketPurchaseActivity.this,
                                "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                        resetNextButton();
                    }
                });
    }

    private void resetNextButton() {
        binding.btnNext.setEnabled(true);
        binding.btnNext.setText("다음");
    }

    // ─────────────────────────────────────────────────────────────
    // 2단계: 결제위젯 렌더링
    // ─────────────────────────────────────────────────────────────

    private void showPaymentWidget() {
        binding.layoutPaymentSection.setVisibility(View.VISIBLE);

        PaymentWidgetStatusListener paymentWidgetStatusListener = new PaymentWidgetStatusListener() {
            @Override
            public void onLoad() {
                Log.i(TAG, "결제위젯 렌더링 완료");
            }

            @Override
            public void onFail(TossPaymentResult.Fail fail) {
                Log.w(TAG, "결제 정보 입력 안내: " + fail.getErrorMessage());
                ToastUtil.makeText(TicketPurchaseActivity.this,
                        fail.getErrorMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        // 3번째 인자(options)는 여러 개의 결제 UI를 구분할 때만 쓰는 variantKey를
        // 담는 객체다. 결제 UI를 하나만 쓰므로 null(기본 UI)을 그대로 넘긴다.
        paymentWidget.renderPaymentMethods(
                binding.paymentMethodWidget,
                new PaymentMethod.Rendering.Amount(
                        preparedAmount,
                        PaymentMethod.Rendering.Currency.KRW,
                        "KR"
                ),
                null,
                paymentWidgetStatusListener
        );
        paymentWidget.renderAgreement(binding.agreementWidget);

        binding.btnNext.setText("결제하기 · " + formatAmount(preparedAmount) + "원");
        binding.btnNext.setEnabled(true);
        binding.btnNext.setOnClickListener(view -> requestTossPayment());
    }

    private void requestTossPayment() {
        if (paymentWidget == null || preparedOrderId == null) {
            ToastUtil.makeText(this, "결제 정보를 다시 불러와주세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnNext.setEnabled(false);

        PaymentMethod.PaymentInfo paymentInfo =
                new PaymentMethod.PaymentInfo(preparedOrderId, preparedOrderName);

        try {
            paymentWidget.requestPayment(paymentInfo, new PaymentCallback() {
                @Override
                public void onPaymentSuccess(TossPaymentResult.Success success) {
                    onTossPaymentSuccess(success.getPaymentKey(), success.getOrderId(), success.getAmount().intValue());
                }

                @Override
                public void onPaymentFailed(TossPaymentResult.Fail fail) {
                    binding.btnNext.setEnabled(true);
                    if (!"USER_CANCEL".equals(fail.getErrorCode())) {
                        ToastUtil.makeText(TicketPurchaseActivity.this,
                                fail.getErrorMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (IllegalAccessException illegalAccessException) {

            Log.e(TAG, "결제위젯 미렌더링 상태에서 결제 요청됨: " + illegalAccessException.getMessage());
            ToastUtil.makeText(this, "결제 화면을 다시 불러와주세요.", Toast.LENGTH_SHORT).show();
            binding.btnNext.setEnabled(true);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 3단계: 결제 성공 → 승인 요청 (단일 진입점)
    // ─────────────────────────────────────────────────────────────

    private void onTossPaymentSuccess(String paymentKey, String orderId, int amount) {
        lastPaymentKey = paymentKey;
        lastConfirmOrderId = orderId;
        lastConfirmAmount = amount;

        binding.btnNext.setText("결제 승인 중...");
        binding.btnRetryConfirm.setVisibility(View.GONE);

        confirmPayment(paymentKey, orderId, amount);
    }

    private void confirmPayment(String paymentKey, String orderId, int amount) {
        ConfirmPaymentRequest confirmPaymentRequest =
                new ConfirmPaymentRequest(paymentKey, orderId, amount);

        apiService.confirmPayment(confirmPaymentRequest)
                .enqueue(new Callback<ApiResponse<ConfirmPaymentResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<ConfirmPaymentResponse>> call,
                                           Response<ApiResponse<ConfirmPaymentResponse>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()
                                && response.body().getData() != null) {

                            int purchasedTicketCount = response.body().getData().getTicket_count();
                            ToastUtil.makeText(TicketPurchaseActivity.this,
                                    "등록권 " + purchasedTicketCount + "개가 충전되었습니다!",
                                    Toast.LENGTH_SHORT).show();

                            setResult(RESULT_OK);
                            finish();

                        } else {
                            String message = response.body() != null
                                    ? response.body().getMessage() : "결제 승인에 실패했습니다.";
                            ToastUtil.makeText(TicketPurchaseActivity.this, message, Toast.LENGTH_SHORT).show();
                            showRetryConfirmButton();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<ConfirmPaymentResponse>> call, Throwable throwable) {
                        Log.e(TAG, "결제 승인 네트워크 오류: " + throwable.getMessage());
                        ToastUtil.makeText(TicketPurchaseActivity.this,
                                "네트워크 오류로 결제 승인에 실패했습니다. 다시 시도해주세요.",
                                Toast.LENGTH_SHORT).show();
                        showRetryConfirmButton();
                    }
                });
    }

    private void showRetryConfirmButton() {
        binding.btnNext.setText("결제하기 · " + formatAmount(preparedAmount) + "원");
        binding.btnRetryConfirm.setVisibility(View.VISIBLE);
    }

    private void retryConfirmPayment() {
        if (lastPaymentKey == null || lastConfirmOrderId == null) {
            ToastUtil.makeText(this, "재시도할 결제 정보가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        binding.btnRetryConfirm.setEnabled(false);
        confirmPayment(lastPaymentKey, lastConfirmOrderId, lastConfirmAmount);
    }

    // ─────────────────────────────────────────────────────────────
    // 유틸
    // ─────────────────────────────────────────────────────────────

    /**
     * 토스페이먼츠 customerKey는 "충분히 무작위적인 값"이어야 하며,
     * 자동증가 숫자나 이메일/전화번호처럼 유추 가능한 값은 사용하면 안 된다.
     * 따라서 user_id를 그대로 쓰지 않고, 기기별로 한 번 생성한 UUID를
     * SharedPreferences에 저장해서 재사용한다.
     */
    private String getOrCreateTossCustomerKey() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        String existingCustomerKey = sharedPreferences.getString(PREFERENCE_KEY_TOSS_CUSTOMER_KEY, null);

        if (existingCustomerKey != null) {
            return existingCustomerKey;
        }

        String newCustomerKey = UUID.randomUUID().toString();
        sharedPreferences.edit()
                .putString(PREFERENCE_KEY_TOSS_CUSTOMER_KEY, newCustomerKey)
                .apply();
        return newCustomerKey;
    }

    private String formatAmount(int amount) {
        return NumberFormat.getInstance(Locale.KOREA).format(amount);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}