package com.example.honbabapp.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.ui.adapter.MessageAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * 채팅 다중 이미지(2~10장) 그리드 — 카톡식 행 분배.
 *
 * 행마다 열 수가 달라 균일 GridLayoutManager 로는 안 되므로
 * {@link #onMeasure}/{@link #onLayout} 에서 직접 행을 배치한다.
 *
 * 행 분배 규칙(= {@link #rowsFor}):
 *   N ≤ 3 → 한 행에 N장
 *   N ≥ 4 → rowCount=ceil(N/3)개 행, 앞 (N%rowCount)개 행이 baseColumns+1(=많은 쪽), 나머지 baseColumns.
 *   (4=2/2, 5=3/2, 6=3/3, 7=3/2/2, 8=3/3/2, 9=3/3/3, 10=3/3/2/2)
 * 각 칸은 행 안에서 동일 너비의 정사각(centerCrop), 간격 일정.
 */
public class ChatImageGridView extends ViewGroup {

    private static final int MAX = 10;

    private final int gap; // 칸 사이 간격(px)
    private List<Integer> rows = new ArrayList<>();

    public ChatImageGridView(Context context) {
        this(context, null);
    }

    public ChatImageGridView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        gap = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
    }

    /** 행별 열 수 목록. n=1~10 에서 규칙과 정확히 일치하도록 계산. */
    public static List<Integer> rowsFor(int imageCount) {
        List<Integer> result = new ArrayList<>();
        if (imageCount <= 0) return result;
        if (imageCount <= 3) {
            result.add(imageCount);
            return result;
        }

        // 필요한 행 계산
        int rowCount = (imageCount + 2) / 3;        // ceil(imageCount/3)

        int baseColumns = imageCount / rowCount;
        int remainder = imageCount % rowCount;
        for (int i = 0; i < rowCount; i++) {
            result.add(i < remainder ? baseColumns + 1 : baseColumns);
        }
        return result;
    }

    /** 이미지 URL 목록으로 그리드를 다시 구성. 매 bind 마다 호출(재사용 잔상 없음). */
    public void setImages(@NonNull List<String> urls, MessageAdapter.OnImageClickListener listener) {
        removeAllViews();
        int imageCount = Math.min(urls.size(), MAX);
        rows = rowsFor(imageCount);
        for (int i = 0; i < imageCount; i++) {
            final int index = i;
            ImageView cell = new ImageView(getContext());
            cell.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Glide.with(getContext())
                    .load(urls.get(i))
                    .centerCrop()
                    .placeholder(R.drawable.message_image_background)
                    .into(cell);
            cell.setOnClickListener(v -> {
                if (listener != null) listener.onImageClick(urls, index);
            });
            addView(cell);
        }
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int totalHeight = 0;
        int child = 0;
        for (int ri = 0; ri < rows.size(); ri++) {
            int columns = rows.get(ri);
            int cellW = (width - (columns - 1) * gap) / columns;
            int cellH = cellW; // 정사각
            for (int ci = 0; ci < columns; ci++) {
                getChildAt(child++).measure(
                        MeasureSpec.makeMeasureSpec(cellW, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(cellH, MeasureSpec.EXACTLY));
            }
            if (ri > 0) totalHeight += gap;
            totalHeight += cellH;
        }
        setMeasuredDimension(width, totalHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = getWidth();
        int y = 0;
        int child = 0;
        for (int ri = 0; ri < rows.size(); ri++) {
            int columns = rows.get(ri);
            int cellW = (width - (columns - 1) * gap) / columns;
            int cellH = cellW;
            int x = 0;
            for (int ci = 0; ci < columns; ci++) {
                View v = getChildAt(child++);
                v.layout(x, y, x + cellW, y + cellH);
                x += cellW + gap;
            }
            y += cellH + gap;
        }
    }
}
