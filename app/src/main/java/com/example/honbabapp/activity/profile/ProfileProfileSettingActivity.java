package com.example.honbabapp.activity.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.AvailabilityData;
import com.example.honbabapp.api.data.ImageUploadData;
import com.example.honbabapp.api.data.request.user.UpdateProfileRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.user.UserProfileResponse;
import com.example.honbabapp.databinding.PageProfileProfilesettingBinding;
import com.example.honbabapp.util.InsetsUtils;
import com.example.honbabapp.util.ToastUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 프로필 설정 Activity
 *
 */
public class ProfileProfileSettingActivity extends AppCompatActivity {

    private static final String TAG = "ProfileProfileSetting";
    private static final String PREF_NAME = "honbabapp_prefs";
    private static final String KEY_USER_ID = "user_id";

    private PageProfileProfilesettingBinding binding;
    private ApiService apiService;

    private String selectedImageUrl = null;
    private boolean isNicknameChecked = false;
    private String originalNickname = "";
    private static final String NICKNAME_PATTERN = "^[가-힣a-zA-Z0-9]+$";

    // ─────────────────────────────────────────────────────────────
    // 갤러리 런처
    // ─────────────────────────────────────────────────────────────

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri imageUri = result.getData().getData();
                            if (imageUri != null) {
                                Glide.with(this)
                                        .load(imageUri)
                                        .placeholder(R.drawable.ic_user)
                                        .circleCrop()
                                        .into(binding.ivProfileAvatar);
                                uploadImageToS3(imageUri);
                            }
                        }
                    }
            );

    // ─────────────────────────────────────────────────────────────
    // 생명주기
    // ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageProfileProfilesettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        InsetsUtils.applyRootInsetsWithIme(binding.getRoot());

        apiService = RetrofitClient.getClient().create(ApiService.class);

        loadCurrentProfile();

        binding.btnBack.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed());

        setupNicknameWatcher();
        binding.btnCheckNickname.setOnClickListener(v -> checkNicknameDuplicate());
        binding.btnChangeAvatar.setOnClickListener(v -> openGallery());
        binding.btnEditProfile.setOnClickListener(v -> saveProfile());

        // CampaignApplyActivity에서 "프로필에서 수정" 클릭 시 SNS 섹션으로 포커스
        if (getIntent().getBooleanExtra("focus_sns", false)) {
            binding.etInstagramId.requestFocus();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 프로필 로드
    // ─────────────────────────────────────────────────────────────

    private void loadCurrentProfile() {
        int userId = getUserId();
        if (userId == -1) return;

        apiService.getUserProfile(userId)
                .enqueue(new Callback<ApiResponse<UserProfileResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<UserProfileResponse>> call,
                                           Response<ApiResponse<UserProfileResponse>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {

                            UserProfileResponse profile = response.body().getData();

                            originalNickname = profile.getUser_nickname();
                            binding.etNickname.setText(profile.getUser_nickname());
                            binding.tvCurrentNickname.setText(profile.getUser_nickname());
                            isNicknameChecked = true;

                            String statusMessage = profile.getUser_status_message();
                            if (statusMessage != null) {
                                binding.etStatusMessage.setText(statusMessage);
                            }

                            Glide.with(ProfileProfileSettingActivity.this)
                                    .load(profile.getUser_profile_image())
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_user)
                                    .into(binding.ivProfileAvatar);

                            // SNS ID prefill
                            String instagramId = profile.getUser_instagram_id();
                            binding.etInstagramId.setText(
                                    instagramId != null ? instagramId : ""
                            );

                            String blogId = profile.getUser_blog_id();
                            binding.etBlogId.setText(
                                    blogId != null ? blogId : ""
                            );
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<UserProfileResponse>> call,
                                          Throwable throwable) {
                        Log.e(TAG, "프로필 로드 실패: " + throwable.getMessage());
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────
    // 프로필 저장
    // ─────────────────────────────────────────────────────────────

    private void saveProfile() {
        int userId = getUserId();
        if (userId == -1) return;

        String nickname      = binding.etNickname.getText().toString().trim();
        String statusMessage = binding.etStatusMessage.getText().toString().trim();
        String instagramId   = binding.etInstagramId.getText().toString().trim();
        String blogId        = binding.etBlogId.getText().toString().trim();

        if (!isNicknameChecked) {
            ToastUtil.makeText(this, "닉네임 중복 확인이 필요합니다", Toast.LENGTH_SHORT).show();
            return;
        }

        if (instagramId.length() > 50) {
            ToastUtil.makeText(this, "인스타그램 ID는 50자 이하로 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }
        if (blogId.length() > 50) {
            ToastUtil.makeText(this, "블로그 ID는 50자 이하로 입력해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUser_nickname(nickname);
        request.setUser_status_message(statusMessage);

        if (selectedImageUrl != null) {
            request.setUser_profile_image(selectedImageUrl);
        }

        request.setUser_instagram_id(instagramId);
        request.setUser_blog_id(blogId);

        binding.btnEditProfile.setEnabled(false);

        apiService.updateProfile(request)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call,
                                           Response<ApiResponse<Void>> response) {
                        binding.btnEditProfile.setEnabled(true);
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {
                            ToastUtil.makeText(ProfileProfileSettingActivity.this,
                                    "프로필이 수정되었습니다", Toast.LENGTH_SHORT).show();

                            setResult(RESULT_OK);
                            finish();
                        } else {
                            String message = response.body() != null
                                    ? response.body().getMessage() : "수정 실패";
                            ToastUtil.makeText(ProfileProfileSettingActivity.this,
                                    message, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable throwable) {
                        binding.btnEditProfile.setEnabled(true);
                        Log.e(TAG, "프로필 수정 실패: " + throwable.getMessage());
                        ToastUtil.makeText(ProfileProfileSettingActivity.this,
                                "네트워크 오류", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────
    // 닉네임 중복확인
    // ─────────────────────────────────────────────────────────────

    private void setupNicknameWatcher() {
        binding.etNickname.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                isNicknameChecked = false;
                binding.tvNicknameCheckResult.setVisibility(View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void checkNicknameDuplicate() {
        String nickname = binding.etNickname.getText().toString().trim();

        if (nickname.isEmpty() || nickname.length() < 2) {
            showNicknameResult(false, "닉네임은 2자 이상 입력해주세요");
            return;
        }
        if (nickname.length() > 10) {
            showNicknameResult(false, "닉네임은 10자 이하로 입력해주세요");
            return;
        }
        if (!nickname.matches(NICKNAME_PATTERN)) {
            showNicknameResult(false, "특수문자는 사용할 수 없습니다");
            return;
        }
        if (nickname.matches("^[0-9]+$")) {
            showNicknameResult(false, "닉네임은 숫자로만 만들 수 없습니다");
            return;
        }
        if (nickname.equals(originalNickname)) {
            isNicknameChecked = true;
            showNicknameResult(true, "현재 닉네임입니다");
            return;
        }

        apiService.checkNickname(nickname)
                .enqueue(new Callback<ApiResponse<AvailabilityData>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<AvailabilityData>> call,
                                           Response<ApiResponse<AvailabilityData>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().isSuccess()) {
                            boolean available = response.body().getData().isAvailable();
                            isNicknameChecked = available;
                            showNicknameResult(available,
                                    available ? "사용 가능한 닉네임입니다" : "이미 사용중인 닉네임입니다");
                        }
                    }
                    @Override
                    public void onFailure(Call<ApiResponse<AvailabilityData>> call,
                                          Throwable throwable) {
                        Log.e(TAG, "닉네임 확인 실패: " + throwable.getMessage());
                    }
                });
    }

    private void showNicknameResult(boolean isAvailable, String message) {
        binding.tvNicknameCheckResult.setVisibility(View.VISIBLE);
        binding.tvNicknameCheckResult.setText(message);
        binding.tvNicknameCheckResult.setTextColor(
                getColor(isAvailable ? R.color.success_green : R.color.error_red)
        );
    }

    // ─────────────────────────────────────────────────────────────
    // 이미지 업로드
    // ─────────────────────────────────────────────────────────────

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private void uploadImageToS3(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            File tempFile = new File(getCacheDir(), "profile_temp.jpg");
            FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, length);
            }
            fileOutputStream.close();
            inputStream.close();

            RequestBody requestBody = RequestBody.create(
                    MediaType.parse("image/jpeg"), tempFile
            );
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
                    "profile_image", tempFile.getName(), requestBody
            );
            RequestBody userIdBody = RequestBody.create(
                    MediaType.parse("text/plain"), String.valueOf(getUserId())
            );

            apiService.uploadProfileImage(imagePart, userIdBody)
                    .enqueue(new Callback<ApiResponse<ImageUploadData>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<ImageUploadData>> call,
                                               Response<ApiResponse<ImageUploadData>> response) {
                            if (response.isSuccessful()
                                    && response.body() != null
                                    && response.body().isSuccess()) {
                                selectedImageUrl = response.body().getData().getProfile_image_url();
                                Log.i(TAG, "이미지 업로드 성공: " + selectedImageUrl);
                            }
                        }
                        @Override
                        public void onFailure(Call<ApiResponse<ImageUploadData>> call,
                                              Throwable throwable) {
                            Log.e(TAG, "이미지 업로드 실패: " + throwable.getMessage());
                        }
                    });
        } catch (Exception exception) {
            Log.e(TAG, "이미지 처리 실패: " + exception.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 유틸
    // ─────────────────────────────────────────────────────────────

    private int getUserId() {
        return getSharedPreferences(PREF_NAME, 0).getInt(KEY_USER_ID, -1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}