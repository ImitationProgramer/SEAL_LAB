# MFA Secret Key Operations

작성일: 2026-05-19

## 목적

`mfa.secret-encryption-key`는 관리자 MFA secret을 DB에 평문으로 두지 않기 위한 애플리케이션 암호화 키다.

현재 코드:
- `src/main/java/com/seal/seal_lab/infra/security/MfaSecretCryptoService.java`
- 저장 형식: `enc:v1:...`
- 암호화 방식: `AES/GCM`

이 문서는 로컬 개발, 서버 배포, AWS 기준 운영 원칙을 정리한다.

## 기본 원칙

- 이 키는 Git에 실제 운영값을 커밋하지 않는다.
- 운영/스테이징/로컬은 서로 다른 키를 사용한다.
- 키를 잃어버리면 기존에 저장된 MFA secret 복호화가 불가능해질 수 있다.
- 따라서 키 교체는 단순 문자열 변경이 아니라 `재등록 전략`과 함께 다뤄야 한다.

## 로컬 개발

권장 방식:
- `application.properties`는 `${MFA_SECRET_ENCRYPTION_KEY}`만 읽게 둔다.
- 실제 값은 셸, IDE 실행 설정, 배포 환경변수에서 주입한다.

예시:

```bash
export MFA_SECRET_ENCRYPTION_KEY="local-dev-only-key-change-me"
```

현재 프로젝트 매핑:

```properties
mfa.secret-encryption-key=${MFA_SECRET_ENCRYPTION_KEY}
```

주의:
- 이제 `application.properties`에는 기본 평문값이 없다.
- 로컬에서 환경변수를 주지 않으면 애플리케이션이 시작되지 않는다.
- 테스트는 `src/test/resources/application.properties`에서 전용 테스트 키를 사용한다.

## AWS 운영 권장안

가장 현실적인 권장 순서:

1. 환경변수 주입
2. AWS Secrets Manager
3. 필요 시 KMS와 조합

## 이번 프로젝트에서 확정한 주입 경로

현재 저장소에는 ECS, Elastic Beanstalk, App Runner 같은 배포 아티팩트가 없고,
앞서 정리한 프록시 문맥도 `Nginx` 기준이다.

따라서 지금 SEAL_LAB의 기준 운영 경로는 아래로 확정한다.

- `AWS EC2 단일 서버`
- `Nginx`가 앞단 프록시
- Spring Boot 앱은 `systemd` 서비스로 실행
- `MFA_SECRET_ENCRYPTION_KEY`는 EC2의 `EnvironmentFile`에서 주입

즉 `application.properties`는 `${MFA_SECRET_ENCRYPTION_KEY}`만 읽고,
실제 값은 운영 서버의 파일 시스템에서 `systemd -> 환경변수 -> JVM` 순서로 전달한다.

### 확정된 운영 방식

1. EC2에 비밀 파일 생성

권장 경로:

```bash
sudo mkdir -p /etc/seal-lab
sudo chmod 700 /etc/seal-lab
sudo sh -c 'printf "%s\n" "MFA_SECRET_ENCRYPTION_KEY=YOUR_PROD_KEY_HERE" > /etc/seal-lab/secrets.env'
sudo chmod 600 /etc/seal-lab/secrets.env
```

2. `systemd` 서비스 유닛에 연결

예시:

```ini
[Service]
EnvironmentFile=/etc/seal-lab/secrets.env
ExecStart=/usr/bin/java -jar /opt/seal-lab/app.jar
```

3. 적용

```bash
sudo systemctl daemon-reload
sudo systemctl restart seal-lab
```

4. 검증

```bash
sudo systemctl show seal-lab --property=Environment
```

또는 애플리케이션 기동 로그에서 `mfa.secret-encryption-key` 관련 placeholder 오류가 없는지 확인한다.

### 왜 이 경로로 확정했는가

장점:
- 현재 배포 구조와 가장 잘 맞는다.
- 코드 변경 없이 바로 적용 가능하다.
- Git, `application.properties`, 셸 export 기록에 운영 키를 남기지 않을 수 있다.
- 나중에 Secrets Manager를 쓰더라도 최종 주입 지점을 같은 환경변수로 유지할 수 있다.

단점:
- 키가 EC2 파일 시스템에 존재하므로 인스턴스 접근 통제가 중요하다.
- 인스턴스가 늘어나면 각 서버에 동일 절차를 반영해야 한다.

결론:
- `지금 당장`은 EC2 `EnvironmentFile` 방식으로 운영
- `나중에` 필요하면 Secrets Manager 값을 배포 단계에서 이 파일에 써 넣거나, 서비스 시작 스크립트에서 환경변수로 주입

### 1. 가장 단순한 방식: 환경변수

EC2, ECS, Elastic Beanstalk, App Runner 같은 환경에서:
- `MFA_SECRET_ENCRYPTION_KEY`를 환경변수로 주입
- `application.properties`는 `${MFA_SECRET_ENCRYPTION_KEY}`만 읽게 구성

장점:
- 구현이 가장 단순
- 현재 구조와 바로 맞물림

단점:
- 콘솔/배포 파이프라인에서 값 노출 관리가 필요

### 2. 권장 운영 방식: AWS Secrets Manager

추천 상황:
- 이미 AWS에 배포되어 있고
- 운영 비밀값을 콘솔이나 IaC로 일원 관리하고 싶을 때

운영 절차:
- Secrets Manager에 예: `seal-lab/prod/mfa-secret-encryption-key` 생성
- 애플리케이션 시작 시 이 값을 환경변수로 주입하거나, Spring이 직접 읽게 구성

장점:
- 비밀값 회전, 접근 권한, 감사 추적이 쉬움
- 운영자별 IAM 통제가 가능

권장 키 이름 예시:
- `seal-lab/dev/mfa-secret-encryption-key`
- `seal-lab/staging/mfa-secret-encryption-key`
- `seal-lab/prod/mfa-secret-encryption-key`

### 3. KMS와의 관계

KMS는 보통 두 방식으로 쓴다.

- Secrets Manager 내부 암호화용
- 애플리케이션 데이터키 보호용

현재 프로젝트 수준에서는:
- Secrets Manager가 값을 저장
- 그 비밀은 KMS로 보호

이 조합이면 충분하다.

즉 지금 당장 `MfaSecretCryptoService`를 KMS API 직접 호출형으로 바꿀 필요는 없다.

## 추천 실배포 형태

1. `application.properties`

```properties
mfa.secret-encryption-key=${MFA_SECRET_ENCRYPTION_KEY}
```

2. AWS에서 `MFA_SECRET_ENCRYPTION_KEY`를 환경변수로 주입
   현재 확정 경로는 `EC2 systemd EnvironmentFile`

또는

3. Secrets Manager 값을 배포 단계에서 환경변수로 주입

## 키 교체 시 주의

이 키는 단순 Discord webhook 같은 값과 다르다.

이유:
- 기존 `enc:v1:...` 데이터 복호화에 사용되기 때문

키를 바꾸면 생길 수 있는 일:
- 저장된 기존 MFA secret을 더 이상 복호화 못 함
- 관리자 MFA 검증 실패 가능

그래서 키 교체는 아래 중 하나가 필요하다.

- 모든 관리자에게 MFA 재등록 공지 후, 기존 secret 폐기
- 또는 이전 키로 복호화 후 새 키로 재암호화하는 마이그레이션 작업

현재 프로젝트에선 아직 `키 회전 마이그레이션` 로직이 없다.
즉 운영 중 키를 바꾸면 `관리자 MFA 재등록 절차`가 필요하다고 보는 편이 안전하다.

## 권한 관리 원칙

- 운영 비밀값은 최소 운영자만 접근
- 개발자는 dev 키만 접근
- prod 키는 별도 IAM role로 제한
- Discord webhook과 달리 이 키는 MFA 복구 난이도에 직접 영향

## 현재 권장 후속 작업

1. EC2 `systemd` 서비스 유닛에 `EnvironmentFile=/etc/seal-lab/secrets.env` 반영
2. 운영 서버에 `/etc/seal-lab/secrets.env` 생성 및 권한 `600` 적용
3. 장기적으로는 Secrets Manager 값을 배포 단계에서 주입하는 방식으로 승격 검토
4. 키 회전 시 MFA 재등록 runbook 작성
