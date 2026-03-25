package com.seal.seal_lab.core.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Gallery {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;       // 사진 제목 또는 설명
    private String imagePath;   // 저장된 이미지 경로
    private LocalDateTime uploadDate;
}