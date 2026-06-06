package com.example.honbabapp.ui.adapter;

import androidx.recyclerview.widget.RecyclerView;

import com.example.honbabapp.api.data.chat.ChatPhotoData;

import java.util.ArrayList;
import java.util.List;


public abstract class ChatPhotoBaseAdapter<ViewHolderType extends RecyclerView.ViewHolder>
        extends RecyclerView.Adapter<ViewHolderType> {

    /** 사진 셀 클릭 콜백 — 클릭한 위치와 전체 아이템 목록을 함께 넘긴다(풀스크린 뷰어용). */
    public interface OnPhotoClickListener {
        void onPhotoClick(List<ChatPhotoData> photoItems, int position);
    }

    protected final List<ChatPhotoData> photoItems = new ArrayList<>();
    protected OnPhotoClickListener photoClickListener;

    public void setOnPhotoClickListener(OnPhotoClickListener listener) {
        this.photoClickListener = listener;
    }

    /** 전체 목록 교체 후 다시 그린다. */
    public void setItems(List<ChatPhotoData> newItems) {
        photoItems.clear();
        if (newItems != null) {
            photoItems.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    /** 페이징 추가 로드 — 뒤에 이어 붙이고 변경 구간만 갱신한다. */
    public void addItems(List<ChatPhotoData> moreItems) {
        if (moreItems == null || moreItems.isEmpty()) {
            return;
        }
        int insertStart = photoItems.size();
        photoItems.addAll(moreItems);
        notifyItemRangeInserted(insertStart, moreItems.size());
    }

    /** 현재 보유 중인 사진 개수(다음 페이지 요청 판단용). */
    public int getCurrentCount() {
        return photoItems.size();
    }

    @Override
    public int getItemCount() {
        return photoItems.size();
    }

    /** 하위 어댑터의 ViewHolder 가 클릭 시 호출. null 콜백 가드 포함. */
    protected void dispatchPhotoClick(int position) {
        if (photoClickListener != null) {
            photoClickListener.onPhotoClick(photoItems, position);
        }
    }
}
