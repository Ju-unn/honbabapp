package com.example.honbabapp.activity.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.PixelCopy;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.honbabapp.R;
import com.example.honbabapp.api.ApiService;
import com.example.honbabapp.api.RetrofitClient;
import com.example.honbabapp.api.data.ImageUploadData;
import com.example.honbabapp.api.data.request.UpdateProfileRequest;
import com.example.honbabapp.api.data.response.ApiResponse;
import com.example.honbabapp.api.data.response.UserProfileResponse;
import com.example.honbabapp.databinding.PageProfileProfilesettingBinding;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileProfileSettingActivity extends AppCompatActivity {

    private static final String TAG = "ProfileProfileSetting";
    private PageProfileProfilesettingBinding binding;
    private ApiService apiService;

    private static final String PREF_NAME = "honbabapp_prefs";
    private static final String KEY_USER_ID = "user_id";

    // 선택된 이미지 S3 URL (null이면 이미지 변경 안 한 것)
    private String selectedImageUrl = null;

    // 갤러리에서 이미지 선택 결과 처리
    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri imageUri = result.getData().getData();
                            if (imageUri != null) {
                                // 미리보기 업데이트
                                Glide.with(this)
                                        .load(imageUri)
                                        .placeholder(R.drawable.ic_user)
                                        .circleCrop()
                                        .into(binding.ivProfileAvatar);

                                // S3 업로드
                                uploadImageToS3(imageUri);
                            }
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = PageProfileProfilesettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        apiService = RetrofitClient.getClient().create(ApiService.class);

        // 현재 프로필 데이터 로드
        loadCurrentProfile();

        // 뒤로가기
        binding.btnBack.setOnClickListener(v ->
                getOnBackPressedDispatcher().onBackPressed()
        );

        // 카메라 버튼 -> 갤러리 열기
        binding.btnChangeAvatar.setOnClickListener(v -> openGalley());

        // 저장 버튼
        binding.btnEditProfile.setOnClickListener(v -> saveProfile());

    }

    // 현재 프로필 불러와서 EditText에 채우기
    private void loadCurrentProfile() {
        int userId = getUserId();
        if (userId == -1) return;

        apiService.getUserProfile(userId)
                .enqueue(new Callback<ApiResponse<UserProfileResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<UserProfileResponse>> call, Response<ApiResponse<UserProfileResponse>> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                            UserProfileResponse profile = response.body().getData();

                            // 기존 데이터 EditText에 채우기
                            binding.etNickname.setText(profile.getUser_nickname());
                            binding.tvCurrentNickname.setText(profile.getUser_nickname());

                            String status = profile.getUser_status_message();
                            if (status != null) {
                                binding.etStatusMessage.setText(status);
                            }

                            // 기존 프로필 이미지
                            String imageUrl = profile.getUser_profile_image();

                            Glide.with(ProfileProfileSettingActivity.this)
                                    .load(imageUrl)
                                    .circleCrop()
                                    .placeholder(R.drawable.ic_user)
                                    .into(binding.ivProfileAvatar);

                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<UserProfileResponse>> call, Throwable t) {
                        Log.e(TAG, "프로필 로드 실패: " + t.getMessage());
                    }
                });
    }


    // 갤러리 열기
    private void openGalley() {
        Intent intent = new Intent(Intent.ACTION_PICK ,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    // 이미지 S3 업로드
    private void uploadImageToS3(Uri imageUri) {

        try {

            // Uri을 File 변환 (Multipar 전송에 필요)
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            File tempFile = new File(getCacheDir(), "profile_temp.jpg");
            FileOutputStream fos = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                fos.write(buffer , 0 , len);
            }
            fos.close();
            inputStream.close();

            // Multipart 구성
            RequestBody requestFile = RequestBody.create(
                    MediaType.parse("image/jpeg" ), tempFile);

            // 서버가 받을 key  , file 이름 , 진짜 file
            MultipartBody.Part imagePart = MultipartBody.Part.createFormData(
                    "image" , tempFile.getName() , requestFile
            );

            RequestBody userIdBody = RequestBody.create(
                    MediaType.parse("text/plain"), String.valueOf(getUserId())
            );

            // 업로드 api 호출
            apiService.uploadProfileImage(imagePart ,userIdBody)
                    .enqueue(new Callback<ApiResponse<ImageUploadData>>() {
                        @Override
                        public void onResponse(Call<ApiResponse<ImageUploadData>> call, Response<ApiResponse<ImageUploadData>> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                // S3 Url 저장 (저장 버튼 누를 때 사용)
                                selectedImageUrl = response.body().getData().getProfile_image_url();
                                Log.d(TAG, "이미지 업로드 성공: " + selectedImageUrl);
                                Toast.makeText(ProfileProfileSettingActivity.this, "이미지 업로드 완료", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ApiResponse<ImageUploadData>> call, Throwable t) {
                            Log.e(TAG, "이미지 업로드 실패: " + t.getMessage());
                            Toast.makeText(ProfileProfileSettingActivity.this, "이미지 업로드 실패", Toast.LENGTH_SHORT).show();
                        }
                    }
            );


        } catch (Exception e) {
            Log.e(TAG, "이미지 처리 오류: " + e.getMessage());
        }
    }

    // 저장 버튼 클릭
    private void saveProfile() {
        int userId = getUserId();
        if (userId == -1) return;

        String nickname = binding.etNickname.getText().toString().trim();
        String statusMessage = binding.etStatusMessage.getText().toString().trim();

        // 닉네임 길이 체크
        // TODO : 닉네임에 특수문자 X
        // TODO : 중복체크 마우스 커서 이동시 중복체크하기
        if (nickname.isEmpty() || nickname.length() < 2) {
            binding.etNickname.setError("닉네임은 2자 이상 입력해주세요");
            return;
        } else if (nickname.length() > 11) {
            binding.etNickname.setError("닉네임은 10자 이하로 입력해주세요");
            return;
        }

        // Request 입력된 값만
        UpdateProfileRequest request = new UpdateProfileRequest(userId);
        request.setUser_nickname(nickname);
        request.setUser_status_message(statusMessage);
        if (selectedImageUrl != null) {
            request.setUser_profile_image(selectedImageUrl);
        }

        binding.btnEditProfile.setEnabled(false);

        apiService.updateProfile(request)
                .enqueue(new Callback<ApiResponse<Void>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                        binding.btnEditProfile.setEnabled(true);
                        if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {

                            Toast.makeText(ProfileProfileSettingActivity.this,
                                    "프로필이 수정되었습니다", Toast.LENGTH_SHORT).show();
                            finish(); // 뒤로가기 → ProfileFragment.onResume()에서 자동 갱신

                        } else {
                            String msg = response.body() != null
                                    ? response.body().getMessage() : "수정 실패";
                            Toast.makeText(ProfileProfileSettingActivity.this,
                                    msg, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                        binding.btnEditProfile.setEnabled(true);
                        Log.e(TAG, "프로필 수정 실패: " + t.getMessage());
                        Toast.makeText(ProfileProfileSettingActivity.this,
                                "네트워크 오류", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private int getUserId() {
        return getSharedPreferences(PREF_NAME, 0).getInt(KEY_USER_ID, -1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }


}