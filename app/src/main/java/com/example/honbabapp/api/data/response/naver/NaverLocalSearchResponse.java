package com.example.honbabapp.api.data.response.naver;

import java.util.List;

/**
 * Naver Local Search API 응답 모델
 *
 */
public class NaverLocalSearchResponse {

    private String lastBuildDate; // 검색 결과 생성 시간
    private int total; // 총 검색 결과 개수
    private int start; // 검색 시작 위치
    private int display; //  한번에 표시할 검색 결과 개수
    private List<LocalItem> items;

    public String getLastBuildDate() {
        return lastBuildDate;
    }

    public void setLastBuildDate(String lastBuildDate) {
        this.lastBuildDate = lastBuildDate;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getDisplay() {
        return display;
    }

    public void setDisplay(int display) {
        this.display = display;
    }

    public List<LocalItem> getItems() {
        return items;
    }

    public void setItems(List<LocalItem> items) {
        this.items = items;
    }

    /**
     * 검색 결과 개별 아이템
     */
    public static class LocalItem {
        private String title;        // 업체명 (HTML 태그 포함 가능: <b>태그</b>)
        private String link;         // 네이버 상세 정보 URL
        private String category;     // 카테고리 (예: "음식점>한식>삼겹살,고기요리")
        private String description;  // 설명
        private String telephone;    // 전화번호
        private String address;      // 지번 주소
        private String roadAddress;  // 도로명 주소
        private String mapx;         // X 좌표 (경도, 소수점 없음: "1269291" → 126.9291)
        private String mapy;         // Y 좌표 (위도, 소수점 없음: "374844" → 37.4844)

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getTelephone() {
            return telephone;
        }

        public void setTelephone(String telephone) {
            this.telephone = telephone;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getRoadAddress() {
            return roadAddress;
        }

        public void setRoadAddress(String roadAddress) {
            this.roadAddress = roadAddress;
        }

        public String getMapx() {
            return mapx;
        }

        public void setMapx(String mapx) {
            this.mapx = mapx;
        }

        public String getMapy() {
            return mapy;
        }

        public void setMapy(String mapy) {
            this.mapy = mapy;
        }

        /**
         * HTML 태그 제거한 제목 반환
         */
        public String getCleanTitle() {
            if (title == null) return "";
            return title.replaceAll("<[^>]*>", "");
        }

        /**
         * X 좌표를 실제 경도 값으로 변환
         * "1269291" → "126.9291"
         */
        public String getMapxDouble() {
            if (mapx == null || mapx.isEmpty()) return "0.0";
            try {
                double value = Double.parseDouble(mapx) / 10000000.0;
                return String.valueOf(value);
            } catch (NumberFormatException e) {
                return "0.0";
            }
        }

        /**
         * Y 좌표를 실제 위도 값으로 변환
         * "374844" → "37.4844"
         */
        public String getMapyDouble() {
            if (mapy == null || mapy.isEmpty()) return "0.0";
            try {
                double value = Double.parseDouble(mapy) / 10000000.0;
                return String.valueOf(value);
            } catch (NumberFormatException e) {
                return "0.0";
            }
        }

        /**
         * 시/도 추출 (address 또는 roadAddress에서)
         */
        public String getSido() {
            String addr = roadAddress != null ? roadAddress : address;
            if (addr == null) return "";

            String[] parts = addr.split(" ");
            if (parts.length >= 1) {
                return parts[0];  // "서울특별시"
            }
            return "";
        }

        /**
         * 시군구 추출 (address 또는 roadAddress에서)
         * "서울특별시 관악구 신림동" → "관악구"
         */
        public String getSigungu() {
            String addr = roadAddress != null ? roadAddress : address;
            if (addr == null) return "";

            String[] parts = addr.split(" ");
            if (parts.length >= 2) {
                return parts[1];  // "관악구"
            }
            return "";
        }

        /**
         * 동 추출 (address에서)
         * "서울특별시 관악구 신림동 108-00" → "신림동"
         */
        public String getDong() {
            if (address == null) return "";

            String[] parts = address.split(" ");
            if (parts.length >= 3) {
                String dong = parts[2];
                // 숫자 제거 (예: "신림동108-00" → "신림동")
                return dong.replaceAll("[0-9\\-]", "");
            }
            return "";
        }

    }
}