package com.example.honbabapp.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 대한민국 시/도 → 시/군/구 매핑 데이터 유틸
 */
public final class KoreaRegion {

    // "전체" 옵션 — 시/도/구 스피너 첫 항목으로 사용
    public static final String ALL = "전체";

    private static final Map<String, List<String>> REGION_MAP = buildRegionMap();

    private KoreaRegion() {
        // 인스턴스화 방지
    }

    /**
     * 시/도 목록 반환 (첫 항목은 "전체")
     */
    public static List<String> getSidoList() {
        List<String> result = new ArrayList<>();
        result.add(ALL);
        result.addAll(REGION_MAP.keySet());
        return result;
    }

    /**
     * 선택된 시/도에 속한 구/군 목록 반환 (첫 항목은 "전체")
     * sido가 null이거나 "전체"이거나 매핑에 없으면 "전체"만 담긴 리스트 반환
     */
    public static List<String> getSigunguList(String sido) {
        List<String> result = new ArrayList<>();
        result.add(ALL);

        if (sido == null || ALL.equals(sido)) {
            return result;
        }

        List<String> guList = REGION_MAP.get(sido);
        if (guList != null) {
            result.addAll(guList);
        }

        return result;
    }

    private static Map<String, List<String>> buildRegionMap() {
        Map<String, List<String>> map = new LinkedHashMap<>();

        map.put("서울특별시", java.util.Arrays.asList(
                "강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구",
                "금천구", "노원구", "도봉구", "동대문구", "동작구", "마포구", "서대문구",
                "서초구", "성동구", "성북구", "송파구", "양천구", "영등포구", "용산구",
                "은평구", "종로구", "중구", "중랑구"
        ));

        map.put("경기도", java.util.Arrays.asList(
                "수원시", "성남시", "고양시", "용인시", "부천시", "안산시", "안양시",
                "남양주시", "화성시", "평택시", "의정부시", "시흥시", "파주시", "광명시",
                "김포시", "군포시", "광주시", "이천시", "양주시", "오산시", "구리시",
                "안성시", "포천시", "의왕시", "하남시", "여주시", "동두천시", "과천시",
                "양평군", "가평군", "연천군"
        ));

        map.put("인천광역시", java.util.Arrays.asList(
                "중구", "동구", "미추홀구", "연수구", "남동구", "부평구", "계양구",
                "서구", "강화군", "옹진군"
        ));

        map.put("부산광역시", java.util.Arrays.asList(
                "중구", "서구", "동구", "영도구", "부산진구", "동래구", "남구", "북구",
                "해운대구", "사하구", "금정구", "강서구", "연제구", "수영구", "사상구",
                "기장군"
        ));

        map.put("대구광역시", java.util.Arrays.asList(
                "중구", "동구", "서구", "남구", "북구", "수성구", "달서구", "달성군"
        ));

        map.put("광주광역시", java.util.Arrays.asList(
                "동구", "서구", "남구", "북구", "광산구"
        ));

        map.put("대전광역시", java.util.Arrays.asList(
                "동구", "중구", "서구", "유성구", "대덕구"
        ));

        map.put("울산광역시", java.util.Arrays.asList(
                "중구", "남구", "동구", "북구", "울주군"
        ));

        map.put("세종특별자치시", java.util.Arrays.asList(
                "세종시"
        ));

        map.put("강원특별자치도", java.util.Arrays.asList(
                "춘천시", "원주시", "강릉시", "동해시", "태백시", "속초시", "삼척시",
                "홍천군", "횡성군", "영월군", "평창군", "정선군", "철원군", "화천군",
                "양구군", "인제군", "고성군", "양양군"
        ));

        map.put("충청북도", java.util.Arrays.asList(
                "청주시", "충주시", "제천시", "보은군", "옥천군", "영동군", "증평군",
                "진천군", "괴산군", "음성군", "단양군"
        ));

        map.put("충청남도", java.util.Arrays.asList(
                "천안시", "공주시", "보령시", "아산시", "서산시", "논산시", "계룡시",
                "당진시", "금산군", "부여군", "서천군", "청양군", "홍성군", "예산군",
                "태안군"
        ));

        map.put("전북특별자치도", java.util.Arrays.asList(
                "전주시", "군산시", "익산시", "정읍시", "남원시", "김제시", "완주군",
                "진안군", "무주군", "장수군", "임실군", "순창군", "고창군", "부안군"
        ));

        map.put("전라남도", java.util.Arrays.asList(
                "목포시", "여수시", "순천시", "나주시", "광양시", "담양군", "곡성군",
                "구례군", "고흥군", "보성군", "화순군", "장흥군", "강진군", "해남군",
                "영암군", "무안군", "함평군", "영광군", "장성군", "완도군", "진도군",
                "신안군"
        ));

        map.put("경상북도", java.util.Arrays.asList(
                "포항시", "경주시", "김천시", "안동시", "구미시", "영주시", "영천시",
                "상주시", "문경시", "경산시", "군위군", "의성군", "청송군", "영양군",
                "영덕군", "청도군", "고령군", "성주군", "칠곡군", "예천군", "봉화군",
                "울진군", "울릉군"
        ));

        map.put("경상남도", java.util.Arrays.asList(
                "창원시", "진주시", "통영시", "사천시", "김해시", "밀양시", "거제시",
                "양산시", "의령군", "함안군", "창녕군", "고성군", "남해군", "하동군",
                "산청군", "함양군", "거창군", "합천군"
        ));

        map.put("제주특별자치도", java.util.Arrays.asList(
                "제주시", "서귀포시"
        ));

        return map;
    }
}