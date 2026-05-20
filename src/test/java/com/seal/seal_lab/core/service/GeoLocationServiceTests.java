package com.seal.seal_lab.core.service;

import com.seal.seal_lab.api.dto.GeoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeoLocationServiceTests {

    @Mock
    private RestTemplate restTemplate;

    private GeoLocationService geoLocationService;

    @BeforeEach
    void setUp() {
        geoLocationService = new GeoLocationService(restTemplate);
        ReflectionTestUtils.setField(geoLocationService, "apiUrl", "http://ip-api.com/json/");
    }

    @Test
    void returnsMockCampusCoordinatesForLoopbackIp() {
        GeoResponse geoResponse = geoLocationService.getGeoLocation("127.0.0.1");

        assertThat(geoResponse).isNotNull();
        assertThat(geoResponse.getLat()).isEqualTo(37.2758);
        assertThat(geoResponse.getLon()).isEqualTo(127.1325);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void delegatesExternalIpLookupToInjectedRestTemplate() {
        GeoResponse geoResponse = new GeoResponse();
        geoResponse.setLat(37.5);
        geoResponse.setLon(127.0);
        when(restTemplate.getForObject("http://ip-api.com/json/8.8.8.8", GeoResponse.class)).thenReturn(geoResponse);

        GeoResponse result = geoLocationService.getGeoLocation("8.8.8.8");

        assertThat(result).isSameAs(geoResponse);
        verify(restTemplate).getForObject("http://ip-api.com/json/8.8.8.8", GeoResponse.class);
    }

    @Test
    void returnsNullWhenGeoApiCallFails() {
        when(restTemplate.getForObject("http://ip-api.com/json/8.8.4.4", GeoResponse.class))
                .thenThrow(new RestClientException("timeout"));

        GeoResponse result = geoLocationService.getGeoLocation("8.8.4.4");

        assertThat(result).isNull();
        verify(restTemplate).getForObject("http://ip-api.com/json/8.8.4.4", GeoResponse.class);
    }
}
