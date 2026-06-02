package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.databinding.RvFullscreenImageBinding;

import java.util.List;

public class FullScreenImagePagerAdapter
        extends RecyclerView.Adapter<FullScreenImagePagerAdapter.ImageViewHolder> {

    private final List<String> urls;

    public FullScreenImagePagerAdapter(List<String> urls) {
        this.urls = urls;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RvFullscreenImageBinding binding = RvFullscreenImageBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ImageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Glide.with(holder.binding.ivFullscreen.getContext())
                .load(urls.get(position))
                .placeholder(R.drawable.message_image_background)
                .error(R.drawable.message_image_background)
                .into(holder.binding.ivFullscreen);
    }

    @Override
    public int getItemCount() { return urls.size(); }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        final RvFullscreenImageBinding binding;

        ImageViewHolder(RvFullscreenImageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}