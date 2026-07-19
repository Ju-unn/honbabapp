package com.example.honbabapp.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import com.example.honbabapp.R;

/**
 * Toast.makeText()의 드롭인 대체. 앱 브랜드 컬러(오렌지) 배경에 흰 텍스트를 쓰는
 * 커스텀 뷰를 적용한 Toast를 만들어 반환한다. 호출부는 Toast.LENGTH_SHORT/LONG과
 * .show() 체이닝을 그대로 사용할 수 있다.
 */
public final class ToastUtil {

    private ToastUtil() {}

    public static Toast makeText(Context context, CharSequence text, int duration) {
        View view = LayoutInflater.from(context).inflate(R.layout.toast_custom, null);
        TextView textView = view.findViewById(R.id.toast_text);
        textView.setText(text);

        Toast toast = new Toast(context.getApplicationContext());
        toast.setDuration(duration);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 150);
        toast.setView(view);
        return toast;
    }

    public static Toast makeText(Context context, int resId, int duration) {
        return makeText(context, context.getText(resId), duration);
    }
}
