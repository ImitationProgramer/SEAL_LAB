# 🛡️ Project Context: SEAL_LAB (Zero Trust-as-a-Module)

이 문서의 목적은 AI 에이전트(Codex)가 현재 로컬 프로젝트 소스코드의 설계 의도, 아키텍처, 핵심 기능 및 트러블슈팅 이력을 완벽히 이해하고 분석하여, 주현님과 안전하고 일관성 있는 공동 개발을 진행할 수 있도록 콘텍스트를 주입하는 데 있습니다.

---

## 1. 프로젝트 개요 (Project Overview)

* **프로젝트명:** SEAL_LAB (Zero Trust-as-a-Module)
* **기획 의도:** 제로 트러스트(Zero Trust) 아키텍처는 현대 보안의 필수 표준이지만, 높은 도입 비용과 인프라 복잡성으로 인해 중소기업(SME)이나 소규모 연구실이 도입하기에는 기술적·비용적 진입 장벽이 높습니다. 본 프로젝트는 범용적인 **전자정부표준프레임워크 환경(Spring Boot 및 Spring Security)** 내에 가벼운 적응형 보안 모듈을 내재화하여, 추가 하드웨어 솔루션 없이 코드 레벨의 설정만으로 제로 트러스트 도입 효과(지속적 검증, 동적 격리)를 누릴 수 있도록 설계되었습니다.
* **핵심 철학:** 아무도 믿지 않는다(Never Trust, Always Verify). 사용자는 로그인에 성공했더라도, 접속 위치, 시간, 기기 등 실시간 컨텍스트의 변화에 따라 신뢰도가 실시간으로 평가되며 권한이 동적으로 통제됩니다.

---

## 2. 기술 스택 (Tech Stack)

* **Language & Framework:** Java 25 (JDK 25), Spring Boot 4.0.5
* **Security:** Spring Security (OAuth2 / Session), Custom Zero Trust Engine
* **Database & JPA:** MySQL 8.0, Spring Data JPA (Hibernate 7.2)
* **View & UI:** Thymeleaf, AJAX, JavaScript, CSS3
* **Logging & Monitoring:** Logback (Custom Async Appender), Discord Webhook API

---

## 3. 제로 트러스트 아키텍처 설계 (ZTA Components)

NIST SP 800-207 표준 아키텍처 모델을 Spring Boot 구조 내에 객체 지향적으로 매핑하였습니다.

1.  **PEP (Policy Enforcement Point - 정책 강제점): `ZeroTrustAspect`**
    * AOP 기반의 커텀 어노테이션(`@ZeroTrust`)을 활용하여 비즈니스 로직을 전혀 침해하지 않는 비침습적(Non-intrusive) 방식으로 요청을 가로챕니다.
    * PDP가 내린 결정(최종 신뢰 점수)을 바탕으로 요청의 진행(`proceed()`) 또는 차단(`AccessDeniedException`)을 물리적으로 강제합니다.
2.  **PDP (Policy Decision Point - 정책 결정점): `ZeroTrustService`**
    * 사용자의 실시간 컨텍스트 데이터를 수집 및 연산하여 $0$점부터 $100$점 사이의 **동적 신뢰 점수(Adaptive Trust Score)**를 산출합니다.
    * 각 리소스 메서드(`@ZeroTrust(requiredScore = X)`)가 요구하는 임계치와 비교하여 최종 승인 여부를 결정합니다.
3.  **PIP (Policy Information Point - 정책 정보지점): 외부 API 및 DB**
    * **내부 PIP:** `UserRepository` 등을 통해 과거 사용자의 최종 접속 시간, 위도·경도, User-Agent 기록을 관리합니다.
    * **외부 PIP:** `GeoLocationService`를 통해 사용자의 현재 요청 IP 기반 위도/경도 컨텍스트를 동적으로 획득합니다.

---

## 4. 핵심 기능 세부 로직 (Core Mechanics)

### ① 지능형 이상 징후 탐지 알고리즘 (Anomaly Detection)
* **불가능한 이동 속도 탐지 (Impossible Travel / Geovelocity):**
    * 사용자의 이전 요청 위치와 현재 요청 위치 사이의 직선 거리를 하버사인(Haversine) 공식으로 계산합니다.
    * 이전 접속 시간과의 시간 간격을 나누어 실시간 이동 속도($	ext{km/h}$)를 측정합니다.
    * 물리적으로 불가능한 속도(예: 시속 500km 이상)가 감지될 경우, 계정 탈취 위협으로 간주하여 신뢰 점수를 즉시 차감합니다($-60$점 감점).
* **기기 식별 (Device Fingerprinting):**
    * HTTP Request의 `User-Agent` 문자열을 분석하여 과거 정상 접속 기기 이력과 일치하는지 비교합니다.
    * 미등록 기기 또는 브라우저 변조 접근 시 즉시 보안 단계를 강화하기 위해 점수를 차감합니다($-25$점 감점).

### ② 자가 치유 신뢰도 알고리즘 (Self-Healing Score)
* 보안성과 가용성(사용자 편의성)의 조화를 위한 지능형 알고리즘입니다.
* 일시적 컨텍스트 변화로 점수가 깎였더라도, 정상 세션을 유지하며 추가적인 이상 행동을 보이지 않는다면 흐른 시간에 비례하여 신뢰 점수가 자동으로 회복됩니다($10$분당 $1$점 자동 복구).

### ③ 실시간 보안 관제 및 감사 (Security Auditing)
* Logback 환경을 고도화하여 보안 위협 수준(`WARN`) 및 시스템 예외(`ERROR`) 발생 시 **`DiscordAppender`**를 통해 관리자의 디스코드 채널로 실시간 차단 로그 및 컨텍스트 정보를 푸시 알림으로 전송합니다.
* `GlobalExceptionHandler`를 통해 위험 증명 유저(신뢰 점수 미달자)에게 명확한 차단 사유를 안내하고 시스템 보안 로그에 영구 기록합니다.

---

## 5. 주요 트러블슈팅 및 예외 처리 이력 (Troubleshooting History)

AI 에이전트는 향후 리팩토링이나 코드 생성 시 아래의 이력과 최적화 패턴을 반드시 준수해야 합니다.

1.  **데이터 무결성 확보 (4090 예외 제어 버그):**
    * 초기 모델에서 Exception 발생 시 문자열 파싱 및 세션/컨텍스트 결합 오류로 데이터 무결성이 깨지는 버그가 발견되었습니다.
    * **해결책:** 보안 필터 및 인터셉터 간 데이터 공유 시 문자열 파싱 대신 **Spring의 `Request Attribute` 기반 데이터 전달 방식**으로 고도화하여 0.1%의 오차도 없는 정확한 신뢰도 연산 환경을 구축했습니다.
2.  **오탐지율(False Positive) 최적화:**
    * 네트워크 순간 지연이나 모바일 환경의 IP 기지국 변경 등으로 인한 미세한 위치 오차로 정상 사용자가 차단되는 이슈가 있었습니다.
    * **해결책:** 요청 간 이동 거리가 **5km 미만**이거나 시간 간격이 **10초 이내**인 무차별 요청에 대해서는 속도 연산 예외 처리 로직을 설계하여 탐지 정밀도를 높였습니다.

---

## 6. 향후 고도화 계획 및 아쉬운 성찰 (Future Roadmaps)

현재 프로젝트는 애플리케이션 레벨(L7)의 보안에 집중되어 있으나, 자원의 한계와 스케일 문제로 포기했던 아젠다들을 향후 고도화할 비전을 가지고 있습니다.

* **인프라 및 비용 제약으로 인한 타협:**
    * 초기 기획 단계에서는 **Kubernetes** 클러스터 인프라 환경에서 **eBPF (Tetragon, Hubble)** 시스템을 활용해 커널 및 네트워크 레벨(L2, L3)의 로우 데이터를 실시간 수집하고, 이를 신뢰도 점수 엔진과 결합하려 했습니다.
    * 그러나 '중소기업형 초경량 모듈'이라는 목적성과 물리적인 인프라 비용 부담으로 인해 현재는 애플리케이션 프레임워크 레벨로 아키텍처를 최적화했습니다.
* **향후 과제:**
    * 독립형(Standalone) 리눅스 서버에서도 오버헤드 없이 동작 가능한 **경량 eBPF 탐지 모듈**을 별도 구현하여, 프로세스 이상 시스템 콜을 제로 트러스트 엔진의 새로운 PIP로 연동할 계획입니다.
    * 고정된 수치(시속 500km 등) 대신 머신러닝(ML) 모델을 적용하여 유저의 행동 패턴을 학습하는 동적 임계치 기술을 연구 중입니다.

---

## 7. AI 에이전트(Codex)를 위한 작업 지침

1.  **관심사의 분리 준수:** 코드를 추가하거나 수정할 때 비즈니스 로직(Controller, Service)과 보안 인프라 로직(Aspect, SecurityConfig)을 철저히 분리하십시오.
2.  **보안 우선 설계:** 모든 리소스 접근 경로에 대해 적절한 `@ZeroTrust`requiredScore 설정이 유지되거나 확장될 수 있도록 아키텍처 일관성을 유지하십시오.
3.  **의존성 주의:** JDK 25 및 Spring Boot 4.0.5 버전에 호환되는 문법과 라이브러리 스펙(Jakarta EE)을 사용하십시오.
