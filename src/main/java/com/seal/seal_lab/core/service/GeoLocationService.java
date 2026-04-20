package com.seal.seal_lab.core.service;

import com.seal.seal_lab.api.dto.GeoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class GeoLocationService {

    private final String API_URL = "http://ip-api.com/json/";

    public GeoResponse getGeoLocation(String ip) {
        // 로컬 환경(127.0.0.1 등)일 경우 테스트를 위해 학교 좌표를 기본값으로 반환
        if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1")) {
            GeoResponse mock = new GeoResponse();
            mock.setLat(37.2758); // 강남대학교 위도
            mock.setLon(127.1325); // 강남대학교 경도
            return mock;
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            return restTemplate.getForObject(API_URL + ip, GeoResponse.class);
        } catch (Exception e) {
            log.error("[Geo-API Error] 위치 정보를 가져올 수 없습니다: {}", e.getMessage());
            return null;
        }
    }
}