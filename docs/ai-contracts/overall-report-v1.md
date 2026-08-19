# overall-report-v1

`TaskType.OVERALL_REPORT` — 월간 요약 전체와 건강 기록 통계를 받아 `OverallReport`
1건(회원당 1개, 재생성 시 덮어씀)을 만듭니다. [공통 규칙](./README.md#공통-규칙)을
함께 따릅니다.

## 입력

```json
{
  "periodStart": "2026-01-01",
  "periodEnd": "2026-08-31",
  "monthlySummaries": [
    { "periodStart": "2026-01-01", "summary": "..." },
    { "periodStart": "2026-02-01", "summary": "..." }
  ],
  "healthStats": [
    { "type": "SLEEP", "recordCount": 45, "avgValue": 6.8, "avgUnit": "hours" },
    { "type": "MEAL", "recordCount": 90, "avgValue": null, "avgUnit": null },
    { "type": "EXERCISE", "recordCount": 20, "avgValue": 25, "avgUnit": "min" }
  ]
}
```

- `monthlySummaries`는 `monthly_conversation_summary` 전체(존재하는 만큼)를
  오래된 순으로 나열합니다.
- `healthStats`는 기록이 1건 이상 있는 `HealthType`만 포함합니다. `avgValue`는
  숫자로 평균을 낼 수 있는 타입(`SLEEP.hours`, `EXERCISE.duration`,
  `WATER.amount`)만 채우고, 그 외(`MEAL`, `SKIN`, `MOOD`, `OTHER`)는 `null`입니다.
  이 집계는 AI가 아니라 저장 시점에 서비스 코드가 `HealthRecord`를 조회해 계산합니다.

## 출력

```json
{
  "schemaVersion": "overall-report-v1",
  "summary": "지난 8개월간 전반적으로 규칙적인 수면 패턴을 유지하셨습니다. 특히 최근 두 달은 평균 수면 시간이 늘어 컨디션에 대한 언급도 긍정적으로 바뀌었습니다. 다만 운동 기록은 다소 뜸해진 편입니다.",
  "detail": {
    "period": { "from": "2026-01-01", "to": "2026-08-31" },
    "highlights": [
      "평균 수면 시간이 1월 6.1시간에서 8월 6.8시간으로 늘었습니다.",
      "운동 기록 빈도가 최근 두 달 줄었습니다."
    ],
    "healthTrends": [
      { "type": "SLEEP", "trend": "improving", "note": "평균 수면 시간이 점차 증가하는 추세입니다." },
      { "type": "EXERCISE", "trend": "worsening", "note": "최근 기록 빈도가 줄었습니다." }
    ],
    "recommendations": [
      "가벼운 산책 등 운동 기록을 다시 늘려보시는 건 어떨까요?"
    ]
  }
}
```

### 필드

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `summary` | string | O | 사용자에게 보여줄 전체 보고서 본문(자연어). `OverallReport.summary`에 저장 |
| `detail` | object | O | 프론트엔드용 구조화 데이터. `OverallReport.detail`(TEXT, JSON 문자열)에 저장 |
| `detail.period` | object | O | `{from, to}` — 입력의 `periodStart`/`periodEnd`를 그대로 반영 |
| `detail.highlights` | string[] | O | 2~5개, 구체적 수치나 변화가 드러난 문장. 빈 배열 가능(데이터 부족 시) |
| `detail.healthTrends` | array | O | 입력 `healthStats`에 있던 타입만 포함. 없던 타입을 지어내지 않음 |
| `detail.healthTrends[].trend` | enum | O | `"improving"\|"stable"\|"worsening"\|"unclear"` — 아래 판단 기준 |
| `detail.healthTrends[].note` | string | O | 추세 판단 근거 한 줄 |
| `detail.recommendations` | string[] | O | 1~3개, 관찰된 사실에 근거한 제안. 빈 배열 가능 |

### `trend` 판단 기준

- `improving` / `worsening` — `monthlySummaries` 또는 `healthStats` 흐름상 방향이
  뚜렷할 때만. 최소 3개월 이상 데이터가 있어야 판단.
- `stable` — 큰 변화 없이 유지되는 경우.
- `unclear` — 데이터가 3개월 미만이거나 방향이 뒤섞여 있어 추세를 단정할 수 없는
  경우. 데이터가 적다고 `improving`/`worsening`을 임의로 고르지 않습니다.

## 안전 규칙 (필수)

- **의학적 진단이나 병명을 언급하지 않습니다.** "수면무호흡증 의심" 같은 진단성
  표현 금지. "수면 시간이 짧아지는 추세입니다"처럼 관찰된 사실만 서술합니다.
- **`recommendations`는 생활 습관 수준의 제안으로 한정합니다.** 약물, 치료,
  병원 방문을 지시하는 문장은 만들지 않습니다 (필요하면 "불편하시면 병원에 한번
  들러보시는 것도 좋겠습니다" 정도의 완곡한 권유까지만).
- `healthStats`에 없는 `HealthType`을 `healthTrends`에 만들어내지 않습니다.

## 데이터 부족

`monthlySummaries`와 `healthStats`가 모두 비어 있는 상태로는 이 작업을 호출하지
않는 것이 원칙입니다(서비스 계층 책임). 데이터가 1개월 치뿐이라 추세 판단이
불가능한 경우에는 `highlights`만 채우고 `healthTrends`는 전부 `"unclear"`로,
`recommendations`는 빈 배열로 반환합니다 — 실패가 아니라 정상 처리입니다.

## 프롬프트 초안

```
당신은 노인 돌봄 서비스의 건강 종합 리포트를 작성하는 어시스턴트입니다.

기간: {periodStart} ~ {periodEnd}
월간 요약:
{monthlySummaries를 날짜순으로 나열}
건강 기록 통계:
{healthStats를 표 형태로 나열}

규칙:
1. 절대 의학적 진단을 내리지 마세요. 관찰된 사실만 서술하세요.
2. 추세(trend)는 최소 3개월 이상 데이터가 뒷받침될 때만 improving/worsening으로
   판단하고, 그렇지 않으면 unclear로 표시하세요.
3. healthStats에 없는 건강 타입에 대해 언급을 만들어내지 마세요.
4. recommendations는 생활 습관 수준의 완곡한 제안만 담고, 진단·처방성 표현은
   피하세요.
5. 아래 JSON 스키마와 정확히 같은 형태로, 다른 텍스트 없이 JSON만 출력하세요.

{overall-report-v1 출력 스키마 삽입}
```
