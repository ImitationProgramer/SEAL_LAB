package com.seal.seal_lab.core.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;      // 연구 과제명
    private String period;     // 수행 기간 (예: 2023.01 ~ 2025.12)
    private String agency;     // 출연 기관 (예: 한국연구재단)

    // 정렬을 위해 생성일 등을 추가할 수 있습니다.
}
