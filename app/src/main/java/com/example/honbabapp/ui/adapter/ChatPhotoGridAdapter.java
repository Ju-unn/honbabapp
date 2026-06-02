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

public class ChatPhotoGridAdapter
        extends RecyclerView.Adapter<ChatPhotoGridAdapter.ViewHolder> {

    // ── 클릭 콜백 ─────────────────────────────
    public interface OnPhotoClickListener {
        void onPhotoClick(List<ChatPhotoData> items, int position);
    }

    private OnPhotoClickListener clickListener;

    public void setOnPhotoClickListener(OnPhotoClickListener listener) {
        this.clickListener = listener;
    }

    // ── 데이터 ────────────────────────────────
    private final List<ChatPhotoData> items = new ArrayList<>();

    public void setItems(List<ChatPhotoData> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void addItems(List<ChatPhotoData> moreItems) {
        if (moreItems == null || moreItems.isEmpty()) return;
        int start = items.size();
        items.addAll(moreItems);
        notifyItemRangeInserted(start, moreItems.size());
    }

    public int getCurrentCount() { return items.size(); }

    // ── Adapter ───────────────────────────────

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RvChatPhotoGridBinding binding = RvChatPhotoGridBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position), position);
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ── ViewHolder ────────────────────────────

    class ViewHolder extends RecyclerView.ViewHolder {
        private final RvChatPhotoGridBinding binding;

        ViewHolder(RvChatPhotoGridBinding binding) {
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