package com.example.honbabapp.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;

/**
 * 업로드 전 이미지 다운스케일 + JPEG 재압축.
 * 큰 원본을 통째로 디코드하면 OOM 나므로 inSampleSize로 다운샘플 후 디코드한다.
 * MediaType("image/jpeg")는 라벨일 뿐 압축이 아니므로, 실제 크기 축소는 여기서 한다.
 */
public final class ImageCompressUtil {

    private ImageCompressUtil() {}

    /** 카드 썸네일/프로필/채팅에 충분한 최대 변 길이. 필요 시 조정. */
    private static final int MAX_DIMENSION = 1280;
    private static final int JPEG_QUALITY = 85;

    public static byte[] compress(byte[] original) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(original, 0, original.length, bounds);

        int sample = 1;
        while (bounds.outWidth / sample > MAX_DIMENSION
                || bounds.outHeight / sample > MAX_DIMENSION) {
            sample *= 2;
        }
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap bitmap = BitmapFactory.decodeByteArray(original, 0, original.length, opts);
        if (bitmap == null) return original; // 디코드 실패 시 원본 그대로 (서버가 재검증)

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out);
        bitmap.recycle();
        return out.toByteArray();
    }
}
