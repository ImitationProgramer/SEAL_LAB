# Member to User Migration Plan

## 목적

기존 `member` 테이블 기반 공개 인원 데이터를 `users` 테이블의 공개 프로필 필드로 흡수한다.

현재 코드 기준:

- 공개 인원 페이지는 이미 `User` 기준 조회로 전환됨
- `Member` 엔티티/리포지토리/수동 편집 경로는 제거됨
- 따라서 남은 작업은 `운영 DB`에 남아 있을 수 있는 기존 `member` 데이터를 `users`로 이관하는 것

## 현재 `User` 공개 프로필 필드

- `name`
- `email`
- `department`
- `keywords`
- `imagePath`
- `bio`
- `labRank`

## 마이그레이션 목표

기존 `member` 테이블의 공개 인원 데이터를 가능한 한 기존 계정(`users`)에 흡수하고,
직급은 `labRank`로 통일한다.

최종 상태:

- 공개 인원 페이지는 `users.lab_rank` 기준으로만 렌더링
- `GENERAL_PUBLIC`은 미노출
- `INTERN`, `UNDERGRAD_RESEARCHER`, `MASTER_STUDENT`, `PROFESSOR`만 노출

## 권장 절차

### 1. 운영 DB 백업

반드시 전체 백업 후 진행:

- `users`
- 기존 `member`
- 관련 업로드 파일 디렉터리

### 2. 기존 `member` 데이터 덤프 확인

운영 DB에서 먼저 아래를 확인:

- 총 `member` 행 수
- `name`, `email`, `department`, `keywords`, `image_path` 값 분포
- `degree`, `role` 값 분포

특히 `degree`, `role` 문자열이 실제로 어떤 값으로 저장돼 있는지 먼저 확인해야 한다.

### 3. `member -> users` 매칭 규칙 확정

1차 매칭 기준:

- `email` 완전 일치

2차 매칭 기준:

- `name` 일치 + 관리자 수동 확인

권장:

- 자동 이관은 `email` 완전 일치인 경우만 수행
- `email`이 없거나 불일치인 데이터는 별도 목록으로 추출 후 수동 확인

이유:

- `name` 단독 매칭은 동명이인/표기 차이 위험이 큼

### 4. 직급 문자열을 `labRank`로 매핑

권장 매핑:

- `교수`, `Professor` -> `PROFESSOR`
- `석사`, `석사생`, `Graduate Student`, `Master Student` -> `MASTER_STUDENT`
- `학부연구생`, `Undergraduate Researcher` -> `UNDERGRAD_RESEARCHER`
- `인턴`, `Intern` -> `INTERN`

매핑 불가 값:

- 자동 변환하지 말고 별도 검토 목록으로 분리

### 5. `users` 공개 프로필 필드 업데이트

매칭된 사용자에 대해 아래 규칙으로 갱신:

- `users.name`
  - 기존 값이 비어 있거나 member 값이 더 정확한 경우만 덮어쓰기 검토
- `users.email`
  - 일반적으로 로그인/복구에 쓰이므로 함부로 덮어쓰지 말 것
  - `member.email`과 `users.email`이 다르면 수동 검토 권장
- `users.department`
  - `member.department`로 채움
- `users.keywords`
  - `member.keywords`로 채움
- `users.image_path`
  - 기존 이미지 경로 유지/이관
- `users.bio`
  - 기존 `member`에 대응 필드가 없으면 비워 둠
- `users.lab_rank`
  - 위 매핑 규칙으로 설정

### 6. `jspark` 운영 계정 보정

별도 확인 항목:

- `login_id = 'jspark'`
- `role = ADMIN`
- `lab_rank = PROFESSOR`

이 계정은 자동 추론에 맡기지 말고 운영자가 직접 검증 후 보정하는 것을 권장한다.

### 7. 미매칭/불완전 데이터 처리

아래 데이터는 자동 반영하지 말고 수동 검토:

- `member.email` 없음
- `users`에서 같은 이메일 계정이 없음
- `degree/role` 값이 매핑표에 없음
- 이미지 파일 경로가 깨져 있음

권장 처리:

- CSV 또는 SQL 결과로 별도 추출
- 관리자 화면(`/admin/users`)에서 수동 보정

## 실제 적용 방식

권장 순서는:

1. 운영 DB 백업
2. `member` 테이블 read-only 덤프
3. 스테이징 또는 로컬 복제 DB에서 이관 SQL/스크립트 검증
4. 운영에 동일 절차 적용
5. `/about/member`, `/admin/users`에서 결과 확인

## SQL/스크립트 전략

권장 방식:

- 1회성 마이그레이션 SQL 또는 스크립트 작성
- 자동 반영 대상만 업데이트
- 모호한 행은 건너뛰고 별도 리포트 생성

비권장 방식:

- 이름만 보고 일괄 UPDATE
- `users.email`을 무조건 `member.email`로 덮어쓰기

## 적용 후 검증 체크리스트

- `GENERAL_PUBLIC` 사용자는 인원 페이지에 보이지 않는가
- `INTERN`, `UNDERGRAD_RESEARCHER`, `MASTER_STUDENT`, `PROFESSOR`는 정상 노출되는가
- `jspark`가 `ADMIN + PROFESSOR`로 보이는가
- 관리자 `/admin/users`에서 직급과 프로필 필드가 정상 보이는가
- 본인 프로필 편집(`/profile/me/edit`)이 정상 동작하는가
- 이미지 경로가 실제 배포 환경에서도 깨지지 않는가

## 현재 결론

가장 안전한 기준은 다음과 같다.

- 자동 이관은 `email` 일치 데이터만
- `degree/role`은 명시적 매핑표로만 변환
- `jspark`는 수동 보정
- 모호한 데이터는 관리자가 `/admin/users`에서 후처리

