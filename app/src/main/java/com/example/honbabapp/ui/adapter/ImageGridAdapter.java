package com.example.honbabapp.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.databinding.RvImageGridCellBinding;

import java.util.List;

/**
 * 채팅 다중 이미지 그리드 어댑터
 *
 * 표시 규칙:
 *   2장 → 1행 2열
 *   3장 → 1행 3열
 *   4장 이상 → 2x2 (최대 4셀 표시, 4번째에 +N 오버레이)
 *
 * GridLayoutManager spanCount 는 외부에서 attachTo() 로 자동 세팅.
 */

public class ImageGridAdapter extends RecyclerView.Adapter<ImageGridAdapter.CellViewHolder> {

    public interface OnImageClickListener {
        void onImageClick(List<String> urls , int index);
    }

    private final List<String> allUrls; // 전체 url 개수 표시
    private final List<String> cellUrls; // 실제 표시할 url (최대 4개)
    private final OnImageClickListener listener;

    public ImageGridAdapter(List<String> urls, OnImageClickListener listener) {
        this.allUrls = urls;
        this.listener = listener;
        // 4장 초과하면 앞 4개만 표시
        this.cellUrls = urls.size() > 4 ? urls.subList(0, 4) : urls;
    }

    /**
     * RecyclerView 에 GridLayoutManager 를 자동 세팅하고 어댑터 등록까지 처리
     * 어댑터 생성 직후 이 메서드 1번 호출로 완료
     */
    public void attachTo(RecyclerView rv) {
        int span = cellUrls.size() == 2 ? 2
                : cellUrls.size() == 3 ? 3
                : 2; // 4장 이상은 2x2

        rv.setLayoutManager(new GridLayoutManager(rv.getContext() , span));
        rv.setAdapter(this);
    }

    @NonNull
    @Override
    public ImageGridAdapter.CellViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        RvImageGridCellBinding binding =
                RvImageGridCellBinding.inflate(inflater, parent, false);

        return new CellViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageGridAdapter.CellViewHolder holder, int position) {
        String url = cellUrls.get(position);
        boolean isLast = (position == 3) && (allUrls.size() > 4 );
        int remaining = allUrls.size() - 3 ; // 3장까지 보여주고 4번째에 나머지 사진 +n개 표시 용도

        Glide.with(holder.binding.ivCell.getContext())
                .load(url)
                .centerCrop()
                .placeholder(R.drawable.message_image_background)
                .into(holder.binding.ivCell);

        // +N 오버레이
        if (isLast) {
            holder.binding.overlayMore.setVisibility(View.VISIBLE);
            holder.binding.tvMore.setText("+" + remaining);
        } else {
            holder.binding.overlayMore.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onImageClick(allUrls, position);
        });
    }

    @Override
    public int getItemCount() {
        return cellUrls.size();
    }

    static class CellViewHolder extends RecyclerView.ViewHolder {

        RvImageGridCellBinding binding;

        public CellViewHolder(RvImageGridCellBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
