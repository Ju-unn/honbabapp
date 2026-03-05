package com.example.honbabapp.api.data.response.naver;


import java.util.List;

/**
 * Naver Reverse Geocoding API 응답 모델
 *
 * API 응답 JSON 구조:
 * {
 *   "status": { "code": 0 },
 *   "results": [
 *     {
 *       "region": {
 *         "area2": { "name": "관악구" },
 *         "area3": { "name": "신림동" }
 *       }
 *     }
 *   ]
 * }
 */

public class NaverReverseGeoResponse {

    public Status status;
    public List<Result> results;

    public static class Status {
        public int code;
    }

    public static class Result {
        public Region region;
    }

    public static class Region {
        public Area area2; // 시/구
        public Area area3; // 동
    }

    public static class Area {
        public String name;
    }

    public String getFormattedAddress() {
        if (results == null || results.isEmpty())
            return null;

        Region region = results.get(0).region;
        if (region == null) return null;

        String area2 = (region.area2 != null) ? region.area2.name : "";
        String area3 = (region.area3 != null) ? region.area3.name : "";

        if (!area2.isEmpty() && !area3.isEmpty()) {
            return area2 + " " + area3;
        } else if (!area2.isEmpty()) {
            return area2;
        } else {
            return area3;
        }
    }
}
