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
 *
 * 현재는 아이템 클릭 시 동작 없음 (V2 에서 풀스크린 뷰어 추가 예정).
 * 사진 더보기는 "더보기 >" 헤더 클릭으로 별도 처리.
 */
public class ChatPhotoPreviewAdapter
        extends RecyclerView.Adapter<ChatPhotoPreviewAdapter.ViewHolder> {

    // ─────────────────────────────────────────
    // 내부 상태
    // ─────────────────────────────────────────

    private List<ChatPhotoData> items = new ArrayList<>();

    // ─────────────────────────────────────────
    // 데이터 갱신
    // ─────────────────────────────────────────

    public void setItems(List<ChatPhotoData> newItems) {
        this.items = (newItems != null) ? newItems : new ArrayList<>();
        notifyDataSetChanged();
    }

    // ─────────────────────────────────────────
    // RecyclerView.Adapter 필수 메서드
    // ─────────────────────────────────────────

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RvChatPhotoPreviewBinding binding = RvChatPhotoPreviewBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
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

    // ─────────────────────────────────────────
    // ViewHolder
    // ─────────────────────────────────────────

    class ViewHolder extends RecyclerView.ViewHolder {

        private final RvChatPhotoPreviewBinding binding;

        ViewHolder(RvChatPhotoPreviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChatPhotoData item) {
            Glide.with(binding.getRoot().getContext())
                    .load(item.getImage_url())
                    .placeholder(R.color.bg_300)   // 로딩 중 회색 박스
                    .error(R.color.bg_300)
                    .centerCrop()
                    .into(binding.ivPhoto);
        }
    }
}