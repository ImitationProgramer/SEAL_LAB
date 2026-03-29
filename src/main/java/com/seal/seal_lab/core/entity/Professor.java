package com.seal.seal_lab.core.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Professor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String basicInfo;        // 이름, 직함, 연락처 등
    @Column(columnDefinition = "TEXT")
    private String researchInterests; // 연구 분야 (리스트 형식)
    @Column(columnDefinition = "TEXT")
    private String education;         // 학력
    @Column(columnDefinition = "TEXT")
    private String awards;            // 수상 내역
}
