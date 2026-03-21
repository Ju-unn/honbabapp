package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.databinding.RvImageSelectBinding;

import java.util.ArrayList;
import java.util.List;

public class ImageSelectAdapter extends RecyclerView.Adapter<ImageSelectAdapter.ViewHolder> {

    private final List<String> imageUrls = new ArrayList<>();
    private int selectedPosition = -1; // 선택된 인덱스 (-1 =미선택);

    private OnImageSelectListener listener;

    public interface OnImageSelectListener {
        void onImageSelected(String imageUrl);
    }
    public void setOnImageSelectListener(OnImageSelectListener listener) {
        this.listener = listener;
    }

    public void setImageUrls(List<String> urls) {
        imageUrls.clear();
        imageUrls.addAll(urls);
        selectedPosition = -1;
        notifyDataSetChanged();
    }

    // 현재 선택된 이미지 url 반환
    public String getSelectedUrl() {
        if (selectedPosition < 0 || selectedPosition >= imageUrls.size()) return null;
        return imageUrls.get(selectedPosition);
    }

    @NonNull
    @Override
    public ImageSelectAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RvImageSelectBinding binding = RvImageSelectBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageSelectAdapter.ViewHolder holder, int position) {
        String url = imageUrls.get(position);
        boolean isSelected = (position == selectedPosition);

        // glide로 이미지 로드
        Glide.with(holder.itemView.getContext())
                .load(url)
                .centerCrop()
                .placeholder(R.drawable.photo_upload_background)
                .into(holder.binding.ivImage);

        // 오버레이 표시 숨김
        holder.binding.viewSelectedOverlay.setVisibility(isSelected ? View.VISIBLE : View.GONE);
        holder.binding.ivCheck.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        // 클릭
        holder.itemView.setOnClickListener(v -> {
            int prev = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            // 이전 선택해제
            if (prev >= 0) notifyItemChanged(prev);
            notifyItemChanged(selectedPosition);

            if (listener != null) listener.onImageSelected(imageUrls.get(selectedPosition));
        });
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final RvImageSelectBinding binding;

        public ViewHolder(@NonNull RvImageSelectBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
