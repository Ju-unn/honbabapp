package com.example.honbabapp.util;

import android.view.View;

import com.google.android.material.snackbar.Snackbar;

/**
 * 실패/재시도 가능한 상황에서 Toast 대신 쓰는 스낵바 헬퍼.
 * Toast는 스쳐가서 놓치기 쉽고 다음 행동이 없다 → 지속 표시 + "다시 시도" 액션 제공.
 * 단순 완료 알림은 그대로 Toast 유지(GX② 결정).
 */
public final class SnackbarUtil {

    private SnackbarUtil() {}

    /** 실패 + 재시도. 액션을 누르면 onRetry 실행. */
    public static void showRetry(View root, CharSequence msg, Runnable onRetry) {
        Snackbar.make(root, msg, Snackbar.LENGTH_LONG)
                .setAction("다시 시도", v -> onRetry.run())
                .show();
    }
}
