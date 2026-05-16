package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.api.data.chat.ChatPhotoData;
import com.example.honbabapp.databinding.RvChatPhotoGridBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * 채팅방 사진 3열 그리드 어댑터
 *
 * - ChatRoomPhotosActivity 에서 사용
 * - GridLayoutManager(this, 3) 와 함께 사용
 * - 정사각 비율 (rv_chat_photo_grid.xml 의 dimensionRatio="1:1")
 *
 * 무한 스크롤 지원:
 *   addItems() 로 페이지 단위 추가 (setItems 는 전체 교체)
 */
public class ChatPhotoGridAdapter
        extends RecyclerView.Adapter<ChatPhotoGridAdapter.ViewHolder> {

    private final List<ChatPhotoData> items = new ArrayList<>();

    // ─────────────────────────────────────────
    // 데이터 갱신
    // ─────────────────────────────────────────

    /** 리스트 전체 교체 (첫 페이지 로드 시) */
    public void setItems(List<ChatPhotoData> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    /**
     * 페이지 추가 (무한 스크롤 시 호출)
     * notifyItemRangeInserted 로 깜빡임 방지
     */
    public void addItems(List<ChatPhotoData> moreItems) {
        if (moreItems == null || moreItems.isEmpty()) return;
        int start = items.size();
        items.addAll(moreItems);
        notifyItemRangeInserted(start, moreItems.size());
    }

    /** 현재 아이템 수 (페이지 계산용) */
    public int getCurrentCount() {
        return items.size();
    }

    // ─────────────────────────────────────────
    // RecyclerView.Adapter 필수 메서드
    // ─────────────────────────────────────────

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RvChatPhotoGridBinding binding = RvChatPhotoGridBinding.inflate(
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

        private final RvChatPhotoGridBinding binding;

        ViewHolder(RvChatPhotoGridBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChatPhotoData item) {
            Glide.with(binding.getRoot().getContext())
                    .load(item.getImage_url())
                    .placeholder(R.color.bg_300)
                    .error(R.color.bg_300)
                    .centerCrop()
                    .into(binding.ivPhoto);
        }
    }
}