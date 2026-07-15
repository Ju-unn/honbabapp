package com.example.honbabapp.activity.chat;

import android.content.ContentValues;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.honbabapp.databinding.ActivityFullscreenImageBinding;
import com.example.honbabapp.ui.adapter.FullScreenImagePagerAdapter;
import com.example.honbabapp.util.InsetsUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FullScreenImageActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URLS  = "image_urls";
    public static final String EXTRA_START_INDEX = "start_index";

    private ActivityFullscreenImageBinding binding;
    private List<String> imageUrls;
    private int currentIndex = 0;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler  = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFullscreenImageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        InsetsUtils.applyTopInsetMargin(binding.topBar);

        // ── Extras 파싱 ──
        imageUrls = getIntent().getStringArrayListExtra(EXTRA_IMAGE_URLS);
        if (imageUrls == null) imageUrls = new ArrayList<>();
        currentIndex = getIntent().getIntExtra(EXTRA_START_INDEX, 0);

        // ViewPager2
        FullScreenImagePagerAdapter adapter = new FullScreenImagePagerAdapter(imageUrls);
        binding.viewPager.setAdapter(adapter);
        binding.viewPager.setCurrentItem(currentIndex, false);
        updateIndicator(currentIndex);

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentIndex = position;
                updateIndicator(position);
            }
        });

        // ── 닫기 ──
        binding.btnClose.setOnClickListener(v -> finish());

        // ── 다운로드 ──
        binding.btnDownload.setOnClickListener(v -> downloadCurrentImage());
    }

    private void updateIndicator(int index) {
        if (imageUrls.size() <= 1) {
            binding.tvIndicator.setText("");
        } else {
            binding.tvIndicator.setText((index + 1) + " / " + imageUrls.size());
        }
    }

    /**
     * 현재 페이지 이미지를 MediaStore (Pictures/honbabapp) 에 저장.
     *
     * 날짜 정렬 문제 해결:
     *   - MediaStore DATE_TAKEN/ADDED/MODIFIED 를 "지금"으로 지정
     *   - 원본 JPEG 의 EXIF DateTimeOriginal(과거 촬영일) 도 "지금"으로 덮어씀
     *     → 갤러리가 EXIF 우선 정렬해도 저장 시점 최상단에 노출
     */
    private void downloadCurrentImage() {
        if (imageUrls.isEmpty()) return;
        String url = imageUrls.get(currentIndex);
        Toast.makeText(this, "저장 중…", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            try {
                long now = System.currentTimeMillis();

                // 1) HTTP 다운로드
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(15_000);
                conn.connect();
                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    throw new IOException("HTTP " + conn.getResponseCode());
                }

                // 2) 파일명 — 현재시간 기반
                String fileName = "honbab_" + now + ".jpg";

                // 3) MediaStore insert ( 강제 지정 )
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/honbabapp");
                values.put(MediaStore.Images.Media.IS_PENDING, 1);
                values.put(MediaStore.Images.Media.DATE_ADDED, now / 1000);     // 초
                values.put(MediaStore.Images.Media.DATE_TAKEN, now);            // 밀리초
                values.put(MediaStore.Images.Media.DATE_MODIFIED, now / 1000);  // 초

                Uri imageUri = getContentResolver()
                        .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (imageUri == null) throw new IOException("MediaStore insert 실패");

                // 4) 스트림 복사
                try (InputStream in = conn.getInputStream();
                     OutputStream out = getContentResolver().openOutputStream(imageUri)) {
                    if (out == null) throw new IOException("OutputStream null");
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = in.read(buf)) != -1) out.write(buf, 0, len);
                }

                // 4-2) EXIF 촬영일자를 "지금"으로 덮어쓰기
                try {
                    SimpleDateFormat exifFormat =
                            new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US);
                    String nowStr = exifFormat.format(new Date(now));

                    ParcelFileDescriptor pfd =
                            getContentResolver().openFileDescriptor(imageUri, "rw");
                    if (pfd != null) {
                        ExifInterface exif = new ExifInterface(pfd.getFileDescriptor());
                        exif.setAttribute(ExifInterface.TAG_DATETIME, nowStr);
                        exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, nowStr);
                        exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, nowStr);
                        exif.saveAttributes();
                        pfd.close();
                    }
                } catch (Exception exifErr) {
                    // EXIF 없는 포맷(PNG 등)이면 무시 — 저장 자체는 성공
                    Log.w("DOWNLOAD", "EXIF 갱신 스킵: " + exifErr.getMessage());
                }

                // 5) IS_PENDING 해제
                values.clear();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(imageUri, values, null, null);

                mainHandler.post(() ->
                        Toast.makeText(this, "갤러리에 저장되었습니다", Toast.LENGTH_SHORT).show());

            } catch (Exception e) {
                mainHandler.post(() ->
                        Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}