# Member Password Reset Plan

## 목적

이 문서는 일반 회원(`ROLE_MEMBER`)의 비밀번호 분실 reset 흐름을 현재 SEAL_LAB 구조에 맞춰 설계한다.

핵심 원칙:

1. 일반 회원 reset은 관리자 reset보다 단순해야 한다.
2. 관리자용 `MFA`, `backup code`, `step-up` 정책과 분리한다.
3. reset 후에는 기존 세션을 더 이상 신뢰하지 않고 강제 revoke한다.
4. 운영자 수가 적은 현재 상황을 고려해, 과도한 운영 개입 없이 self-service 중심으로 설계한다.

## 현재 구조 기준 상태

이미 구현된 것:
- 회원가입 시 `loginId`, `password`, `name`, `email` 저장
- 비밀번호는 BCrypt로 암호화 저장
- `User.passwordChangedAt` 필드 존재
- `PasswordSessionRevocationFilter`가 `passwordChangedAt` 이후의 오래된 세션을 강제로 만료
- 관리자 전용 `forgot/reset` 흐름 존재

아직 없는 것:
- 일반 회원용 `forgot password`
- 일반 회원용 `reset token`
- 일반 회원용 로그인 후 비밀번호 변경 화면

즉 현재 `/password/forgot`, `/password/reset`은 이름은 공용처럼 보이지만 실제로는 관리자 전용이다.

## 관리자와 일반 회원의 차이

관리자:
- `loginId + email`만으로는 부족
- 추가 본인 확인 필요
- 현재는 `backup code`를 추가 요구

일반 회원:
- 복구 난이도를 너무 높일 필요는 없음
- `loginId + email + reset token` 정도면 충분
- `MFA`, `backup code`를 강제하지 않음

결론:
- 관리자와 일반 회원의 reset 정책은 분리 유지가 맞다.

## 권장 사용자 흐름

### 1. 비밀번호 분실 요청

회원이 로그인 페이지에서 `비밀번호를 잊으셨나요?`를 누른다.

예상 경로:
- `GET /password/forgot/member`
- `POST /password/forgot/member`

입력값:
- `loginId`
- `email`

처리:
- 사용자 존재 확인
- `ROLE_MEMBER`인지 확인
- email이 일치하는지 확인
- reset token 발급

### 2. reset token 전달

현재 프로젝트 운영 상황을 고려한 2단계 옵션:

#### Option A. 현재와 같은 화면 1회 노출

장점:
- 구현이 빠름
- 메일 인프라 필요 없음

단점:
- 실서비스 UX로는 약함
- 관리자가 옆에서 도와주는 상황이 아니면 불편할 수 있음

#### Option B. 메일 발송

장점:
- 일반 회원 self-service에 더 자연스러움

단점:
- SMTP/외부 메일 인프라 필요

권장:
- 1차 구현은 화면 1회 노출 가능
- 장기적으로는 메일 발송으로 이동

### 3. reset 페이지 진입

예상 경로:
- `GET /password/reset/member?token=...`

처리:
- token 존재 확인
- token 만료 여부 확인
- 사용 여부 확인
- 대상 사용자가 `ROLE_MEMBER`인지 확인

화면:
- 새 비밀번호
- 새 비밀번호 확인

### 4. reset 완료

예상 경로:
- `POST /password/reset/member`

처리:
- 새 비밀번호 정책 검증
- 비밀번호 저장
- `passwordChangedAt = now`
- token `usedAt = now`
- 기존 세션 revoke 효과 발생

결과:
- `/login?passwordReset=true`

## 비밀번호 정책

관리자와 같은 패턴을 재사용하는 것이 좋다.

현재 정책:
- 최소 8자
- 영문 포함
- 숫자 포함
- 특수문자 포함

즉 일반 회원도 [UserSignupDto.java](/Users/leejoohyun/IdeaProjects/SEAL_LAB/src/main/java/com/seal/seal_lab/api/dto/UserSignupDto.java:8)와 동일 수준의 비밀번호 정책을 유지하는 것이 일관적이다.

## 세션 revoke

일반 회원도 reset 후 기존 세션은 유지하면 안 된다.

이미 있는 구조를 그대로 재사용 가능:
- `User.passwordChangedAt`
- `PasswordSessionService`
- `PasswordSessionRevocationFilter`

즉 일반 회원 reset을 구현해도 세션 revoke 메커니즘은 새로 만들 필요 없다.

## 토큰 저장 구조

현재 `PasswordResetToken` 엔티티를 재사용할 수 있다.

다만 `purpose`를 조금 확장하는 것이 좋다.

예:
- `ADMIN_FORGOT_PASSWORD`
- `MEMBER_FORGOT_PASSWORD`

또는
- `FORGOT_PASSWORD_ADMIN`
- `FORGOT_PASSWORD_MEMBER`

현재처럼 `FORGOT_PASSWORD` 하나만 쓰고 role 검증으로 분기할 수도 있지만, 운영 추적성은 purpose를 분리하는 편이 더 낫다.

## 접근 경로 권장안

### Option 1. 관리자/회원 경로 분리

- `/password/forgot`
- `/password/reset`
- 관리자 전용으로 유지

- `/password/forgot/member`
- `/password/reset/member`
- 회원 전용으로 추가

장점:
- UI/정책을 명확히 분리 가능
- 운영자 혼동이 적음

이 프로젝트에서는 이 방식이 가장 적절하다.

### Option 2. 공용 경로 하나로 통합

- `/password/forgot`
- `/password/reset`

내부에서 role 따라 분기

단점:
- 정책이 섞이기 쉬움
- 템플릿/문구가 애매해짐

권장하지 않음.

## 감사 로그

일반 회원도 민감 이벤트이므로 최소한 아래 로그는 남기는 게 좋다.

- `MEMBER_PASSWORD_RESET_REQUESTED`
- `MEMBER_PASSWORD_RESET_SUCCEEDED`
- `MEMBER_PASSWORD_RESET_FAILED`

다만 관리자만큼 상세한 Discord 보안 경보까지는 필수는 아니다.

권장:
- 서버 로그는 남김
- Discord 알림은 1차 구현에서는 생략 가능

이유:
- 일반 회원 이벤트 수가 많아질 수 있고
- 운영자 수가 적은 현재는 오히려 노이즈가 될 수 있음

## UI 권장안

### 회원용 forgot 화면

보여줄 것:
- loginId
- email
- 간단한 안내 문구

예:
- "가입 시 사용한 loginId와 email을 입력하세요."

### 회원용 reset 화면

보여줄 것:
- 새 비밀번호
- 새 비밀번호 확인

굳이 backup code, MFA 관련 문구는 넣지 않는다.

## 구현 순서 권장

1. `PasswordResetToken.Purpose`를 member/admin 구분 가능하게 확장
2. `PasswordResetService`에 member용 issue/reset 메서드 추가
3. `PasswordResetController`를 분리하거나 member 전용 컨트롤러 추가
4. 회원용 forgot/reset 템플릿 추가
5. 로그인 페이지에 회원용 `비밀번호 찾기` 링크 추가
6. MockMvc 테스트 추가

## 파일별 변경 예상

- `src/main/java/com/seal/seal_lab/core/entity/PasswordResetToken.java`
- `src/main/java/com/seal/seal_lab/infra/security/PasswordResetService.java`
- `src/main/java/com/seal/seal_lab/api/controller/PasswordResetController.java`
  또는 신규 `MemberPasswordResetController.java`
- 신규 템플릿
  - `templates/auth/member_password_forgot.html`
  - `templates/auth/member_password_reset.html`
- `src/main/resources/templates/login.html`
- `src/test/java/com/seal/seal_lab/SecurityAccessFlowMockMvcTests.java`

## 권장 최종 판단

일반 회원용 비밀번호 분실 reset은 필요한 기능이 맞다.

다만 구현 방식은:
- 관리자 흐름 복제 금지
- `backup code`, `MFA` 제거
- `loginId + email + reset token + passwordChangedAt 기반 session revoke`

이 조합으로 단순하게 가는 것이 현재 프로젝트에 가장 잘 맞는다.
