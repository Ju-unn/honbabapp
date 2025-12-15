package com.example.honbabapp.api.data.response;
    public class ApiResponse<T> {
        /**
         * API 공통 응답 클래스
         * PHP에서 보내는 모든 응답의 기본 구조
         *
         * 응답 예시:
         * {
         *   "success": true,
         *   "message": "성공 메시지",
         *   "data": { ... }
         * }
         */

        private boolean success;
        private String message;
        private T data;

        public ApiResponse() {
        }

        public ApiResponse(boolean success, String message, T data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        // Getters and Setters
        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }
}
