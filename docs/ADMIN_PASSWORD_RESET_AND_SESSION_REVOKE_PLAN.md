# Admin Password Reset And Session Revoke Plan

## 목적

이 문서는 관리자 계정의 `비밀번호 분실`과 `비밀번호 노출 의심`을 현재 SEAL_LAB 구조에 맞춰 어떻게 처리할지 설계한다.

핵심 원칙은 아래 4가지다.

1. `비밀번호 문제`와 `MFA 문제`를 분리한다.
2. 관리자 비밀번호 reset은 일반 회원보다 더 강한 절차를 쓴다.
3. 비밀번호가 바뀌면 기존 세션은 신뢰하지 않고 `강제 revoke`한다.
4. reset/revoke는 감사 로그와 Discord 알림에 남긴다.

## 현재 구조에서 확인된 사실

- 관리자 로그인은 `비밀번호 1차 인증 -> /mfa/setup 또는 /mfa/verify` 흐름으로 분기된다.
- 관리자 세션은 MFA 완료 후에만 최종 확정된다.
- 관리자 step-up MFA는 `세션 한정 임시 grant` 방식으로 이미 구현되어 있다.
- MFA backup code recovery는 구현되어 있다.
- 반면 `비밀번호 reset`, `비밀번호 변경 후 전체 세션 무효화`, `비밀번호 노출 대응용 운영 절차`는 아직 없다.
- 현재 세션 관리는 기본 `JSESSIONID` 기반이며, 사용자별 활성 세션 추적 저장소는 없다.

## 용어 구분

### 1. 비밀번호 분실

관리자가 로그인 자체를 못 하는 상태다.

목표:
- 본인 확인 후 새 비밀번호를 설정하게 한다.
- reset 직후 모든 기존 세션을 폐기한다.
- reset 후 MFA를 다시 통과하게 한다.

### 2. 비밀번호 노출 의심

로그인은 가능할 수도 있지만, 기존 비밀번호를 계속 쓰면 안 되는 상태다.

목표:
- 즉시 비밀번호를 교체한다.
- 모든 기존 세션과 step-up grant를 폐기한다.
- 필요하면 trust score를 낮추거나 관리자 작업을 다시 MFA/step-up으로 묶는다.

### 3. MFA 분실

비밀번호는 맞지만 MFA 수단이 없는 상태다.

이건 이미 `backup code / recovery` 축으로 처리 중이다.

## 권장 정책

### A. 관리자 비밀번호 분실 reset

권장 방식:

1. `비밀번호 reset 요청`
2. `이메일 링크` 또는 `운영자 승인 reset token` 발급
3. reset 링크 진입 후 `MFA 또는 backup code` 추가 검증
4. 새 비밀번호 설정
5. 모든 기존 세션 revoke
6. 다음 요청부터 관리자 MFA 다시 요구

중요:
- 관리자 계정은 `이메일 링크만으로 즉시 reset 완료`는 약하다.
- 가능하면 `email link + MFA`, 또는 `email link + backup code`, 또는 `운영자 승인 + reset token` 조합이 맞다.

### B. 관리자 비밀번호 노출 의심 대응

권장 방식:

1. 현재 로그인된 관리자가 `비밀번호 변경` 실행
2. 현재 비밀번호 확인
3. 새 비밀번호 입력
4. 성공 즉시 `전체 세션 revoke`
5. 현재 세션도 재로그인 또는 재-MFA 유도
6. Discord/감사 로그에 `PASSWORD_ROTATION` 기록

운영 대응:
- 노출 의심이 있으면 `MFA secret 재등록 권고`도 같이 검토한다.
- 필요하면 `backup code 재발급`도 같이 수행한다.

## 세션 revoke 원리

현재 구조에서 제일 중요한 신규 축은 `세션 폐기`다.

비밀번호가 바뀌었는데 예전 세션이 계속 살아 있으면 관리자 보호가 약해진다.

### 단기 권장안

`users` 테이블에 아래 필드를 추가한다.

- `passwordChangedAt`
- 선택: `forceMfaReverifyAt`

그리고 세션에 아래 값을 저장한다.

- `authEstablishedAt`

검증 로직:
- 요청마다 `authEstablishedAt < passwordChangedAt`이면 세션 무효화
- 세션 무효화 후 `/login?sessionRevoked` 또는 `/mfa/verify`로 보낸다

장점:
- 단일 서버 환경에서 구현이 단순하다.
- 세션 목록을 직접 저장하지 않아도 된다.

한계:
- "어떤 세션들이 살아 있는지"를 관리자가 보는 기능은 제공하지 못한다.

### 중기 권장안

Spring Security `SessionRegistry` 또는 `Spring Session JDBC/Redis`를 도입한다.

그러면 가능해지는 것:
- 사용자별 활성 세션 추적
- 특정 사용자 전체 세션 무효화
- 운영 도구에서 `세션 revoke` 직접 실행
- 멀티 인스턴스 환경 대응

현재 AWS 단일 배포 수준이라면, 1차 구현은 `passwordChangedAt 기반 강제 만료`가 현실적이다.

## 추천 기능 분해

### 1. 관리자 비밀번호 변경

대상:
- 이미 로그인된 관리자

요구:
- 현재 비밀번호 검증
- 새 비밀번호 정책 검증
- 성공 시 모든 세션 revoke
- 필요 시 `step-up grant`도 제거

예상 경로:
- `GET /admin/security/password`
- `POST /admin/security/password`

### 2. 관리자 비밀번호 분실 reset 요청

대상:
- 로그인 불가 관리자

요구:
- `loginId` 또는 `email` 입력
- reset token 생성
- token 만료 시간 부여
- email 또는 운영 도구로 전달

예상 경로:
- `GET /password/forgot`
- `POST /password/forgot`
- `GET /password/reset?token=...`
- `POST /password/reset`

### 3. 관리자 비밀번호 reset 완료

요구:
- token 유효성 확인
- 관리자면 추가 본인 확인
  - 권장: backup code 또는 MFA
- 새 비밀번호 저장
- `passwordChangedAt = now`
- 모든 기존 세션 revoke 처리

## 엔티티/필드 권장안

### User

추가 후보:

- `LocalDateTime passwordChangedAt`
- `LocalDateTime forceSessionReauthAt`
- 선택: `LocalDateTime passwordCompromisedAt`

### PasswordResetToken

신규 엔티티 권장:

- `loginId`
- `tokenHash`
- `expiresAt`
- `usedAt`
- `issuedBy`
- `purpose`
  - `FORGOT_PASSWORD`
  - `COMPROMISE_ROTATION`

평문 token 저장은 피하고 hash 저장을 권장한다.

## 필터/보안 검증 포인트

신규 `PasswordReauthEnforcementFilter` 또는 기존 MFA enforcement 옆 필터 추가를 권장한다.

역할:

1. 인증 세션의 `authEstablishedAt` 확인
2. DB의 `passwordChangedAt`와 비교
3. 과거 세션이면 즉시 invalidate
4. `/login?sessionRevoked`로 redirect

추가 검증:
- step-up grant가 살아 있어도 `passwordChangedAt` 이후가 아니면 무효
- MFA pending session도 reset 이후에는 폐기

## 제로트러스트 관점에서의 처리

비밀번호가 바뀌었다고 해서 `trustScore`를 자동으로 회복하면 안 된다.

권장 동작:

- password reset 후 `trustScore`는 유지
- 대신 `registered fingerprint`, `last location`, `last access`는 선택적으로 유지
- 노출 의심이 강하면 아래 중 하나 검토
  - trust score 소폭 감점
  - 관리자 보호 작업에 step-up 재요구
  - MFA 재등록 권고

즉 비밀번호 교체는 `신원 복구`이지 `위험 신호 제거`가 아니다.

## 감사 로그 / Discord 이벤트 권장안

추가 이벤트:

- `PASSWORD_RESET_REQUESTED`
- `PASSWORD_RESET_TOKEN_ISSUED`
- `PASSWORD_RESET_SUCCEEDED`
- `PASSWORD_RESET_FAILED`
- `PASSWORD_CHANGED_BY_USER`
- `PASSWORD_COMPROMISE_REPORTED`
- `SESSION_REVOKE_TRIGGERED`
- `SESSION_REVOKE_ENFORCED`

필수 필드:

- operator/loginId
- targetLoginId
- requestIp
- sessionFingerprint
- reason
- revokeScope
- tokenPurpose

## UI/운영 경험

### 관리자 본인 비밀번호 변경 화면

보여줄 것:

- 현재 비밀번호
- 새 비밀번호
- 새 비밀번호 확인
- "변경 후 모든 세션에서 다시 로그인 필요" 안내

### 비밀번호 분실 reset 화면

보여줄 것:

- loginId/email 입력
- 관리자 계정은 reset 후 MFA 또는 backup code가 다시 필요하다는 안내

### 운영자용 보조 도구

나중에 추가 검토:

- 특정 관리자 계정 `force password reset`
- 특정 관리자 계정 `force session revoke`
- 사유 입력 필수

## 단계별 구현 순서 권장

### Phase 1

- `User.passwordChangedAt` 추가
- 로그인 성공 시 세션에 `authEstablishedAt` 저장
- 요청 필터에서 `passwordChangedAt` 비교 후 과거 세션 강제 만료
- 관리자 본인 비밀번호 변경 화면 추가

### Phase 2

- `PasswordResetToken` 엔티티 추가
- forgot/reset 흐름 추가
- 관리자 계정 reset 완료 시 backup code 또는 MFA 재검증 연결

### Phase 3

- 운영자용 `force session revoke`
- 운영자용 `force password reset`
- 감사 로그 뷰 추가

## 파일별 변경 예상

- `src/main/java/com/seal/seal_lab/core/entity/User.java`
- 신규 `PasswordResetToken.java`
- 신규 `PasswordResetTokenRepository.java`
- 신규 `PasswordSecurityService.java`
- 신규 `PasswordResetService.java`
- 신규 `PasswordSessionService.java`
- 신규 `PasswordReauthEnforcementFilter.java`
- `src/main/java/com/seal/seal_lab/infra/config/SecurityConfig.java`
- `src/main/java/com/seal/seal_lab/infra/security/LoginSuccessHandler.java`
- 신규 `PasswordController.java`
- 신규 템플릿
  - `templates/auth/password_forgot.html`
  - `templates/auth/password_reset.html`
  - `templates/admin/password_change.html`

## 권장 최종 판단

현재 프로젝트 단계에서는 아래가 가장 현실적이다.

1. 먼저 `관리자 본인 비밀번호 변경 + passwordChangedAt 기반 session revoke`
2. 그 다음 `관리자 비밀번호 분실 reset token + 추가 본인 확인`
3. 마지막으로 `운영자 강제 reset / 강제 revoke`

즉 다음 실제 구현 1순위는 `forgot password` 전체를 한 번에 여는 것보다, `관리자 비밀번호 변경 후 기존 세션 강제 만료`부터 시작하는 쪽이 더 안전하고 구현 위험도도 낮다.
