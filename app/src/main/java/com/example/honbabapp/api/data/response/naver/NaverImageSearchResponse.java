package com.example.honbabapp.api.data.response.naver;
import java.util.List;

/**
 * 네이버 이미지 검색 API 응답 모델
 * GET https://openapi.naver.com/v1/search/image
 *
 * 응답 예시:
 * {
 *   "lastBuildDate": "...",
 *   "total": 3921,
 *   "start": 1,
 *   "display": 1,
 *   "items": [
 *     {
 *       "title": "스타벅스 신림점",
 *       "link": "https://...원본이미지URL.jpg",
 *       "thumbnail": "https://...썸네일URL.jpg",
 *       "sizeheight": "400",
 *       "sizewidth": "600"
 *     }
 *   ]
 * }
 */
public class NaverImageSearchResponse {
    private int total;
    private int start;
    private int display;
    private List<ImageItem> items;

    public static class ImageItem {
        private String title;
        private String link;
        private String thumbnail;
        private String sizeheight;
        private String sizewidth;

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

        public String getThumbnail() {
            return thumbnail;
        }

        public void setThumbnail(String thumbnail) {
            this.thumbnail = thumbnail;
        }

        public String getSizeheight() {
            return sizeheight;
        }

        public void setSizeheight(String sizeheight) {
            this.sizeheight = sizeheight;
        }

        public String getSizewidth() {
            return sizewidth;
        }

        public void setSizewidth(String sizewidth) {
            this.sizewidth = sizewidth;
        }
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

    public List<ImageItem> getItems() {
        return items;
    }

    public void setItems(List<ImageItem> items) {
        this.items = items;
    }
}
