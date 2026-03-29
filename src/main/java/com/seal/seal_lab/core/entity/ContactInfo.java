package com.seal.seal_lab.core.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class ContactInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 국문 정보
    private String addressKr;
    private String phone;
    private String labHeadKr;
    private String email;

    // 영문 정보 (푸터용)
    private String addressEn;
    private String labHeadEn;

    // 지도 좌표 (JSON/JS에서 사용할 위도, 경도)
    private Double latitude;
    private Double longitude;
}
