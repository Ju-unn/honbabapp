package com.example.honbabapp.api.data.request;

/**
 * 소셜 로그인 요청 모델
 *
 * provider: "kakao" 또는 "google"
 * accessToken: 카카오는 access_token, 구글은 id_token을 여기 담아서 보냄
 *              (서버 social_login.php가 access_token/id_token 둘 다 같은 필드명으로 받음)
 */
public class SocialLoginRequest {
    private String provider;
    private String access_token;
    private String id_token;

    public SocialLoginRequest(String provider, String accessToken, String idToken) {
        this.provider = provider;
        this.access_token = accessToken;
        this.id_token = idToken;
    }

    public String getProvider() {
        return provider;
    }

    public String getAccess_token() {
        return access_token;
    }

    public String getId_token() {
        return id_token;
    }
}