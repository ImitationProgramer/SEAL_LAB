package com.seal.seal_lab.core.service;

import com.seal.seal_lab.api.dto.GeoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeoLocationService {

    @Qualifier("geoRestTemplate")
    private final RestTemplate restTemplate;

    @Value("${geo.ip-api.url:http://ip-api.com/json/}")
    private String apiUrl;

    public GeoResponse getGeoLocation(String ip) {
        // 로컬 환경(127.0.0.1 등)일 경우 테스트를 위해 학교 좌표를 기본값으로 반환
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            GeoResponse mock = new GeoResponse();
            mock.setLat(37.2758); // 강남대학교 위도
            mock.setLon(127.1325); // 강남대학교 경도
            return mock;
        }

        try {
            return restTemplate.getForObject(apiUrl + ip, GeoResponse.class);
        } catch (Exception e) {
            log.error("[Geo-API Error] 위치 정보를 가져올 수 없습니다: {}", e.getMessage());
            return null;
        }
    }
}
