# HEY — 백엔드

중앙해커톤 1팀 백엔드입니다. AI가 매일 정해진 시각에 먼저 연락해 대화를 나누고,
그 대화에서 건강 정보를 추출해 기록과 리포트로 만들어 주는 서비스입니다.

| 항목 | 값 |
|---|---|
| JDK | **17** (고정 — 서버와 다르면 jar가 안 뜹니다) |
| Spring Boot | **4.1.0** |
| 빌드 | Gradle (wrapper 사용) |
| DB | 운영 RDS MySQL 8 / 로컬 H2 또는 MySQL |
| 산출물 | `build/libs/app.jar` |
| 배포 주소 | `http://52.79.79.220:8080` |
| **API 명세 (프론트에 주는 링크)** | **http://52.79.79.220:8080/swagger-ui/index.html** |

인프라 구성과 배포 절차는 [`docs/process-and-roles.md`](docs/process-and-roles.md)에 있습니다.
프론트에 넘길 때 함께 알려야 할 것은 [프론트에 API 명세 넘기기](#프론트에-api-명세-넘기기)에 있습니다.

---

## ⚠️ 먼저 읽어야 하는 것 — 이미 배포되어 있습니다

**`main`에 머지되는 순간 GitHub Actions가 EC2로 자동 배포합니다.** (`.github/workflows/deploy.yml`)
빌드 → 테스트 → jar 전송 → 서비스 재시작 → `/health` 스모크 테스트까지 자동으로 돕니다.

그래서 아래 3개는 반드시 지킵니다.

1. **`main`에 직접 push하지 않습니다.** 항상 브랜치를 따고 PR로 올립니다.
   내 로컬에서 안 돌아가는 코드가 main에 들어가면 **배포된 서비스가 내려갑니다.**
2. **PR 올리기 전에 로컬에서 `./gradlew build`가 통과하는지 확인합니다.**
   CI가 `build`(테스트 포함)를 돌리므로, 로컬에서 깨지면 CI에서도 깨집니다.
3. **엔티티를 고쳤으면 PR 본문에 그 사실을 적습니다.** 이유는 바로 아래에 있습니다.

### 엔티티를 고칠 때 (중요)

운영 프로파일이 `ddl-auto: update`입니다. 즉 **엔티티를 고쳐서 main에 머지하면 운영 DB
스키마가 그대로 따라 바뀝니다.** 그리고 `update`에는 함정이 있습니다.

- 컬럼 **추가**는 됩니다.
- 컬럼 **삭제 / 이름 변경 / 타입 축소는 하지 않습니다.** 필드를 지우면 운영 DB에 유령 컬럼이
  남고, 그 컬럼이 `NOT NULL`이면 **이후 모든 INSERT가 실패합니다.**

따라서 필드를 지우거나 이름을 바꾸거나 타입을 바꿨다면, **PR에 마이그레이션 SQL을 함께 적고**
머지 전에 팀에 공유합니다. 혼자 판단해서 머지하지 않습니다.

### 운영 Swagger는 해커톤 동안 열려 있습니다

`application-prod.yml`을 `${SWAGGER_ENABLED:false}` → **`enabled: true` 고정**으로 바꿨습니다.
방침이 바뀐 게 아니라 **접근 수단** 때문입니다. 닫혀 있는 동안 Swagger를 켜려면 EC2의
`/etc/hackathon.env`를 고쳐야 하는데 **지금 팀에 pem 키를 가진 사람이 없습니다.**
반면 이 파일은 main에 머지하면 GitHub Actions가 알아서 배포하므로 SSH 없이 반영됩니다.

**환경변수 참조를 아예 걷어낸 이유가 중요합니다.** 기본값만 `true`로 바꾸면, 인프라 세팅 때
`/etc/hackathon.env`에 `SWAGGER_ENABLED=false`가 이미 들어가 있는 경우 환경변수가 이겨서
그대로 닫혀 있습니다. 그리고 그걸 고치려면 다시 SSH가 필요합니다. 레포에서는 그 파일 내용을
확인할 수 없으므로 값을 고정했습니다.

**열려 있는 동안의 위험은 알고 있어야 합니다.** 전체 엔드포인트와 필드가 공개되고,
"Try it out"이 외부인에게 쓰기 요청 폼이 됩니다. **심사가 끝나면 이 값을 `false`로 되돌려
머지하십시오.**

---

## 로컬 실행

### 1. 설정 파일 만들기

이 파일이 없으면 앱이 뜨지 않습니다 (`Failed to configure a DataSource`).

```bash
cp config/application-local.yml.example config/application-local.yml
```

Windows PowerShell:

```powershell
Copy-Item config/application-local.yml.example config/application-local.yml
```

`config/application-local.yml`은 `.gitignore`에 있어 **커밋되지 않습니다.** 각자 자기 값으로
채웁니다. `src/main/resources`에 두지 마십시오 — jar 안으로 패키징돼서 로컬 DB 자격증명이
배포 산출물에 실립니다.

### 2. DB 고르기 — 둘 중 하나

복사한 예제 파일의 기본값은 **A(H2)** 입니다. 그대로 두면 설치할 것이 없습니다.

| | 방식 | 언제 쓰는가 | 설치 |
|---|---|---|---|
| **A** | H2 인메모리 (기본값) | 앱 기동, API 응답 모양, Swagger 확인 | 없음 |
| **B** | 로컬 MySQL 8 | 스키마·쿼리를 실제로 검증할 때 | Docker 한 줄 |

**A를 쓸 때 주의** — 반드시 `./gradlew bootRun`으로 실행합니다. H2는 `developmentOnly`로
들어가 있어서 `bootJar` 산출물에는 포함되지 않습니다(운영 jar에 H2가 섞여 들어가는 사고를
막기 위한 의도된 설정입니다). `java -jar`로는 H2를 못 찾습니다.
그리고 **재시작하면 데이터가 사라집니다.**

**B를 쓸 때** — 예제 파일의 A 블록을 주석 처리하고 B 블록을 살린 뒤, MySQL을 띄웁니다.
아래 값은 예제 파일과 맞춰 두었습니다.

```bash
docker run -d --name hey-mysql -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=hackathon \
  -e MYSQL_USER=hackathon -e MYSQL_PASSWORD=1234 mysql:8
```

Windows PowerShell (한 줄):

```powershell
docker run -d --name hey-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=hackathon -e MYSQL_USER=hackathon -e MYSQL_PASSWORD=1234 mysql:8
```

`H2와 MySQL은 완전히 같지 않습니다.` JSON 컬럼 동작이 특히 달라서 `detail` 컬럼들이 TEXT로
정해졌습니다(`HealthRecord.detail` 주석 참고). **JSON·날짜·유니크 제약이 걸린 작업을 했다면
머지 전에 한 번은 B로 확인합니다.**

### 3. 실행

```bash
./gradlew bootRun
```

Windows에서는 `.\gradlew bootRun` 또는 `gradlew.bat bootRun`입니다.

프로파일을 안 넘기면 `local`로 뜹니다(`application.yml`의 `spring.profiles.default: local`).
기동 로그에 이렇게 나오면 정상입니다.

```
No active profile set, falling back to 1 default profile: "local"
```

### 4. 뜬 것 확인하기

```bash
curl http://localhost:8080/health
```

```json
{"status":"UP","serverTime":"2026-08-17T22:15:30.123+09:00","timeZone":"Asia/Seoul","database":"UP"}
```

- `database`가 `UP`이면 DB 접속까지 성공한 것입니다. `DOWN`이면 **503**이 떨어집니다
  (DB가 끊긴 서버를 정상으로 판정하지 않기 위한 의도입니다).
- `serverTime`에 `+09:00`이 붙어야 합니다.

| 주소 | 용도 |
|---|---|
| http://localhost:8080/swagger-ui/index.html | Swagger UI (로컬은 기본 ON) |
| http://localhost:8080/v3/api-docs | OpenAPI 스펙 JSON |

**H2 콘솔(`/h2-console`)은 없습니다.** Boot 4가 H2 콘솔 자동설정을 제거해서
`spring.h2.console.enabled`를 켜도 404입니다(Boot 4 jar에 `H2ConsoleAutoConfiguration`이
존재하지 않습니다). 인터넷 가이드는 대부분 Boot 3 기준이라 이 설정을 넣으라고 하는데,
넣어도 동작하지 않습니다.

A 방식에서 데이터를 확인해야 하면 `show-sql` 로그를 읽거나 조회 API를 직접 호출합니다.
**테이블을 눈으로 봐야 하면 B(로컬 MySQL)로 가는 것이 맞습니다.**

---

## 프론트에 API 명세 넘기기

전체 API는 Swagger로 문서화돼 있습니다. **엔드포인트 20개 전부 성공/실패 응답과 예시가 스펙에 실립니다.**

### 프론트에 줄 주소 — 이것 하나면 됩니다

```
http://52.79.79.220:8080/swagger-ui/index.html
```

**지금 열려 있고 별도 설정이 필요 없습니다.** 브라우저에서 바로 열립니다.
`servers`가 배포 주소로 자동 설정되므로 **"Try it out"도 그대로 동작합니다.**

베이스 URL은 `http://52.79.79.220:8080`입니다(이슈 #1의 인프라 검증 코멘트 기준).

| 주소 | 용도 |
|---|---|
| `http://52.79.79.220:8080/swagger-ui/index.html` | **프론트에 주는 링크** |
| `http://52.79.79.220:8080/v3/api-docs` | OpenAPI 스펙 JSON (codegen용) |
| `http://52.79.79.220:8080/health` | 서버 상태 확인 |

배포 서버 Swagger는 **항상 현재 코드와 일치합니다.** main에 머지되는 순간 자동 배포되면서
갱신되므로 따로 할 일이 없습니다.

### `docs/api-docs.json`은 왜 있나

프론트가 `orval`이나 `openapi-typescript`로 타입을 생성할 때 파일이 필요한 경우를 위한
**스냅샷**입니다. 레포가 public이라 raw URL로도 읽힙니다.

```
https://raw.githubusercontent.com/likelion-kwu/14th-hackathon-team1-backend/main/docs/api-docs.json
```

**API를 고치면 이 파일은 자동으로 갱신되지 않습니다.** 다시 뽑아서 커밋해야 합니다.

```bash
./gradlew bootRun          # 다른 터미널에서
curl -s http://localhost:8080/v3/api-docs -o docs/api-docs.json
```

커밋된 파일의 `servers`는 뽑아낸 환경 주소(`http://localhost:8080`)라 배포 서버를 가리키지
않습니다. 배포 주소를 실으려면 이렇게 뽑습니다.

```bash
SPRING_APPLICATION_JSON='{"app":{"api":{"public-url":"http://52.79.79.220:8080"}}}' ./gradlew bootRun
```

**이 값을 `config/application-local.yml`에 넣지 마십시오.** 넣으면 로컬 Swagger UI의
"Try it out"이 배포 서버로 요청을 보냅니다.

> 굳이 SwaggerHub 같은 외부 도구에 올릴 이유는 없습니다. 스냅샷이라 고칠 때마다
> 재임포트해야 하고, Try it out도 CORS로 막힙니다. 배포 서버 Swagger가 상위 호환입니다.

### CORS — 브라우저에서 호출하려면

Swagger 링크와 별개 문제입니다. EC2의 `CORS_ALLOWED_ORIGINS`에 프론트 origin이 없으면
브라우저 호출이 차단됩니다. 이 값은 `/etc/hackathon.env`에 있어 **바꾸려면 SSH가 필요합니다.**

- 프론트가 `localhost:5173` / `localhost:3000`이면 **이미 들어 있습니다**(응답 헤더로 확인했습니다).
- Swagger UI 화면에서의 "Try it out"은 서버와 같은 origin이라 CORS와 무관하게 동작합니다.
- 다른 origin이 필요하면 `application-prod.yml`의 `allowed-origins`에 기본값을 주는 방식으로
  SSH 없이 넓힐 수 있습니다(`${CORS_ALLOWED_ORIGINS:...}`). 팀에 공유하고 하십시오.

### ⚠️ HTTP입니다 — 프론트 배포 시 막힙니다

Swagger를 **보는 것**과 로컬 개발(`localhost`)에서 호출하는 것은 문제없습니다.
하지만 프론트를 **Vercel 같은 HTTPS 도메인에 배포하면 그 앱에서 `http://` API 호출이
브라우저에 차단됩니다**(mixed content). 프론트 배포 전에 HTTPS 전환이 필요합니다 —
[`docs/process-and-roles.md`](docs/process-and-roles.md) 9장에 후속 작업으로 잡혀 있습니다.

8080은 비표준 포트라 일부 학교·회사 네트워크에서 막힐 수 있습니다. 특정 팀원만 안 보이면
다른 네트워크에서 시도해 보라고 하십시오.

### 엔드포인트 목록

`구현` 열이 **스텁**이면 아직 로직이 없습니다. **고정 예시를 반환하고 DB를 읽거나 쓰지 않습니다.**
요청·응답 형태는 확정된 것이라 프론트가 지금 붙여도 나중에 고칠 필요가 없지만, **돌아오는 값은
실제 데이터가 아닙니다.**

| 태그 | 메서드 | 경로 | 구현 | 설명 |
|---|---|---|---|---|
| member | POST | `/api/members` | 스텁 | 회원 가입 (**201**, Location 헤더) |
| member | GET | `/api/members/{memberId}` | 스텁 | 회원 조회 |
| member | PATCH | `/api/members/{memberId}/notification` | 스텁 | 알림 시각·사용 여부 변경 |
| member | PUT | `/api/members/{memberId}/fcm-token` | 스텁 | FCM 토큰 등록 |
| engagement | GET | `/api/members/{memberId}/streak` | 스텁 | 스트릭 조회 (기록 없으면 0) |
| conversation | GET | `/api/conversations` | 스텁 | 대화 목록 (`date` 생략 시 전체 최신순) |
| conversation | POST | `/api/conversations` | 스텁 | 대화 시작 (오늘 진행 중이면 그것을 반환) |
| conversation | GET | `/api/conversations/{id}` | 스텁 | 대화 상세 |
| conversation | GET | `/api/conversations/{id}/messages` | 스텁 | 메시지 목록 (sequenceNo 오름차순) |
| conversation | POST | `/api/conversations/{id}/messages` | 스텁 | 메시지 전송 (사용자 발화 + AI 응답 함께 반환) |
| conversation | PATCH | `/api/conversations/{id}/complete` | 스텁 | 대화 종료 (멱등) |
| health-record | GET | `/api/health-records` | 스텁 | 기간 조회 (기본 최근 7일) |
| health-record | PATCH | `/api/health-records/{id}/confirm` | 스텁 | 사용자 확인 (멱등) |
| health-record | GET | `/api/health-records/today` | **완료** | 오늘(KST) 기록 |
| summary | GET | `/api/summaries/{daily,weekly,monthly,overall}` | **완료** | 요약·리포트 조회 |
| ai | GET | `/api/ai-analyses` | 스텁 | 분석 작업 상태 조회 |
| health | GET | `/health` | **완료** | 배포 스모크 (**공통 래퍼 없음**) |

`/api/items`는 `@Hidden`이라 스펙에 나오지 않습니다. 배포 검증용으로 코드만 남겨둔 것입니다.

### 프론트에게 함께 전달할 것

1. **분기는 HTTP 상태 코드가 아니라 `error.code`로 합니다.** 같은 400 안에 `VALIDATION_FAILED`,
   `MALFORMED_REQUEST`, `BAD_REQUEST`가 있습니다. 가능한 값은 스펙의 `ErrorResponse` enum에 전부 있습니다.
2. **회원 식별은 `memberId`입니다.** 해커톤 기간에는 로그인을 붙이지 않기로 했으므로 이 형태가
   그대로 유지됩니다. 가입 응답의 `id`를 보관했다가 씁니다.
3. **위 표의 "스텁"은 아직 고정 예시만 돌려줍니다.** 화면 레이아웃과 타입은 지금 확정할 수 있지만,
   실제 데이터가 필요한 검증은 구현 완료 후에 해야 합니다.

### 엔드포인트를 추가할 때 (중요)

성공 응답은 springdoc이 알아서 만들지만 **실패 응답은 그렇지 않습니다.**
`ApiErrorResponseCustomizer`가 `/api` 아래 모든 엔드포인트에 400과 500을 자동으로 붙이고,
나머지는 표시를 보고 붙입니다.

| 표시 | 붙는 것 | 언제 |
|---|---|---|
| (없음) | 400, 500 | 자동 — 아무것도 안 해도 됩니다 |
| `@ApiNotFound("...")` | 404 | 없을 수 있는 것을 찾는 엔드포인트 |
| `@ApiConflict("...")` | 409 | 유니크 제약이 걸린 값을 쓰는 엔드포인트 |
| `@ApiCreated` | 200 → 201 | `ResponseEntity.created(...)`로 201을 반환할 때 |

`@ApiCreated`를 빠뜨리면 **실제로는 201인데 문서에는 200으로 실립니다.** springdoc은
`ResponseEntity`의 실제 상태 코드를 알 수 없어 기본값을 씁니다.

DTO 필드에는 `@Schema(description = ..., example = ...)`를 붙입니다. **javadoc의 `@param`은
스펙에 실리지 않습니다.** springdoc이 javadoc을 읽지 않기 때문입니다.

JSON 원문을 그대로 내보내는 필드(`@JsonRawValue`)에는 `@Schema(implementation = Object.class)`를
씁니다. `type = "object"`는 record 컴포넌트에서 무시되고 `string`으로 실립니다.

`OpenApiConfigTest`가 위 규칙이 지켜졌는지 검사합니다. 엔드포인트를 추가하면 그 테스트의
목록에도 한 줄 추가합니다.



---

## 테스트

```bash
./gradlew test        # 테스트만
./gradlew build       # 컴파일 + 테스트 + jar (PR 올리기 전에 이걸 돌립니다)
```

**IDE 자체 JUnit 러너로 돌리지 마십시오.** `build.gradle`이 테스트에
`spring.config.location`을 지정해서 `config/`의 로컬 설정이 테스트 컨텍스트로 딸려 들어가는 것을
막고 있습니다. IDE 러너는 이 설정을 적용하지 않아 **사람마다 다르게 깨집니다.**
결과 판정은 항상 `./gradlew test`로 합니다.

테스트는 별도 설정 없이 H2로 돕니다(`testRuntimeOnly`). 그래서 **DDL은 H2 방언으로만 검증되고
MySQL 방언은 검증되지 않습니다.** 테스트가 통과했다는 것이 운영 스키마가 맞다는 뜻은 아닙니다.

실패한 테스트 리포트는 `build/reports/tests/test/index.html`에 있습니다.

---

## 브랜치와 담당

### 브랜치 규칙

```
main                    ← 직접 push 금지. 머지되면 자동 배포됩니다
  └─ feat/<범위>        ← 여기서 작업하고 PR
```

PR 템플릿(`.github/pull_request_template.md`)이 자동으로 붙습니다. 엔티티를 고쳤으면
"공유사항 to 리뷰어"에 반드시 적습니다.

### 패키지 소유권

같은 파일을 두 사람이 만지지 않도록 패키지 단위로 나눴습니다. **남의 패키지에 파일을 추가해야
하면 먼저 물어봅니다.**

| 패키지 | 담당 | 범위 |
|---|---|---|
| `member/`, `conversation/` | 승효 | 회원·대화 조회 API |
| `healthrecord/`, `summary/` | 건하 | 건강기록·요약·리포트 **조회** API |
| `ai/` | 나 | AI 연동, 건강정보 추출, JSON 스키마 검증 |
| `engagement/` | 나 | Streak, 스케줄러, Push 알림 |
| `common/`, `config/`, `health/` | 공통 | 고칠 일이 있으면 팀에 공유 |

**쓰기·읽기 경계** — 건강기록과 요약 테이블의 **쓰기는 AI 파이프라인(`ai/`)이** 담당하고,
**읽기는 각 도메인 패키지가** 담당합니다. 이 규칙은 `HealthRecordRepository`와
`DailyConversationSummaryRepository` 주석에도 적혀 있습니다.

---

## 막히면 여기부터

| 증상 | 원인 |
|---|---|
| `Failed to configure a DataSource: 'url' attribute is not specified` | `config/application-local.yml`을 안 만들었습니다. 1번으로 돌아갑니다 |
| H2로 띄웠는데 `Cannot load driver class: org.h2.Driver` | `java -jar`로 실행했습니다. H2는 `developmentOnly`라 `./gradlew bootRun`을 써야 합니다 |
| `/health`가 503, `database: DOWN` | B 방식인데 MySQL 컨테이너가 안 떠 있습니다. `docker ps`로 확인합니다 |
| MySQL인데 한글이 `???` | JDBC URL에 `characterEncoding=UTF-8`이 빠졌습니다 |
| 응답 시각이 9시간 어긋남 | JDBC URL의 `connectionTimeZone=Asia/Seoul`이 빠졌습니다 |
| IDE에선 통과하는데 `./gradlew test`는 실패 | IDE 러너가 `spring.config.location`을 적용하지 않습니다. Gradle 결과가 기준입니다 |
| 앱 기동 실패 (`write-dates-as-timestamps`) | Boot 4의 Jackson 3에서 제거된 설정입니다. 넣지 않습니다 |
| `/h2-console`이 404 | Boot 4가 H2 콘솔 자동설정을 제거했습니다. 설정으로 살릴 수 없습니다 |
| 필드를 지웠는데 운영에서 INSERT가 전부 실패 | `ddl-auto: update`는 컬럼을 삭제하지 않습니다. 유령 `NOT NULL` 컬럼이 남았습니다 |
| 프론트가 Swagger 링크에서 404 | 배포가 실패해 옛 jar가 돌고 있습니다. Actions 탭에서 마지막 deploy 결과를 확인합니다 |
| 프론트가 Swagger 링크에서 응답 없음 | 8080이 막힌 네트워크입니다. 다른 네트워크에서 확인합니다 |
| HTTPS 프론트에서 API 호출이 차단됨 | 서버가 HTTP라 mixed content로 막힙니다. HTTPS 전환이 필요합니다 |

운영 서버 쪽 문제는 `journalctl -u hackathon -n 100`부터 봅니다.
그 외 배포·인프라 이슈는 [`docs/process-and-roles.md`](docs/process-and-roles.md)의
"막히기 쉬운 지점" 표에 정리돼 있습니다.
