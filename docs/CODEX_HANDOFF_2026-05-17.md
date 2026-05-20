# SEAL_LAB Codex Handoff

작성일: 2026-05-19

## 현재 작업 목적

SEAL_LAB 프로젝트의 제로트러스트 보안 구조를 실제 코드 기준으로 정리하고 고도화 중.

핵심 방향:
- 관리자 UI는 유지
- 서버 측에서 `ROLE_ADMIN` 강제
- 그 위에 `@ZeroTrust(requiredScore=...)`로 동적 신뢰 점수 제어
- 차단 사유를 `권한 부족`과 `신뢰 점수 부족`으로 분리
- 테스트 가능한 보안 경계로 고정

## 이번 세션에서 완료한 것

### 1. 관리자 권한 모델 정리

- `/admin/**`, `/projects/admin/**`는 서버에서 `ROLE_ADMIN` 요구하도록 정리
- `MemberController`의 수정/삭제 엔드포인트에 누락된 `@ZeroTrust` 추가
- 역할 부족은 `CustomAccessDeniedHandler`로 403 처리
- 신뢰 점수 부족은 기존 `ZeroTrustAspect -> GlobalExceptionHandler` 흐름 유지

관련 파일:
- `src/main/java/com/seal/seal_lab/infra/config/SecurityConfig.java`
- `src/main/java/com/seal/seal_lab/infra/security/CustomAccessDeniedHandler.java`
- `src/main/java/com/seal/seal_lab/api/controller/AccessDeniedPageController.java`
- `src/main/java/com/seal/seal_lab/infra/exception/GlobalExceptionHandler.java`
- `src/main/java/com/seal/seal_lab/api/controller/MemberController.java`
- `src/main/resources/templates/error/access-denied.html`

### 2. ZeroTrust false-positive 완화 반영

문서에 적어둔 예외 규칙을 실제 코드에 반영함.

- 이동 거리 `5km 미만`이면 impossible travel 계산 스킵
- 요청 간격 `10초 이하`면 impossible travel 계산 스킵

관련 파일:
- `src/main/java/com/seal/seal_lab/core/service/ZeroTrustService.java`

### 3. Discord webhook 하드코딩 제거

기존 `logback-spring.xml`에 하드코딩되어 있던 webhook URL 제거.

이제 아래 설정값으로 주입해야 함:

```properties
discord.webhook.url=
```

관련 파일:
- `src/main/resources/logback-spring.xml`
- `src/main/resources/application.properties.sample`

### 4. 테스트 추가

보안 시나리오를 회귀 테스트로 고정함.

- 일반 회원이 관리자 경로 직접 호출 시 403
- 관리자라도 trust score 부족 시 차단
- 관리자 + 충분한 trust score면 접근 허용
- geovelocity false-positive 예외 규칙 단위 테스트 추가

관련 파일:
- `src/test/java/com/seal/seal_lab/SecurityAccessFlowMockMvcTests.java`
- `src/test/java/com/seal/seal_lab/core/service/ZeroTrustServiceTests.java`
- `build.gradle`

검증:
- `bash gradlew test` 통과

## 실제 관리자 계정 확인 결과

실제 로컬 서버를 `bootRun`으로 띄우고 아래 계정으로 확인함.

- id: `jspark`
- password: 운영 환경에서 별도 관리

결과:
- 로그인 성공
- 홈 화면에 관리자 UI 노출 확인
  - `ADMIN` 배지
  - `Logout`
  - `Edit Intro`
  - `+ 뉴스 추가하기`
- 하지만 `/admin/member/edit/1` 접근은 403

차단 사유:
- 권한 부족이 아니라 `Zero Trust 점수 부족`
- 실제 응답에서 `현재 점수 56`, `요구 점수 90` 확인

로그 기준 원인:
- 기존 trust score `80`
- 자가 회복 후 `81`
- `Impossible Travel`은 예외 처리로 스킵
- `User-Agent` 불일치로 `-25`
- 최종 `56`

즉 `jspark`는 관리자 권한은 맞지만, 현재 저장된 `lastUserAgent`와 접속 클라이언트가 달라서 관리자 작업이 차단되고 있음.

## MockMvc가 필요한 이유

수동 브라우저 확인만으로는 보안 회귀를 막을 수 없기 때문.

MockMvc로 고정한 시나리오:
- 일반 회원이 숨겨진 관리자 URL 직접 호출
- 관리자라도 trust score 부족
- 관리자 + trust score 충족

효과:
- 이후 코드 수정으로 보안 경계가 무너지면 테스트가 바로 실패

## Discord 로그가 테스트 때 안 뜬 이유

의도된 동작임.

- 테스트에서는 `discord.webhook.url=`로 비워둠
- 자동 테스트가 실운영 Discord 채널로 로그를 쏘지 않게 하기 위함
- 따라서 MockMvc 테스트에서는 webhook 전송이 발생하지 않음

실운영처럼 다시 받으려면 실제 런타임 설정에 `discord.webhook.url`을 넣어야 함.

## 이후 추가 반영 사항

이 handoff 작성 후 아래 작업까지 추가로 반영됨.

- 실제 런타임 `discord.webhook.url` 복구 및 webhook 응답 `204` 확인
- `logback-spring.xml`의 잘못된 `ThresholdFilter` 배치 정리
- 테스트 전용 `logback-test.xml` 추가
  - 테스트에서 Discord 비동기 전송 비활성화
  - `AsyncAppender flush timeout`, `RestTemplate No appenders present` 경고 제거
- `lastUserAgent`를 원본 UA 대신 정규화된 `device fingerprint`로 운영하도록 변경
- 같은 플랫폼 내 브라우저 변경은 `-10`, 완전히 다른 플랫폼은 `-25`로 완화
- `Trust Debug` 페이지 추가
  - 로그인 사용자는 자기 세션 기준 예상 점수/기기/위치 판정 확인 가능
  - 관리자는 특정 `loginId`의 저장 상태 조회 가능
- 관리자용 `trust score` 운영 도구 추가
  - `/admin/trust/override`
  - `/admin/trust/test-baseline`
  - 둘 다 `@ZeroTrust(requiredScore = 95)` 보호
  - 운영 사유 `reason` 필수, 감사 로그에 저장
- 감사 로그 전용 페이지 추가
  - `/admin/trust/audit`
  - `@ZeroTrust(requiredScore = 90)` 보호
  - `operator`, `target`, `dateFrom`, `dateTo` 기준 필터 가능
- 관리자 전용 `Security Audit` 페이지 추가
  - `/admin/security/audit`
  - `@ZeroTrust(requiredScore = 90)` 보호
  - `MFA`, `backup code`, `admin MFA reset`, `password change/reset`, `password session revoke` 이벤트를 최근 50건 기준으로 조회
  - `loginId`, `flowType`, `resultType`, `dateFrom`, `dateTo` 기준 필터 가능
- 원클릭 테스트 기준선 추가
  - `100점 + fp:chrome|windows|desktop + 위치/시간 초기화`
  - `jspark` 같은 실검증 계정 baseline 재설정 용도
- 감사 로그에 `actionType`, `reason` 저장
  - `MANUAL_OVERRIDE`
  - `TEST_BASELINE_RESET`
- `spring.jpa.open-in-view=false` 적용
- 불필요한 `hibernate.dialect` 설정 제거
- Tomcat 세션 영속 복원 비활성화
  - 재시작 시 이전 `SPRING_SECURITY_CONTEXT` 역직렬화 경고 방지 목적
- 프록시 환경 IP 해석 보강
  - `ClientIpResolver` 추가
  - `ZeroTrustAspect`, `LoginSuccessHandler`, `LogoutSuccessHandler`, `TrustDebugController`에서 공통 사용
  - `X-Forwarded-For` 첫 IP 우선 해석
- `ForwardedHeaderFilter` 및 프레임워크 전략 활성화
  - `server.forward-headers-strategy=framework`
  - 배포 메모: `docs/PROXY_HEADER_DEPLOYMENT.md`
- `GeoLocationService`를 주입형 `RestTemplate`로 리팩터링
  - `geo.ip-api.url` 설정값 외부화
  - `2초 connect/read timeout`
- 관리자 MFA + 세션 기반 step-up 인증 1차 구현 완료
  - 관리자 로그인은 비밀번호 1차 인증 후 바로 세션 확정하지 않고 `/mfa/setup` 또는 `/mfa/verify`로 분기
  - 관리자 MFA 미완료 상태는 `AdminMfaEnforcementFilter`가 민감 경로 접근을 `/mfa/**`로 강제 리다이렉트
  - MFA 성공 후에만 `finalizeSuccessfulLogin(...)`으로 trust score 계산 및 trusted login context 등록
  - trust score 부족으로 관리자 기능이 차단되면 `access-denied` 페이지에 `MFA 추가 인증으로 계속하기` 버튼 노출
  - 이 버튼은 DB trust score를 올리지 않고, 세션에만 `step-up grant`를 저장
  - `ZeroTrustAspect`는 기본 점수가 부족해도, 같은 사용자/IP/fingerprint에 대한 유효한 step-up grant가 있으면 요구 점수 이하 작업을 통과시킴
  - step-up grant는 현재 `10분` TTL, 동일 사용자/IP/fingerprint 조건으로 제한
  - 차단된 GET 요청은 원래 URI로 복귀, 차단된 POST 요청은 이전 화면으로 돌아간 뒤 사용자가 다시 실행해야 함
  - MFA issuer 설정값 추가: `mfa.issuer=SEAL_LAB`
  - MFA 템플릿 추가:
    - `src/main/resources/templates/auth/mfa_setup.html`
    - `src/main/resources/templates/auth/mfa_verify.html`
  - MFA 관련 테스트 추가:
    - `src/test/java/com/seal/seal_lab/infra/security/MfaServiceTests.java`
    - `src/test/java/com/seal/seal_lab/infra/security/MfaSessionServiceTests.java`
    - `SecurityAccessFlowMockMvcTests`에 관리자 pending MFA / step-up candidate / step-up grant 허용 시나리오 추가
  - 검증:
    - `bash gradlew test` 통과
- 관리자 MFA 이벤트 감사 로그/Discord 구조화 완료
  - `LOGIN_SETUP`, `LOGIN_VERIFY`, `STEP_UP`의 `STARTED/SUCCESS/FAILURE`를 `MfaAuditLog`로 저장
  - 같은 이벤트를 Discord용 멀티라인 포맷으로도 남김
- `mfaSecret` 평문 저장 제거
  - `MfaSecretCryptoService`로 `AES/GCM` 암호화 저장
  - 저장 형식: `enc:v1:...`
  - 기존 평문 값은 `decryptIfNeeded(...)`로 호환
- MFA setup 화면에 실제 QR 이미지 렌더링 추가
  - 서버가 `/mfa/setup/qr`에서 PNG 생성
  - 사용자는 인증 앱으로 바로 스캔 가능
- 관리자 분실 대응용 backup code / recovery 구현
  - MFA setup 완료 시 1회용 backup code 8개 발급
  - DB에는 평문이 아니라 `SHA-256 hash`만 저장
  - 평문 backup code는 최초 1회 화면에서만 노출
  - 로그인 MFA와 step-up MFA 둘 다 backup code로 대체 가능
  - backup code 사용 성공/실패도 `MfaAuditLog` + Discord로 남김
- step-up 정책 설정 외부화 완료
  - `seal.security.zero-trust.absolute-min-score`
  - `seal.security.mfa.step-up.ttl-minutes`
  - `seal.security.mfa.step-up.validate-request-ip`
  - `seal.security.mfa.step-up.validate-fingerprint`
  - `ZeroTrustPolicyProperties`, `MfaStepUpProperties`로 로딩

주요 추가/변경 파일:
- `src/main/java/com/seal/seal_lab/api/controller/TrustDebugController.java`
- `src/main/java/com/seal/seal_lab/api/dto/TrustDebugView.java`
- `src/main/java/com/seal/seal_lab/core/entity/TrustOverrideAuditLog.java`
- `src/main/java/com/seal/seal_lab/infra/repository/TrustOverrideAuditLogRepository.java`
- `src/main/java/com/seal/seal_lab/infra/config/TomcatSessionConfig.java`
- `src/main/java/com/seal/seal_lab/infra/config/ProxyHeaderConfig.java`
- `src/main/java/com/seal/seal_lab/infra/config/GeoClientConfig.java`
- `src/main/java/com/seal/seal_lab/infra/web/ClientIpResolver.java`
- `src/main/java/com/seal/seal_lab/api/controller/MfaController.java`
- `src/main/java/com/seal/seal_lab/core/entity/MfaAuditLog.java`
- `src/main/java/com/seal/seal_lab/core/entity/MfaBackupCode.java`
- `src/main/java/com/seal/seal_lab/infra/repository/MfaAuditLogRepository.java`
- `src/main/java/com/seal/seal_lab/infra/repository/MfaBackupCodeRepository.java`
- `src/main/java/com/seal/seal_lab/infra/logging/MfaAuditLogFormatter.java`
- `src/main/java/com/seal/seal_lab/infra/security/MfaAuditService.java`
- `src/main/java/com/seal/seal_lab/infra/security/MfaBackupCodeService.java`
- `src/main/java/com/seal/seal_lab/infra/security/MfaService.java`
- `src/main/java/com/seal/seal_lab/infra/security/MfaSecretCryptoService.java`
- `src/main/java/com/seal/seal_lab/infra/security/MfaSessionService.java`
- `src/main/java/com/seal/seal_lab/infra/security/MfaQrCodeService.java`
- `src/main/java/com/seal/seal_lab/infra/security/PasswordSessionService.java`
- `src/main/java/com/seal/seal_lab/infra/security/PasswordSecurityService.java`
- `src/main/java/com/seal/seal_lab/infra/security/PasswordSessionRevocationFilter.java`
- `src/main/java/com/seal/seal_lab/infra/security/PasswordResetService.java`
- `src/main/java/com/seal/seal_lab/infra/security/AdminMfaEnforcementFilter.java`
- `src/main/java/com/seal/seal_lab/infra/config/ZeroTrustPolicyProperties.java`
- `src/main/java/com/seal/seal_lab/infra/config/MfaStepUpProperties.java`
- `src/main/java/com/seal/seal_lab/infra/config/PasswordResetProperties.java`
- `src/main/java/com/seal/seal_lab/core/entity/PasswordResetToken.java`
- `src/main/java/com/seal/seal_lab/infra/repository/PasswordResetTokenRepository.java`
- `src/main/java/com/seal/seal_lab/api/controller/AdminSecurityController.java`
- `src/main/java/com/seal/seal_lab/api/controller/MemberSecurityController.java`
- `src/main/java/com/seal/seal_lab/api/controller/PasswordResetController.java`
- `src/main/resources/templates/trust/debug.html`
- `src/main/resources/templates/trust/audit.html`
- `src/main/resources/templates/auth/mfa_setup.html`
- `src/main/resources/templates/auth/mfa_verify.html`
- `src/main/resources/templates/auth/mfa_recovery.html`
- `src/main/resources/templates/auth/mfa_recovery_codes.html`
- `src/main/resources/templates/auth/mfa_recovery_status.html`
- `src/main/resources/templates/admin/password_change.html`
- `src/main/resources/templates/auth/password_forgot.html`
- `src/main/resources/templates/auth/password_reset_requested.html`
- `src/main/resources/templates/auth/password_reset.html`
- `src/main/resources/templates/auth/member_password_change.html`
- `src/main/resources/templates/auth/member_password_forgot.html`
- `src/main/resources/templates/auth/member_password_reset_requested.html`
- `src/main/resources/templates/auth/member_password_reset.html`
- `src/test/resources/logback-test.xml`
- `docs/PROXY_HEADER_DEPLOYMENT.md`
- `docs/MFA_SECRET_KEY_OPERATIONS.md`
- `docs/ADMIN_PASSWORD_RESET_AND_SESSION_REVOKE_PLAN.md`

## 관리자 MFA / Step-up 현재 상태

현재 구현된 것:
- 관리자 로그인 시 MFA setup/verify 분기
- trust 차단 페이지에서 관리자 step-up MFA 시작 버튼 노출
- step-up은 세션 한정 grant로만 동작하며 DB trust score는 변경하지 않음
- step-up grant는 현재 `IP + fingerprint + grantedScore + expiresAt` 기준으로 검사
- backup code 8개를 setup 완료 직후 1회 노출하고, 이후에는 해시만 DB에 보관
- backup code는 로그인 MFA 대체와 step-up MFA 대체 둘 다 가능
- backup code 상태 조회 `/mfa/recovery/status` 구현
- backup code 재발급 `/mfa/recovery/reissue` 구현
  - 재발급 시 기존 미사용 backup code는 모두 폐기
  - 새 code 세트는 기존 1회 노출 화면에서 다시 한 번만 보여줌
  - 재발급 시작/성공/실패도 `MfaAuditLog` + Discord 포맷으로 남김
- 운영자용 `MFA reset` 정책 및 구현 반영
  - `backup code`가 1차 복구 수단
  - `운영자 MFA reset`은 최후 수단
  - `/admin/mfa/reset`는 `@ZeroTrust(requiredScore = 95)` 보호
  - `reason` 필수
  - 실행 시 대상 계정의 `mfaEnabled=false`, `mfaSecret=null`, `mfaEnrolledAt=null`
  - 기존 backup code도 전부 폐기
  - 다음 요청부터 `AdminMfaEnforcementFilter`가 `/mfa/setup`을 강제
  - 시작/성공/실패는 `MfaAuditLog` + Discord 포맷으로 기록
- `mfaSecret`는 암호화 저장되고, 기존 평문 값도 호환
- QR 이미지는 `/mfa/setup/qr`에서 서버가 직접 생성
- `absolute minimum score`, `step-up TTL`, `IP/fingerprint strictness`는 설정값으로 제어 가능
- 관리자 본인 비밀번호 변경 `/admin/security/password` 구현
  - 현재 비밀번호 검증 후 새 비밀번호로 교체
  - 성공 시 `passwordChangedAt` 갱신
  - 현재 세션 포함 기존 세션은 다음 요청부터 강제 만료
- `PasswordSessionRevocationFilter` 구현
  - 세션의 `authEstablishedAt`와 DB의 `passwordChangedAt` 비교
  - 예전 세션이면 `/login?sessionRevoked=true`로 강제 종료
- 관리자 비밀번호 분실용 `forgot/reset token` 구현
  - `/password/forgot`에서 `loginId + email` 확인 후 1회용 reset token 발급
  - 현재는 메일 인프라 대신 화면에 token을 1회 노출
  - `/password/reset?token=...`에서 token 검증 후 `backup code` 1개를 추가로 요구
  - 비밀번호 재설정 성공 시 `passwordChangedAt` 갱신, 기존 세션 강제 만료 효과 발생
- 일반 회원 비밀번호 분실용 `forgot/reset token` 구현
  - `/password/forgot/member`에서 `loginId + email` 확인 후 1회용 reset token 발급
  - 현재는 관리자와 동일하게 화면 1회 노출 방식을 유지
  - `/password/reset/member?token=...`에서 token 검증 후 새 비밀번호만 입력
  - `backup code`, `MFA`는 요구하지 않음
  - 비밀번호 재설정 성공 시 `passwordChangedAt` 갱신, 기존 세션 강제 만료 효과 발생
- 일반 회원 로그인 후 비밀번호 변경 `/member/security/password` 구현
  - 현재 비밀번호 검증 후 새 비밀번호로 교체
  - `ROLE_MEMBER` 경로로 분리
  - 성공 시 `passwordChangedAt` 갱신
  - 현재 세션 포함 기존 세션은 다음 요청부터 강제 만료
  - 헤더에 `Change Password` 링크 추가
- `Trust Debug`는 이제 관리자 전용
  - `/trust/**` 경로는 `ROLE_ADMIN`으로 제한
  - 일반 회원은 `Trust Debug` 진입 불가
  - 헤더의 `Trust Debug` 링크도 관리자에게만 노출
- `Security Audit` 구현으로 MFA/backup code/password 보안 이벤트를 하나의 관리자 전용 화면에서 조회 가능
- 비밀번호 보안 이벤트도 `MfaAuditLog` 축으로 기록
  - `ADMIN_PASSWORD_CHANGE`
  - `MEMBER_PASSWORD_CHANGE`
  - `ADMIN_PASSWORD_RESET_ISSUE`
  - `ADMIN_PASSWORD_RESET_COMPLETE`
  - `MEMBER_PASSWORD_RESET_ISSUE`
  - `MEMBER_PASSWORD_RESET_COMPLETE`
  - `PASSWORD_SESSION_REVOKE`
- 관련 설정 외부화
  - `seal.security.password-reset.token-ttl-minutes`

아직 남은 것:
- `mfa.secret-encryption-key`는 `${MFA_SECRET_ENCRYPTION_KEY}` 환경변수 기반으로 전환 완료
- 테스트는 `src/test/resources/application.properties`의 전용 테스트 키 사용
- 실배포 주입 경로는 `AWS EC2 + systemd EnvironmentFile (/etc/seal-lab/secrets.env)` 기준으로 확정
- 관련 운영 문서: `docs/MFA_SECRET_KEY_OPERATIONS.md`
- 관리자/회원 reset token은 현재 `화면 1회 노출 유지`로 운영 정책 확정
- 메일/운영 채널 전달 방식은 현 단계 우선순위에서 제외
- 일반 회원용 password forgot/reset은 구현됨
- 운영자용 강제 `MFA reset`, 강제 `password reset`, 강제 `session revoke`는 아직 없음
- 운영자용 `MFA reset`은 구현됨
- 운영자용 강제 `password reset`, 강제 `session revoke`는 아직 없음
- `Security Audit` 화면은 구현됨
- `MFA/backup code/password` 전용 별도 추가 감사 화면은 현재 보류
- `Trust Debug`는 관리자 전용으로 제한됨
  - `/trust/**`는 `ROLE_ADMIN`
  - 헤더의 `Trust Debug` 링크도 관리자에게만 노출
- `User` 중심 연구실 직급 모델 도입 시작
  - `User.labRank` 추가
  - 직급 enum:
    - `GENERAL_PUBLIC`
    - `INTERN`
    - `UNDERGRAD_RESEARCHER`
    - `MASTER_STUDENT`
    - `PROFESSOR`
  - 한글 라벨/정렬 우선순위 포함
  - 회원가입 기본값은 `GENERAL_PUBLIC`
- `User`에 공개 프로필 필드 추가
  - `department`
  - `keywords`
  - `imagePath`
  - `bio`
- 관리자 전용 전체 `User` 관리 화면 구현
  - `/admin/users`
  - `@ZeroTrust(requiredScore = 90)` 보호
  - 전체 계정 조회, `role`, `labRank`, 공개 노출 여부, MFA 여부 확인 가능
  - `POST /admin/users/{loginId}/lab-rank`
  - 직급 변경은 `@ZeroTrust(requiredScore = 95)` 보호
- 공개 인원 페이지 `/about/member`는 이제 `Member`가 아니라 `User.labRank` 기준 조회
  - `GENERAL_PUBLIC`은 미노출
  - `INTERN`, `UNDERGRAD_RESEARCHER`, `MASTER_STUDENT`, `PROFESSOR`만 노출
  - 기존 `Add Member` 버튼은 제거되고 관리자 `User 관리` 링크로 대체
- 본인/관리자 공개 프로필 편집 구현
  - 본인: `GET/POST /profile/me/edit` with `@ZeroTrust(requiredScore = 70)`
  - 관리자 타인 편집: `GET/POST /admin/users/{loginId}/edit` with `@ZeroTrust(requiredScore = 90)`
  - 수정 가능한 공개 필드:
    - `name`
    - `email`
    - `department`
    - `keywords`
    - `bio`
    - `imagePath`
  - `labRank`는 읽기 전용, 변경은 `User 관리` 화면에서만 수행

## 연구실 직급/프로필 모델 구현 상태

이미 구현된 것:

- 회원가입 기본 직급 `GENERAL_PUBLIC`
- 관리자 전체 계정 조회 `/admin/users`
- 관리자 직급 변경 `/admin/users/{loginId}/lab-rank`
- 공개 인원 페이지의 `GENERAL_PUBLIC` 미노출, 연구실 직급 자동 노출
- 본인 공개 프로필 수정 가능
- 관리자 타인 공개 프로필 수정 가능
- 기존 `Member` 수동 편집 경로(`/admin/member/**`) 제거 완료
- `Member` 엔티티/리포지토리/`about/member_edit.html` 제거 완료
- 기존 `member` 테이블 -> `users` 공개 프로필 필드 자동 마이그레이션 구현
- `jspark` 계정은 시작 시 `ADMIN` 상태에서 `PROFESSOR`로 자동 보정

아직 남은 것:

- 프로필 이미지 저장 경로는 아직 배포 환경 설정값으로 외부화되지 않음
- 마이그레이션 전략 문서:
  - `docs/MEMBER_TO_USER_MIGRATION_PLAN.md`

## 테스트 계정 운영 절차

`jspark` 같은 테스트 계정의 baseline reset, 브라우저 교차 테스트, 운영 도구 사용 원칙은 아래 별도 문서로 분리함.

- `docs/TEST_ACCOUNT_OPERATION_RUNBOOK.md`

핵심:
- 테스트 계정은 운영 도구로 baseline reset 가능
- `reason`을 반드시 남김
- 실서비스 운영 절차와 혼동하지 않도록 별도 문서에서 관리

## 프록시 배포 메모

프록시 뒤 배포 시 참고 문서:

- `docs/PROXY_HEADER_DEPLOYMENT.md`
- `docs/MFA_SECRET_KEY_OPERATIONS.md`

핵심:
- 앱 서버는 외부에서 직접 접근되지 않게 막고, 신뢰 프록시를 앞단에 둔다.
- `X-Forwarded-For`, `X-Real-IP`는 오직 프록시가 덮어써서 넣게 한다.
- 앱은 `ForwardedHeaderFilter` + `ClientIpResolver` 기준으로 실제 클라이언트 IP를 복원한다.

## 현재 우선순위 5

1. EC2 운영 서버에 `/etc/seal-lab/secrets.env`와 `systemd EnvironmentFile`을 실제 반영
2. 운영자용 강제 `password reset` 또는 강제 `session revoke`까지 확장할지 결정
3. 회원 비밀번호 변경/분실 reset에 대한 운영 알림 수준을 관리자와 분리할지 결정
4. 프로필 이미지 저장 경로를 배포 환경 설정값으로 외부화
5. 운영 DB에서 legacy `member` 데이터가 실제로 얼마나 자동 매칭되는지 1회 점검

## 다음 세션 시작 프롬프트 예시

다음 세션에서 이렇게 말하면 됨:

`SEAL_LAB 이어서 하자. docs/CODEX_HANDOFF_2026-05-17.md 먼저 읽고 보안 복구 흐름 후속 작업부터 진행해줘.`

또는

`SEAL_LAB 보안 고도화 이어서 진행해줘. handoff 문서 읽고 관리자 비밀번호 분실/노출 대응 설계부터 봐줘.`

## Codex resume 사용법

이 환경의 로컬 CLI 도움말 기준으로 `resume` 서브커맨드는 실제로 존재함.

기본:

```bash
codex resume
```

설명:
- 세션 picker가 뜸
- 현재 디렉터리 기준으로 이어갈 세션을 고를 수 있음

가장 최근 세션 바로 이어가기:

```bash
codex resume --last
```

특정 세션 ID로 이어가기:

```bash
codex resume <SESSION_ID>
```

세션 이어가면서 바로 추가 프롬프트 넣기:

```bash
codex resume --last "docs/CODEX_HANDOFF_2026-05-17.md 먼저 읽고 이어서 진행해줘"
```

모든 세션 목록까지 보고 고르기:

```bash
codex resume --all
```

참고:
- `codex resume`은 이전 인터랙티브 세션을 다시 여는 명령
- 그래도 가장 안전한 재개 방법은 이 handoff 문서를 같이 읽히는 것
