# conversation-summary-v1 (daily / weekly / monthly)

`TaskType.DAILY_SUMMARY` / `WEEKLY_SUMMARY` / `MONTHLY_SUMMARY` — 압축 체인입니다.

```
conversation_message ×N  → (daily-summary-v1)   → daily_conversation_summary
daily_conversation_summary ×7 → (weekly-summary-v1) → weekly_conversation_summary
weekly_conversation_summary ×4 → (monthly-summary-v1) → monthly_conversation_summary
```

세 단계 모두 "여러 텍스트를 받아 텍스트 하나로 압축한다"는 동일한 구조라 한
문서로 묶었습니다. `schemaVersion`만 단계별로 다릅니다. [공통 규칙](./README.md#공통-규칙)을
함께 따릅니다.

## 입력

### daily-summary-v1

```json
{
  "summaryDate": "2026-08-19",
  "conversations": [
    {
      "conversationId": 123,
      "type": "CALL",
      "messages": [
        { "role": "USER", "content": "..." },
        { "role": "ASSISTANT", "content": "..." }
      ]
    }
  ]
}
```

같은 날 여러 대화(`Conversation`)가 있을 수 있으므로 배열입니다. `conversationCount`,
`tokenCount`는 AI가 만드는 값이 아니라 저장 시점에 서비스 코드가 직접 계산합니다
(`conversations.length`, 메시지 `tokenCount` 합) — 출력 스키마에 포함하지 않습니다.

### weekly-summary-v1

```json
{
  "periodStart": "2026-08-17",
  "periodEnd": "2026-08-23",
  "dailySummaries": [
    { "date": "2026-08-17", "summary": "..." },
    { "date": "2026-08-18", "summary": "..." }
  ]
}
```

`dailySummaries`가 7개 미만이어도(결측일 존재) 받은 만큼만 넣어 요약합니다 — 7개
고정을 강제하지 않습니다.

### monthly-summary-v1

```json
{
  "periodStart": "2026-08-01",
  "periodEnd": "2026-08-31",
  "weeklySummaries": [
    { "periodStart": "2026-08-03", "summary": "..." }
  ]
}
```

## 출력

세 단계 모두 동일한 출력 형태이며 `schemaVersion` 값만 다릅니다.

```json
{
  "schemaVersion": "daily-summary-v1",
  "summary": "오늘은 아침 7시 30분에 일어나 컨디션이 좋다고 하셨고, 산책을 30분 다녀오셨습니다. 저녁에는 무릎이 약간 뻐근하다고 하셨습니다."
}
```

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `schemaVersion` | string | O | `daily-summary-v1` / `weekly-summary-v1` / `monthly-summary-v1` 중 호출한 단계에 해당하는 값 |
| `summary` | string | O | 압축된 요약 본문. 최소 1자 이상 — 빈 문자열 금지(아래 "내용 없음" 참고) |

### 길이 권장치 (강제 검증 아님, 프롬프트 가이드용)

| 단계 | 권장 길이 |
|---|---|
| daily | 200~500자 |
| weekly | 400~800자 |
| monthly | 600~1200자 |

각 단계는 이전 단계 요약을 다시 압축하는 것이므로 입력이 짧으면(예: 하루 대화가
아주 짧았던 날) 그보다 짧아도 됩니다. 하한을 강제하지 않습니다.

## 내용 없음 (빈 결과 대신 최소 요약)

이 세 단계는 `health-extraction-v1`과 달리 **빈 배열 같은 "없음" 상태가 없습니다.**
대화가 있었다면(`conversations`/`dailySummaries`/`weeklySummaries`가 비어있지 않다면)
반드시 1자 이상의 `summary`를 생성합니다. 대화 내용이 인사말 수준으로 빈약해도
"짧은 안부 통화였고 특이사항은 없었습니다"처럼 사실대로 요약합니다.

호출하는 쪽(서비스 계층)이 애초에 `conversations`/`dailySummaries`/`weeklySummaries`가
빈 배열인 채로 AI를 호출하지 않는 것이 원칙입니다 — 그 경우는 AI 작업 자체를
만들지 않습니다.

## 프롬프트 초안 (daily 기준, weekly/monthly는 입력 단위만 교체)

```
당신은 노인 돌봄 대화 기록을 압축하는 요약기입니다.

날짜: {summaryDate}
대화 내용:
{conversations를 "화자: 내용" 형식으로 시간순 나열}

규칙:
1. 사실만 요약하세요. 대화에 없는 감정이나 조언을 지어내지 마세요.
2. 200~500자 사이로 요약하세요.
3. 건강 상태, 활동, 기분 변화가 언급됐다면 우선적으로 포함하세요.
4. 아래 JSON 스키마와 정확히 같은 형태로, 다른 텍스트 없이 JSON만 출력하세요.

{daily-summary-v1 출력 스키마 삽입}
```

weekly/monthly는 1번 규칙의 "대화 내용" 대신 "일일 요약 목록"/"주간 요약 목록"을
입력으로 주고, 2번 길이 기준만 위 표의 값으로 바꿉니다.
