package com.seal.seal_lab.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeoResponse {
    private String status;
    private double lat; // 위도
    private double lon; // 경도
    private String city;
}