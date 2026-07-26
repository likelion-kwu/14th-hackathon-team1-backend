# 백엔드 배포 전체 과정 및 역할 구분

프론트가 붙을 수 있는 백엔드를 AWS에 올려두는 것이 목표입니다. 작업 인원 2명, 하루 기준입니다.

담당자 칸은 비워 두었습니다. 킥오프에서 채웁니다.

## 구성

```
프론트(로컬 / Vercel) ──HTTP──> EC2:8080 (Spring Boot, systemd)
                                     │
                                     └──> RDS MySQL (프라이빗, EC2 SG만 허용)
```

| 항목 | 선택 | 이유 |
|---|---|---|
| 실행 | jar + systemd | Docker 세팅 시간을 아낍니다. 나중에 전환 가능합니다 |
| DB | RDS MySQL 8 (db.t3.micro) | 프리티어입니다. EC2를 지워도 데이터가 남습니다 |
| 프로토콜 | HTTP (8080) | 프론트를 로컬에서 돌리는 동안은 충분합니다 |
| CI/CD | GitHub Actions | main push 시 자동 배포합니다 |
| 리전 | ap-northeast-2 (서울) | 모든 리소스를 같은 리전에 만듭니다 |

**선행 조건**: 사용 가능한 AWS 계정입니다. 인프라 트랙 전체가 여기에 묶여 있습니다.

---

## 역할 분담

작업은 두 트랙으로 갈립니다. 아래 **0. 착수 전 합의 사항**만 먼저 맞추면 두 트랙은
서로를 기다리지 않고 병렬로 진행할 수 있고, 마지막 8번에서 합류합니다.

담당란은 킥오프에서 채웁니다.

### 인프라 트랙 (담당: ______)

| | 작업 | 상세 |
|---|---|---|
| A-1 | AWS 계정 준비, 리전 ap-northeast-2 고정 | 3장 |
| A-2 | 보안그룹 2개 생성 | 3-1 |
| A-3 | RDS MySQL 8 생성 | 3-2 |
| A-4 | EC2 생성 + 탄력적 IP + 초기 설정 | 3-3, 3-4 |
| A-5 | 수동 배포 1회 성공 | 4장 |
| A-6 | systemd 등록 | 5장 |
| A-7 | GitHub Actions 자동배포 | 7장 |

### 앱 트랙 (담당: ______)

| | 작업 | 상세 |
|---|---|---|
| B-1 | 프로젝트 초기화, jar 이름 고정, `/health` | 2장 |
| B-2 | 프로파일 분리 (공통/local/prod) | 6-1 |
| B-3 | CORS, 공통 응답 래퍼, 예외 핸들러, Swagger | 6-2 |
| B-4 | 샘플 CRUD (`Item`) | 6-3 |

### 공동

| | 작업 | 상세 |
|---|---|---|
| C-1 | 착수 전 합의 사항 확정 | 0장 |
| C-2 | 레포 준비 | 1장 |
| C-3 | 합류 및 최종 검증 | 8장 |

RDS 생성에 5~10분 걸리므로 A-3을 시작해 놓고 A-4와 systemd 파일 작성을 병행합니다.

---

## 0. 착수 전 합의 사항

이것만 맞추면 두 트랙을 병렬로 진행할 수 있습니다. 여기서 어긋나면 마지막 합류 단계에서
전부 터집니다.

**환경변수 이름**

```
DB_URL=jdbc:mysql://<rds-endpoint>:3306/hackathon?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=admin
DB_PASSWORD=<...>
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:3000
```

**버전** — JDK **17**, Spring Boot **4.1.0**입니다. 로컬과 서버가 다르면 jar가 서버에서 안 뜹니다.

**빌드 산출물** — `build/libs/app.jar` → EC2의 `/opt/hackathon/app.jar`입니다.
`build.gradle`에서 `bootJar { archiveFileName = 'app.jar' }`로 이름을 고정합니다. 그렇지 않으면
배포 스크립트에 버전 문자열이 섞여 버전 올릴 때마다 스크립트를 고쳐야 합니다.

| 항목 | 담당 |
|---|---|
| 위 4개 환경변수 이름 확정 | |
| JDK / Boot 버전 확정 | |
| jar 이름 고정 규칙 확정 | |

---

## 1. 레포 준비

백엔드 전용 레포 1개로 갑니다. 프론트는 별도 레포이고 Vercel이 직접 연결합니다.
같은 레포에 두면 프론트 push마다 백엔드 배포가 돌아서 `paths:` 필터가 필요해집니다.

| 작업 | 담당 |
|---|---|
| 레포 생성, 협업자 초대 | |
| `.gitignore` 작성 (`*.pem`, `.env`, `application-local.yml`, `build/`, `.gradle/`) | |
| 초기 커밋 + push | |

**검증**: GitHub 웹에서 파일이 보이고 `.pem` / `.env`가 올라가지 않았습니다.

---

## 2. 최소 앱 만들기

기능이 아니라 **배포할 대상 하나를 만드는 것**이 목적입니다. DB도 CRUD도 아직 붙이지 않습니다.

| 작업 | 담당 |
|---|---|
| Spring Initializr — Gradle / Java 17 / Boot 4.1.0 / **Spring Web만** | |
| `bootJar` 이름을 `app.jar`로 고정 | |
| `GET /health` — `status`, `serverTime` 반환 | |

`serverTime`은 타임존 설정이 살아있는지 확인하는 용도입니다. 배포 후에도 이 값 하나로
EC2/JVM 타임존이 제대로 잡혔는지 즉시 알 수 있습니다.

> **버전 주의**
> Initializr가 Boot 3.x를 더 이상 제공하지 않아 4.1.0으로 갑니다.
> 인터넷 배포 가이드는 대부분 3.x 기준이라 설정 문법이 다른 곳이 있습니다.
> `spring.jackson.serialization.write-dates-as-timestamps`를 넣으면 **기동에 실패합니다**
> (Boot 4의 Jackson 3에서 제거된 상수입니다). 날짜는 기본으로 ISO-8601로 나가므로 불필요합니다.

**검증**: `./gradlew bootJar` 산출물명이 정확히 `app.jar`이고, `java -jar`로 단독 실행해도
`/health`가 200과 현재 KST를 반환합니다.

---

## 3. AWS 리소스

### 3-1. 보안그룹 2개 (EC2/RDS보다 먼저)

리소스보다 먼저 만들어야 EC2/RDS 생성 화면에서 바로 선택할 수 있습니다.

`hackathon-ec2-sg`

| 유형 | 포트 | 소스 |
|---|---|---|
| SSH | 22 | 내 IP |
| 사용자 지정 TCP | 8080 | 0.0.0.0/0 |

`hackathon-rds-sg`

| 유형 | 포트 | 소스 |
|---|---|---|
| MYSQL/Aurora | 3306 | **`hackathon-ec2-sg`** (IP가 아니라 보안그룹 선택) |

3306 소스를 IP로 넣는 것이 가장 흔한 실수입니다. EC2에서 RDS로 붙을 때는 EC2의 사설 IP로
오기 때문에 공인 IP를 넣으면 연결이 타임아웃납니다.

### 3-2. RDS (생성에 5~10분 걸리므로 먼저 걸어둡니다)

- MySQL 8.x / 프리티어 템플릿 / db.t3.micro
- 마스터 사용자 `admin` (비밀번호에 `@ / :` 는 JDBC URL에서 문제되니 피합니다)
- 퍼블릭 액세스 **아니요**, 보안그룹 `hackathon-rds-sg`
- 추가 구성 > 초기 데이터베이스 이름 `hackathon` — 빼먹으면 빈 인스턴스만 생기고
  나중에 직접 `CREATE DATABASE`를 해야 합니다
- 마스터 비밀번호는 팀 공유 채널이 아니라 별도로 전달합니다

### 3-3. EC2 (RDS 생성 대기 중에 진행)

- Ubuntu 22.04 LTS / t2.micro
- 스토리지 **30GB gp3** — 기본 8GB로 두지 않습니다. 프리티어가 30GB까지 무료고,
  나중에 늘리려면 볼륨 수정 + 파일시스템 확장까지 해야 합니다
- 키페어 생성 → `.pem`을 `~/.ssh/`에 두고 `chmod 400` 합니다.
  프로젝트 폴더에 두지 않습니다 (커밋 사고 원천 차단)
- 보안그룹 `hackathon-ec2-sg`
- **탄력적 IP 할당 후 즉시 연결** — 연결하지 않으면 재시작마다 IP가 바뀌어 프론트 설정이
  깨집니다. 또한 할당만 하고 연결하지 않으면 과금됩니다

### 3-4. EC2 초기 설정

- JDK: `sudo apt update && sudo apt install -y openjdk-17-jdk`
- 타임존: `sudo timedatectl set-timezone Asia/Seoul` (기본 UTC라 안 하면 9시간 어긋납니다)
- 스왑 4GB (t2.micro는 메모리 1GB라 없으면 앱이 실행 중 갑자기 죽습니다)
- `mysql-client` 설치 후 RDS 연결 확인: `mysql -h <endpoint> -u admin -p`

| 작업 | 담당 |
|---|---|
| 3-1 보안그룹 | |
| 3-2 RDS | |
| 3-3 EC2 | |
| 3-4 초기 설정 + RDS 연결 확인 | |

**검증**: SSH 접속이 되고, EC2에서 RDS에 `mysql` 클라이언트로 접속됩니다.

---

## 4. 수동 배포 (자동화 전에 반드시 한 번)

**핵심 원칙은 수동으로 한 번 성공시킨 뒤에 자동화하는 것입니다.**
처음부터 Actions로 배포하면 실패했을 때 원인이 빌드/전송/실행/방화벽 중 어디인지 좁힐 수 없습니다.

| 작업 | 담당 |
|---|---|
| `scp`로 `app.jar` 전송 → `java -jar`로 직접 실행 | |
| 외부에서 `curl http://<탄력적IP>:8080/health` | |

**검증**: 로컬이 아닌 **외부**에서 200이 나옵니다. 여기서 실패하면 보안그룹 8080이 안 열렸거나
`server.address`가 잘못 들어간 것입니다.

---

## 5. systemd 등록

| 작업 | 담당 |
|---|---|
| `sudo mkdir -p /opt/hackathon` | |
| `/etc/hackathon.env` 작성 (환경변수 4개) + `chmod 600` | |
| `/etc/systemd/system/hackathon.service` 작성 | |
| `daemon-reload && enable && start` | |

```ini
[Unit]
Description=Hackathon Backend
After=network.target

[Service]
Type=simple
User=ubuntu
EnvironmentFile=/etc/hackathon.env
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /opt/hackathon/app.jar --spring.profiles.active=prod
SuccessExitStatus=143
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=hackathon

[Install]
WantedBy=multi-user.target
```

`-Xmx512m`은 t2.micro에서 필수입니다. 지정하지 않으면 JVM이 힙을 크게 잡고 OOM으로 죽습니다.

**검증**: `systemctl status hackathon`이 active이고, EC2를 재부팅해도 자동으로 뜹니다
(`enable`을 실제로 했는지 검증하는 단계입니다).

---

## 6. DB 연결 + 프론트 연동 준비

### 6-1. 프로파일 분리

- `application.yml` — 공통입니다. Jackson 타임존을 여기 넣습니다. 빼면 API 응답 시각이
  UTC로 나갑니다

```yaml
spring:
  jackson:
    time-zone: Asia/Seoul
```

- `application-local.yml` — 각자 로컬 MySQL 또는 H2입니다
- `application-prod.yml` — 값을 하드코딩하지 않고 전부 환경변수를 참조합니다

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update   # 배포 파이프라인 구축 기간 한정
```

`server.address: 127.0.0.1`은 **넣지 않습니다.** Nginx를 세운 뒤에야 의미가 있는 설정이고,
지금 넣으면 외부에서 8080 접근이 막혀 배포 검증이 실패합니다.

**`ddl-auto` 전환 규칙** — 엔티티가 하나뿐인 지금은 `update`로 갑니다. 본 기획에 들어가
여러 명이 엔티티를 동시에 수정하기 시작하면 `validate`로 바꾸고, "엔티티 변경 PR에는
마이그레이션 SQL 첨부"를 팀 규칙으로 정합니다. 여러 명이 `update`로 같은 운영 DB에 붙으면
스키마가 꼬입니다.

### 6-2. 프론트 연결에 필요한 것

| 작업 | 담당 |
|---|---|
| `CorsConfig` — origin을 `${CORS_ALLOWED_ORIGINS}`에서 읽기. `allowedMethods`에 **OPTIONS 포함** | |
| `GET /health` — DB 접속까지 확인 | |
| 공통 응답 래퍼 `ApiResponse<T>` | |
| `@RestControllerAdvice` 예외 핸들러 — 최소 400/404/500 | |
| Swagger (springdoc-openapi) | |

CORS origin을 코드에 박아두면 프론트 배포 주소가 정해질 때마다 재빌드해야 합니다.
`OPTIONS`가 빠지면 GET은 되는데 POST만 CORS 에러가 납니다.

### 6-3. 샘플 CRUD

| 작업 | 담당 |
|---|---|
| 엔티티 1개 (`Item` — id, name, createdAt) | |
| `POST` / `GET` 목록 / `GET` 단건 / `DELETE` `/api/items` | |

목적은 기능이 아니라 **RDS 연결과 프론트 호출 경로가 실제로 뚫렸다는 증명**입니다.
본 기획이 정해지면 이 엔티티는 지웁니다.

---

## 7. GitHub Actions 자동화

| 작업 | 담당 |
|---|---|
| Secrets 등록: `EC2_HOST`(탄력적 IP), `EC2_USER`(ubuntu), `EC2_SSH_KEY`(.pem 전문) | |
| `.github/workflows/deploy.yml` 작성 | |
| 배포 계정에 재시작 권한 부여 (sudoers NOPASSWD, 해당 명령만) | |

워크플로 흐름입니다.

1. `./gradlew bootJar`
2. `scp build/libs/app.jar` → `/opt/hackathon/app.jar`
3. `ssh sudo systemctl restart hackathon`
4. `curl http://<host>:8080/health` 스모크 테스트 (실패 시 워크플로 실패 처리)

**4번을 빼면 "배포는 성공했는데 앱은 죽어있는" 상태를 못 잡습니다. 반드시 넣습니다.**

**검증**: main에 push해서 자동 배포가 끝까지 돕니다.

---

## 8. 합류 및 최종 검증 (2인 공동)

| 확인 | 담당 |
|---|---|
| main 머지 → Actions 자동 배포 성공 | |
| `curl http://<탄력적IP>:8080/health` → 200 | |
| `curl -X POST .../api/items` → `GET`으로 방금 넣은 데이터 확인 | |
| **브라우저 콘솔**에서 fetch로 GET과 POST 둘 다 → CORS 에러 없음 | |
| 응답 `createdAt`이 KST인지, 한글이 `???`로 깨지지 않는지 | |
| Swagger UI 접속 | |
| EC2 재부팅 후 자동 기동 | |
| 프론트 담당자에게 API 베이스 URL / Swagger 주소 전달, 프론트 배포 주소 요청 | |

CORS는 curl로 재현되지 않으므로 **반드시 브라우저에서** 확인합니다. POST를 해야 preflight가
검증됩니다.

---

## 9. 이후 (프론트 배포 주소 확정 후)

HTTPS는 프론트를 Vercel 같은 HTTPS 호스팅에 올리는 순간 필요해집니다. 브라우저가 HTTPS
페이지에서 HTTP API 호출을 차단하기 때문입니다.

| 작업 | 담당 |
|---|---|
| 도메인 확보 (가비아/Route53 또는 duckdns) | |
| Nginx 리버스 프록시 (80 → 8080) | |
| Certbot으로 Let's Encrypt 발급, 443 개방 | |
| 보안그룹에서 8080 인바운드 제거 | |
| `application-prod.yml`에 `server.address: 127.0.0.1` 추가 | |
| `CORS_ALLOWED_ORIGINS`에 프론트 도메인 추가 후 서비스 재시작 | |
| Vercel 프리뷰 도메인 대응을 위해 `allowedOriginPatterns`로 전환 | |

레포를 조직으로 이전(transfer)하는 경우, 이전 직후 Actions를 한 번 돌려
Secrets가 살아있는지 확인합니다. 유실됐으면 재등록합니다.

---

## 막히기 쉬운 지점

| 증상 | 원인 |
|---|---|
| RDS 연결 타임아웃 | RDS 보안그룹 인바운드 소스를 IP로 넣었습니다. EC2 보안그룹 ID로 지정해야 합니다 |
| Actions에서 SSH 실패 | `.pem`을 Secret에 넣을 때 `-----BEGIN`부터 마지막 줄바꿈까지 전문을 넣지 않았습니다 |
| 배포는 성공인데 응답 없음 | `journalctl -u hackathon -n 100`부터 봅니다. 환경변수 누락이 흔합니다 |
| CORS 에러 | curl로는 재현되지 않습니다. 반드시 브라우저에서 확인합니다 |
| 재시작 후 IP 변경 | 탄력적 IP를 연결하지 않았습니다 |
| 로컬 curl은 되는데 외부만 실패 | `server.address: 127.0.0.1`이 들어갔거나 보안그룹 8080이 미개방입니다 |
| 앱이 로그도 없이 죽음 | 메모리 부족입니다. `dmesg \| grep -i oom`으로 확인합니다. 스왑과 `-Xmx512m`을 함께 적용했는지 점검합니다 |
| GET은 되는데 POST만 CORS 에러 | preflight(OPTIONS)를 허용하지 않았습니다 |
| 시각이 9시간 어긋남 | 타임존 세 군데(EC2 OS / `spring.jackson` / `DB_URL`의 `serverTimezone`) 중 누락이 있습니다 |
| 앱 기동 실패 | Boot 4에서 제거된 `write-dates-as-timestamps`를 설정에 넣었습니다 |
