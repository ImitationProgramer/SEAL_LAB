# Proxy Header Deployment Notes

작성일: 2026-05-18

## 목적

SEAL_LAB는 IP 기반 위치 판정과 Trust Debug 표시를 사용하므로, 프록시 뒤에 배포될 때 실제 클라이언트 IP를 일관되게 복원해야 한다.

현재 반영된 사항:
- `ForwardedHeaderFilter` 활성화
- `server.forward-headers-strategy=framework`
- 애플리케이션 내부 `ClientIpResolver`가 `X-Forwarded-For`, `X-Real-IP` 등을 우선 해석

관련 파일:
- `src/main/java/com/seal/seal_lab/infra/config/ProxyHeaderConfig.java`
- `src/main/java/com/seal/seal_lab/infra/web/ClientIpResolver.java`
- `src/main/resources/application.properties`
- `src/main/resources/application.properties.sample`

## 신뢰 경계 원칙

애플리케이션은 아무 클라이언트가 보내는 `X-Forwarded-For`를 무조건 신뢰하면 안 된다.

배포 시 원칙:
- 인터넷에서 앱 서버로 직접 접근하지 않도록 막고, 반드시 `Nginx`/`ALB`/`Ingress` 같은 신뢰 프록시를 앞단에 둔다.
- `X-Forwarded-For`, `X-Forwarded-Proto`, `X-Real-IP`는 오직 신뢰 프록시가 덮어써서 넣게 한다.
- 앱 서버는 프록시 내부망에서만 접근되게 보안 그룹/방화벽으로 제한한다.

즉:
- `헤더 신뢰 범위 = 프록시 내부망`
- `직접 외부 접근 = 차단`

## 권장 프록시 설정 예시

### Nginx

```nginx
proxy_set_header Host $host;
proxy_set_header X-Real-IP $remote_addr;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
```

### 클라우드 LB / Ingress

- AWS ALB, GCP LB, Kubernetes Ingress를 쓰는 경우 기본 forwarded header 정책을 확인한다.
- 중간 프록시가 여러 단계면 `X-Forwarded-For` 맨 앞 IP가 실제 클라이언트인지 검증한다.

## 운영 체크리스트

- `Trust Debug`의 현재 IP가 프록시 IP가 아니라 실제 사용자 공인 IP로 보이는지 확인
- 로그인 성공 로그와 `ZTA-PDP` 로그의 IP가 동일 기준으로 남는지 확인
- 프록시를 거치지 않은 직접 접근이 막혀 있는지 확인
- `X-Forwarded-For`를 임의로 조작한 외부 요청이 앱까지 직접 들어오지 않는지 확인

## 남은 보완 가능 항목

- 프록시 내부망 CIDR 기준으로만 forwarded header를 신뢰하는 커스텀 정책
- 환경별 `trusted proxy` 목록을 설정값으로 분리
- Discord 보안 로그에도 `resolved client IP`와 `proxy chain` 일부를 함께 남길지 검토
