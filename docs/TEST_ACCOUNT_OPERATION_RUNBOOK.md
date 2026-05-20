# Test Account Operation Runbook

작성일: 2026-05-18

## 목적

이 문서는 `jspark` 같은 테스트 계정을 사용해 SEAL_LAB의 제로트러스트 보안 흐름을 실검증할 때 필요한 운영 절차를 분리해 정리한 문서다.

이 문서는 개발/테스트용이다.

- 실서비스 관리자 계정에 그대로 적용하지 않는다.
- 운영 도구 실행 시 반드시 `reason`을 남긴다.
- 실행 이력은 `/admin/trust/audit`와 Discord 운영 알림에서 함께 확인한다.

## 기본 테스트 계정

- loginId: `jspark`
- role: `ADMIN`

비밀번호는 별도 안전 채널 또는 로컬 보안 설정 기준으로 관리한다.

## 권장 초기화 방식

### 1. 운영 도구 원클릭 baseline

가장 권장되는 방식이다.

절차:
1. 관리자 계정으로 로그인
2. `/trust/debug?loginId=jspark` 진입
3. `테스트 기준선 원클릭 초기화` 실행
4. `reason` 입력

권장 reason 예시:
- `jspark 브라우저 교차 테스트 시작 전 baseline 재설정`
- `관리자 Trust Debug 실검증 전 계정 기준선 복구`

적용 결과:
- `trust_score=100`
- `last_user_agent=fp:chrome|windows|desktop`
- `last_access_time=null`
- `last_latitude=null`
- `last_longitude=null`

## 대체 초기화 방식

### 2. jshell + mysql-connector 직접 초기화

로컬 환경에 `mysql` CLI가 없을 때만 사용한다.

```bash
jshell --class-path /Users/leejoohyun/.gradle/caches/modules-2/files-2.1/com.mysql/mysql-connector-j/9.6.0/a76b3cf1facc2faca4b6e34c1a63ef5e7840131/mysql-connector-j-9.6.0.jar <<'EOF'
import java.sql.*;
try (Connection conn = DriverManager.getConnection(
        "jdbc:mysql://localhost:3306/seal_lab_db?serverTimezone=Asia/Seoul&characterEncoding=UTF-8",
        "root",
        "YOUR_DATABASE_PASSWORD");
     PreparedStatement update = conn.prepareStatement(
             "UPDATE users SET trust_score = ?, last_user_agent = ? WHERE login_id = ?")) {
    update.setInt(1, 100);
    update.setString(2, "fp:chrome|windows|desktop");
    update.setString(3, "jspark");
    System.out.println("updated=" + update.executeUpdate());
}
EOF
```

필요 시 컨텍스트까지 완전히 비우려면 SQL로 아래 필드도 함께 `null`로 바꾼다.

- `last_access_time`
- `last_latitude`
- `last_longitude`
- `last_user_agent`

## 권장 실검증 시나리오

### 1. 관리자 기준선 복구 확인

목적:
- baseline reset이 정상 동작하는지 확인

체크:
- `/trust/debug?loginId=jspark`
- 저장 점수 `100`
- 등록 fingerprint `fp:chrome|windows|desktop`

### 2. 같은 플랫폼 브라우저 변경 완화 확인

목적:
- `Windows + Chrome -> Windows + Firefox` 시 `-10` 완화가 적용되는지 확인

체크:
- 로그인 직후 관리자 작업 접근 가능 여부
- `Trust Debug`의 device penalty와 reason

### 3. 다른 플랫폼 감점 확인

목적:
- 완전히 다른 기기/OS는 더 큰 감점이 적용되는지 확인

체크:
- 관리자 작업 차단 여부
- `Trust Debug` 점수 하락 확인

### 4. 운영 도구 감사 추적 확인

목적:
- override/baseline/reset reason이 로그로 남는지 확인

체크:
- `/admin/trust/audit`
- Discord 운영 알림

## 운영 도구 사용 원칙

- baseline reset은 테스트 시작 전 기준선 복구 용도로만 사용한다.
- 테스트 중 얻은 차단/감점 결과를 없애기 위해 무분별하게 반복 사용하지 않는다.
- `reason`에는 왜 이 조치를 했는지 운영 맥락을 남긴다.

좋은 예:
- `Firefox 동일 플랫폼 감점 테스트 전 baseline 복구`
- `관리자 운영 도구 시나리오 재현을 위한 점수 복원`

피해야 할 예:
- `테스트`
- `초기화`
- 빈 값

## 검증 후 확인 포인트

- `/admin/trust/audit`에 `actionType`, `reason`, `context reset`이 기록되었는지
- Discord 알림에 운영 도구 성공/실패가 들어왔는지
- 테스트 후 필요하면 계정을 다시 baseline으로 돌렸는지

## 관련 문서

- `docs/CODEX_HANDOFF_2026-05-17.md`
- `docs/PROXY_HEADER_DEPLOYMENT.md`
