package com.example.honbabapp.util;

import android.view.View;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Android 15(API 35, targetSdk 35)부터 edge-to-edge 표시가 강제되어 더 이상
 * opt-out할 수 없다. setDecorFitsSystemWindows(false)를 API 33~34에서도 명시적으로
 * 호출해 두면(각 Activity의 onCreate에서) 시스템이 자동으로 콘텐츠를 시스템 바
 * 안쪽으로 맞춰주지 않으므로, 여기서 상태바/내비게이션바 인셋을 직접 반영한다.
 */
public final class InsetsUtils {

    private InsetsUtils() {}

    /** 일반 화면: 루트 뷰에 상태바/내비게이션바 인셋만큼 padding을 적용한다. */
    public static void applyRootInsets(View root) {
        final int initialPaddingLeft = root.getPaddingLeft();
        final int initialPaddingTop = root.getPaddingTop();
        final int initialPaddingRight = root.getPaddingRight();
        final int initialPaddingBottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    initialPaddingLeft + bars.left,
                    initialPaddingTop + bars.top,
                    initialPaddingRight + bars.right,
                    initialPaddingBottom + bars.bottom);
            return insets;
        });
    }

    /**
     * 하단에 자체적으로 내비게이션바 인셋을 처리하는 자식 뷰(예: BottomNavigationView는
     * 기본적으로 자신의 하단에 내비게이션바 인셋만큼 padding을 추가함)가 있는 화면 전용.
     * 루트에 bottom까지 패딩을 주면 그 자식의 자체 인셋 처리와 겹쳐 이중으로 밀려
     * 올라가 보이므로, 루트는 top/left/right만 담당하고 bottom은 자식에게 맡긴다.
     */
    public static void applyRootInsetsExceptBottom(View root) {
        final int initialPaddingLeft = root.getPaddingLeft();
        final int initialPaddingTop = root.getPaddingTop();
        final int initialPaddingRight = root.getPaddingRight();
        final int initialPaddingBottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                    initialPaddingLeft + bars.left,
                    initialPaddingTop + bars.top,
                    initialPaddingRight + bars.right,
                    initialPaddingBottom);
            return insets;
        });
    }

    /**
     * 하단에 메시지 입력창처럼 키보드 위로 따라 올라와야 하는 뷰가 있는 화면 전용.
     * setDecorFitsSystemWindows(false) 상태에서는 시스템이 더 이상 창을 자동으로
     * 리사이즈하지 않으므로, IME(키보드) 인셋을 직접 읽어 루트 하단 padding에 반영해야
     * 키보드가 올라올 때 입력창도 함께 밀려 올라간다.
     */
    public static void applyRootInsetsWithIme(View root) {
        final int initialPaddingLeft = root.getPaddingLeft();
        final int initialPaddingTop = root.getPaddingTop();
        final int initialPaddingRight = root.getPaddingRight();
        final int initialPaddingBottom = root.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomInset = Math.max(bars.bottom, ime.bottom);
            v.setPadding(
                    initialPaddingLeft + bars.left,
                    initialPaddingTop + bars.top,
                    initialPaddingRight + bars.right,
                    initialPaddingBottom + bottomInset);
            return insets;
        });
    }

    /** 풀블리드 화면: 배경/콘텐츠는 유지하고 특정 뷰에만 상태바 인셋만큼 top margin을 더한다. */
    public static void applyTopInsetMargin(View target) {
        final int initialMarginTop = ((ViewGroup.MarginLayoutParams) target.getLayoutParams()).topMargin;

        ViewCompat.setOnApplyWindowInsetsListener(target, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.topMargin = initialMarginTop + bars.top;
            v.setLayoutParams(params);
            return insets;
        });
    }

    /** 풀블리드 화면: 배경/콘텐츠는 유지하고 특정 뷰에만 내비게이션바 인셋만큼 bottom margin을 더한다. */
    public static void applyBottomInsetMargin(View target) {
        final int initialMarginBottom = ((ViewGroup.MarginLayoutParams) target.getLayoutParams()).bottomMargin;

        ViewCompat.setOnApplyWindowInsetsListener(target, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.bottomMargin = initialMarginBottom + bars.bottom;
            v.setLayoutParams(params);
            return insets;
        });
    }
}
