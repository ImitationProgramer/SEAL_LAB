package com.seal.seal_lab.core.entity;
import com.seal.seal_lab.core.enums.PubCategory;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Entity
@Table(name="publications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Publication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private PubCategory category; // 카테고리

    private String title;         // 논문/특허 제목
    private String authors;       // 저자 (예: 홍길동, 박정수*)
    private String venue;         // 게재지/학회명/등록기관
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate publishDate; // 발행일 (정렬의 기준)
    private String link;          // DOI 또는 관련 링크
}