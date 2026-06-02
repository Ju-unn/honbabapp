package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.api.data.chat.ChatPhotoData;
import com.example.honbabapp.databinding.RvChatPhotoPreviewBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * 채팅방 사진 가로 미리보기 어댑터
 *
 * - ChatRoomDetailActivity 의 사진 섹션에서 사용
 * - 가로 RecyclerView (LinearLayoutManager HORIZONTAL)
 * - 최대 10장 정사각 80dp 썸네일
 */
public class ChatPhotoPreviewAdapter
        extends RecyclerView.Adapter<ChatPhotoPreviewAdapter.ViewHolder> {

    // ── 클릭 콜백 ─────────────────────────────

    public interface OnPhotoClickListener {
        void onPhotoClick(List<ChatPhotoData> items, int position);
    }

    private OnPhotoClickListener clickListener;

    public void setOnPhotoClickListener(OnPhotoClickListener listener) {
        this.clickListener = listener;
    }

    // ── 데이터 ────────────────────────────────

    private List<ChatPhotoData> items = new ArrayList<>();

    public void setItems(List<ChatPhotoData> newItems) {
        this.items = (newItems != null) ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    // ── Adapter ───────────────────────────────

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RvChatPhotoPreviewBinding binding = RvChatPhotoPreviewBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ── ViewHolder ────────────────────────────

    class ViewHolder extends RecyclerView.ViewHolder {
        private final RvChatPhotoPreviewBinding binding;

        ViewHolder(RvChatPhotoPreviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChatPhotoData item, int position) {
            Glide.with(binding.getRoot().getContext())
                    .load(item.getImage_url())
                    .placeholder(R.color.bg_300)
                    .error(R.color.bg_300)
                    .centerCrop()
                    .into(binding.ivPhoto);

            binding.getRoot().setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onPhotoClick(items, position);
                }
            });
        }
    }
}