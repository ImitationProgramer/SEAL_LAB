package com.seal.seal_lab.api;

import com.seal.seal_lab.core.entity.*;
import com.seal.seal_lab.infra.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataInit implements CommandLineRunner {

    private final UserRepository userRepository;
    private final LabIntroRepository labIntroRepository;
    private final PublicationRepository publicationRepository;
    private final ProfessorRepository professorRepository;
    private final NewsRepository newsRepository; // 1. 뉴스 레포지토리 추가
    private final PasswordEncoder passwordEncoder; // 2. 비밀번호 암호화 도구 추가
    // [추가] application.properties에서 값을 읽어옵니다.
    @org.springframework.beans.factory.annotation.Value("${admin.initial.password}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {

        // 1. 기존 'test_admin' 계정이 있다면 무조건 삭제 (초기화 목적)
        userRepository.findByLoginId("jspark").ifPresent(user -> {
            userRepository.delete(user);
            System.out.println(">>> 기존의 stale한 'jspark' 계정을 삭제했습니다.");
        });

        // 2. 새롭게 암호화된 비밀번호로 계정 생성
        User admin = User.builder()
                .loginId("jspark")
                .password(passwordEncoder.encode(adminPassword))
                .name("박정수교수님")
                .email("admin@seal.com")
                .role(User.Role.ADMIN)
                .build();

        userRepository.save(admin);
        System.out.println(">>> [성공] 새 관리자 계정이 생성되었습니다.");
// DataInit.java의 run 메서드에 추가
        if (labIntroRepository.count() == 0) {
            String initialContent = "<h3>Security Engineering and Analysis Lab (SEAL)</h3>" +
                    "<p>정보보안 분야에서 연구와 분석을 수행하는 연구실입니다. SEAL은 시스템 보안과 보안 아키텍처를 비롯한 다양한 정보보안 주제를 중심으로 연구를 진행하며, 새로운 보안 방법론을 탐구합니다.</p>" +
                    "<p>이를 통해 새롭고 다양한 보안 문제를 해결하고, 향상된 보안 기술과 솔루션 개발에 기여하는 것을 목표로 하고 있습니다.</p>" +
                    "<p><strong>[연구 관심 분야]</strong> 시스템 보안, 보안 아키텍처 (Zero Trust, Cross Domain Solution)</p>";

            labIntroRepository.save(LabIntro.builder().content(initialContent).build());
        }

        // 2. 테스트용 메인 뉴스 데이터 삽입 (아코디언 테스트용)
        if (newsRepository.count() == 0) {
            newsRepository.save(News.builder()
                    .title("[공지] 2026년 SEAL 실무 연수 학부생 모집")
                    .content("보안 공학 및 분석 연구실(SEAL)에서 함께 연구할 학부 연구생을 모집합니다. 관심 있는 학생은 Contact 메뉴를 통해 문의 바랍니다.")
                    .build());

            newsRepository.save(News.builder()
                    .title("[소식] Zero Trust 보안 모델 연구 논문 채택")
                    .content("우리 연구실의 Zero Trust 보안 성숙도 모델 관련 연구가 국제 학술지에 채택되었습니다.")
                    .build());

            System.out.println(">>> 테스트 뉴스 데이터 삽입 완료!");
        }

        if (professorRepository.count() == 0) {
            Professor prof = Professor.builder()
                    .basicInfo("<h2>Prof. Jungsoo Park</h2>" +
                            "<p>Assistant Professor<br>School of Software Application<br>Kangnam University</p>" +
                            "<p><a href='mailto:jspark@kangnam.ac.kr'>jspark@kangnam.ac.kr</a><br>+82-31-280-3694</p>")
                    .researchInterests("<ul>" +
                            "<li>Android Malware Analysis</li>" +
                            "<li>Android Security Architecture</li>" +
                            "<li>AI Security (AI for Security & Security for AI)</li>" +
                            "<li>Container Security Architecture</li>" +
                            "<li>Zero Trust & Cross Domain Solution</li>" +
                            "</ul>")
                    .education("<p><strong>Ph.D. in Software Convergence, Aug. 2021</strong><br>Soongsil University, Seoul, Republic of Korea</p>")
                    .awards("<ul>" +
                            "<li>2025 국가정보원장 표창 수상</li>" +
                            "<li>2025 국가 망 보안체계 가이드라인 집필</li>" +
                            "<li>2024 제로트러스트 2.0 가이드라인 집필</li>" +
                            "</ul>")
                    .build();

            professorRepository.save(prof);
            System.out.println(">>> 교수님 초기 데이터 삽입 완료!");
        }
    }
}