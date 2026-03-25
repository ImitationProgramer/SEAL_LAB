package com.seal.seal_lab.core.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Member {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;        // 이름 (예: 이여민)
    private String degree;      // 학위 (예: 석사생)
    private String role;        // 역할 (예: Graduate Student)
    private String department;  // 학과
    private String email;
    private String keywords;    // 관심분야 키워드
    private String imagePath;   // 저장된 사진 파일명
}