package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.honbabapp.api.data.chat.ChatPhotoData;
import com.example.honbabapp.databinding.RvChatPhotoPreviewBinding;
import com.example.honbabapp.util.ChatGlide;

/**
 * 채팅방 사진 가로 미리보기 어댑터 (ChatRoomDetailActivity 사진 섹션).
 * 가로 RecyclerView, 정사각 80dp 썸네일. 공통 로직은 ChatPhotoBaseAdapter.
 */
public class ChatPhotoPreviewAdapter
        extends ChatPhotoBaseAdapter<ChatPhotoPreviewAdapter.ViewHolder> {

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RvChatPhotoPreviewBinding binding = RvChatPhotoPreviewBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(photoItems.get(position), position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final RvChatPhotoPreviewBinding binding;

        ViewHolder(RvChatPhotoPreviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChatPhotoData photoItem, int position) {
            ChatGlide.loadSquareThumbnail(binding.ivPhoto, photoItem.getImage_url());
            binding.getRoot().setOnClickListener(view -> dispatchPhotoClick(position));
        }
    }
}
