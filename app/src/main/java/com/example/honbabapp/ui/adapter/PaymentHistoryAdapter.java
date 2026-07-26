package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.honbabapp.R;
import com.example.honbabapp.api.data.response.payment.PaymentHistoryItem;
import com.example.honbabapp.databinding.RvPaymentHistoryItemBinding;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 구매내역(등록권 결제 이력) 어댑터
 */
public class PaymentHistoryAdapter extends RecyclerView.Adapter<PaymentHistoryAdapter.ViewHolder> {

    private final List<PaymentHistoryItem> items = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RvPaymentHistoryItemBinding binding = RvPaymentHistoryItemBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * 첫 페이지 로드 (전체 교체)
     */
    public void setItems(List<PaymentHistoryItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    /**
     * 다음 페이지 데이터 추가 (무한 스크롤)
     */
    public void addItems(List<PaymentHistoryItem> newItems) {
        if (newItems == null || newItems.isEmpty()) return;
        int startPosition = items.size();
        items.addAll(newItems);
        notifyItemRangeInserted(startPosition, newItems.size());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final RvPaymentHistoryItemBinding binding;

        public ViewHolder(RvPaymentHistoryItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(PaymentHistoryItem item) {
            binding.tvOrderName.setText(item.getOrder_name());

            String amountText = NumberFormat.getInstance(Locale.KOREA).format(item.getAmount()) + "원";
            binding.tvPaymentAmount.setText(amountText);

            // 승인 시각 (FAILED면 confirmed_at이 null일 수 있음)
            String confirmedAt = item.getConfirmed_at();
            binding.tvPaymentDate.setText(confirmedAt != null ? confirmedAt : "-");

            boolean isDone = "DONE".equals(item.getStatus());
            binding.tvPaymentStatus.setText(isDone ? "결제완료" : "결제실패");
            binding.tvPaymentStatus.setTextColor(ContextCompat.getColor(
                    itemView.getContext(),
                    isDone ? R.color.primary_100 : R.color.error_red));
        }
    }
}
