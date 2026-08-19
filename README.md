# HEY — 백엔드

중앙해커톤 1팀 백엔드입니다. AI가 매일 정해진 시각에 먼저 연락해 대화를 나누고,
그 대화에서 건강 정보를 추출해 기록과 리포트로 만들어 주는 서비스입니다.

| 항목 | 값 |
|---|---|
| JDK | **17**  |
| Spring Boot | **4.1.0** |
| 빌드 | Gradle (wrapper 사용) |
| DB | 운영 RDS MySQL 8 / 로컬 H2 또는 MySQL |
| 산출물 | `build/libs/app.jar` |
| 배포 주소 | `http://52.79.79.220:8080` |
| **API 명세 (swagger)** | **http://52.79.79.220:8080/swagger-ui/index.html** |

인프라 구성과 배포 절차는 [`docs/process-and-roles.md`](docs/process-and-roles.md)에 있습니다.
프론트에 넘겨줘야하는 것들은 [프론트에 API 명세 넘기기](#프론트에-api-명세-넘기기)에 있습니다.

---

## ⚠️ 엔티티를 고칠 때

현재 운영 프로파일이 `ddl-auto: update`입니다. 
즉 **엔티티를 고쳐서 main에 머지하면 운영 DB
스키마가 그대로 따라 바뀝니다.**

- 컬럼 **추가**는 됩니다.
- 컬럼 **삭제 / 이름 변경 / 타입 축소는 XX.** 필드를 지우면 운영 DB에 유령 컬럼이
  남고, 그 컬럼이 `NOT NULL`이면 **이후 모든 INSERT가 실패합니다.**

따라서 필드를 지우거나 이름을 바꾸거나 타입을 바꿨다면, **PR에 마이그레이션 SQL을 함께 적고**
머지 전에 팀에 공유해주세요

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

| 주소 | 용도 |
|---|---|
| http://localhost:8080/swagger-ui/index.html | Swagger UI (로컬은 기본 ON) |
| http://localhost:8080/v3/api-docs | OpenAPI 스펙 JSON |

A 방식에서 데이터를 확인해야 하면 `show-sql` 로그를 읽거나 조회 API를 직접 호출합니다.
**테이블을 눈으로 봐야 하면 B(로컬 MySQL)로 가는 것이 맞습니다.**

---

## 프론트에 API 명세 넘기기

전체 API는 Swagger로 문서화돼 있습니다.

### 프론트에 줄 주소

```
http://52.79.79.220:8080/swagger-ui/index.html
```

**지금 열려 있고 별도 설정이 필요 없습니다.** 브라우저에서 바로 열립니다.
`servers`가 배포 주소로 자동 설정되므로 **"Try it out"도 그대로 동작합니다.**

베이스 URL은 `http://52.79.79.220:8080`입니다

| 주소 | 용도 |
|---|---|
| `http://52.79.79.220:8080/swagger-ui/index.html` | **프론트에 주는 링크** |
| `http://52.79.79.220:8080/v3/api-docs` | OpenAPI 스펙 JSON (codegen용) |
| `http://52.79.79.220:8080/health` | 서버 상태 확인 |


### CORS — 브라우저에서 호출하려면

Swagger 링크와 별개 문제입니다. EC2의 `CORS_ALLOWED_ORIGINS`에 프론트 origin이 없으면
브라우저 호출이 차단됩니다. 이 값은 `/etc/hackathon.env`에 있어 **바꾸려면 SSH가 필요합니다.**

- 프론트가 `localhost:5173` / `localhost:3000`이면 **이미 들어 있습니다**(응답 헤더로 확인했습니다).
- Swagger UI 화면에서의 "Try it out"은 서버와 같은 origin이라 CORS와 무관하게 동작합니다.
- **와일드카드를 쓸 수 있습니다.** `CorsConfig`가 `allowedOriginPatterns`를 쓰므로
  `https://*.vercel.app` 같은 패턴이 통합니다. Vercel은 배포마다 프리뷰 주소가 바뀌어서
  (`https://<프로젝트>-git-<브랜치>-<팀>.vercel.app`) 정확히 일치하는 주소만 받으면
  배포할 때마다 설정을 고쳐야 합니다.
- **값 자체를 바꾸려면 여전히 SSH가 필요합니다.** 환경변수가 설정돼 있으면 그쪽이 이깁니다.
  SSH를 쓸 수 없는데 주소를 추가해야 하면, springdoc에서 했던 것처럼 `application-prod.yml`의
  환경변수 참조를 걷어내고 값을 직접 적으십시오. **인프라 담당자에게 반드시 공유해야 합니다.**
- 환경변수가 빠져도 앱은 뜹니다. 기본값(`localhost:5173`, `localhost:3000`)을 넣어 두었습니다.
  전에는 기본값이 없어서 이 변수가 빠지면 플레이스홀더가 풀리지 않아 **기동 자체가 실패**했습니다.

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

### 엔드포인트를 추가할 때

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
운영 서버 쪽 문제는 `journalctl -u hackathon -n 100`부터 봅니다.
그 외 배포·인프라 이슈는 [`docs/process-and-roles.md`](docs/process-and-roles.md)의
"막히기 쉬운 지점" 표에 정리돼 있습니다.
