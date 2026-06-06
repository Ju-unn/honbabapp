package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.honbabapp.api.data.chat.ChatPhotoData;
import com.example.honbabapp.databinding.RvChatPhotoGridBinding;
import com.example.honbabapp.util.ChatGlide;

/**
 * 채팅방 사진 그리드 어댑터 (ChatRoomPhotosActivity).
 * 공통 로직(데이터/콜백/페이징)은 ChatPhotoBaseAdapter, 여기는 셀 레이아웃 바인딩만.
 */
public class ChatPhotoGridAdapter
        extends ChatPhotoBaseAdapter<ChatPhotoGridAdapter.ViewHolder> {

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RvChatPhotoGridBinding binding = RvChatPhotoGridBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(photoItems.get(position), position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final RvChatPhotoGridBinding binding;

        ViewHolder(RvChatPhotoGridBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ChatPhotoData photoItem, int position) {
            ChatGlide.loadSquareThumbnail(binding.ivPhoto, photoItem.getImage_url());
            binding.getRoot().setOnClickListener(view -> dispatchPhotoClick(position));
        }
    }
}
