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

인프라 구성과 배포 절차는 [`docs/process-and-roles.md`](docs/process-and-roles.md)에 있습니다.

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

### 운영 Swagger는 기본으로 닫혀 있습니다

`application-prod.yml`이 `springdoc`을 `${SWAGGER_ENABLED:false}`로 잠가 뒀습니다. 인증이 없는
상태로 열어두면 전체 엔드포인트가 공개되고 "Try it out"이 외부인에게 쓰기 폼이 되기 때문입니다.

프론트에 공유해야 할 때만 EC2의 `/etc/hackathon.env`에 `SWAGGER_ENABLED=true`를 넣고
서비스를 재시작합니다. **이걸 안 하고 링크만 주면 프론트는 404를 받습니다.**

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
| 프론트가 Swagger 링크에서 404 | 운영은 `SWAGGER_ENABLED=true` 없이는 닫혀 있습니다 |

운영 서버 쪽 문제는 `journalctl -u hackathon -n 100`부터 봅니다.
그 외 배포·인프라 이슈는 [`docs/process-and-roles.md`](docs/process-and-roles.md)의
"막히기 쉬운 지점" 표에 정리돼 있습니다.
