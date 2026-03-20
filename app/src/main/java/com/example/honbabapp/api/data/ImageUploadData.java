package com.example.honbabapp.api.data;

/**
 * 이미지 업로드 응답 데이터
 *
 * PHP upload_profile_image.php 응답:
 * {
 *   "success": true,
 *   "message": "이미지 업로드 완료",
 *   "data": {
 *     "profile_image_url": "https://honbabapp-images.s3.ap-northeast-2.amazonaws.com/profiles/...",
 *     "file_name": "user_0_1703001234_abc123.jpg",
 *     "file_size": 245678,
 *     "mime_type": "image/jpeg"
 *   }
 * }
 */

public class ImageUploadData {
    private String profile_image_url;

    // 모임 이미지
    private String image_url;
    private String file_name;
    private long file_size;
    private String mime_type;

    public ImageUploadData() {

    }

    public ImageUploadData(String profile_image_url, String file_name, long file_size, String mime_type) {
        this.profile_image_url = profile_image_url;
        this.file_name = file_name;
        this.file_size = file_size;
        this.mime_type = mime_type;
    }

    public String getProfile_image_url() {
        return profile_image_url;
    }

    public void setProfile_image_url(String profile_image_url) {
        this.profile_image_url = profile_image_url;
    }

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }

    public String getFile_name() {
        return file_name;
    }

    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }

    public long getFile_size() {
        return file_size;
    }

    public void setFile_size(long file_size) {
        this.file_size = file_size;
    }

    public String getMime_type() {
        return mime_type;
    }

    public void setMime_type(String mime_type) {
        this.mime_type = mime_type;
    }
}
