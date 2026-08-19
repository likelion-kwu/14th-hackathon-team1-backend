# health-extraction-v1

`TaskType.HEALTH_EXTRACTION` — 대화 하나에서 건강 관련 사실을 뽑아 `HealthRecord`
후보 목록으로 만듭니다. [공통 규칙](./README.md#공통-규칙)을 함께 따릅니다.

## 입력

호출 시점에 조립해서 프롬프트에 채우는 값입니다. AI 모델 자체의 출력 스키마는
아니지만, 이 문서가 그 입력 형태까지 규정합니다.

```json
{
  "conversationId": 123,
  "referenceDate": "2026-08-19",
  "messages": [
    { "role": "USER", "content": "어제 11시에 자서 아침 6시 반에 일어났어요" },
    { "role": "ASSISTANT", "content": "잘 주무셨네요! 아침은 드셨어요?" },
    { "role": "USER", "content": "삼각김밥이랑 커피 한 잔이요" }
  ]
}
```

- `referenceDate`는 `Conversation.sessionDate`입니다. "어제", "오늘 아침"처럼 상대적
  표현을 절대 날짜로 환산하는 기준점으로 프롬프트에 반드시 포함합니다.
- `messages`는 `ConversationMessage`를 시간순(`sequenceNo`)으로 나열한 것입니다.
  `role`이 `SYSTEM`인 메시지는 추출 대상이 아니므로 호출 전에 제외합니다.

## 출력

```json
{
  "schemaVersion": "health-extraction-v1",
  "records": [
    {
      "type": "SLEEP",
      "summary": "어젯밤 7시간 30분 수면",
      "detail": { "hours": 7.5, "quality": null, "bedtime": "23:00", "wakeTime": "06:30" },
      "recordedDate": "2026-08-18",
      "recordedAt": null,
      "confidence": 0.92,
      "evidence": "어제 11시에 자서 아침 6시 반에 일어났어요"
    },
    {
      "type": "MEAL",
      "summary": "아침으로 삼각김밥과 커피",
      "detail": { "menu": "삼각김밥, 커피", "timing": "아침", "amount": null },
      "recordedDate": "2026-08-19",
      "recordedAt": null,
      "confidence": 0.85,
      "evidence": "삼각김밥이랑 커피 한 잔이요"
    }
  ]
}
```

### 필드

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `type` | enum | O | `HealthRecord.HealthType` — 아래 표 |
| `summary` | string (≤500자) | O | 사람이 읽는 한 줄 요약. `HealthRecord.summary`에 그대로 저장 |
| `detail` | object | O | 타입별 구조화 정보. 아래 타입별 스키마 참고. 알 수 없는 하위 필드는 `null` |
| `recordedDate` | string (date) | O | 실제 일어난 날짜. `referenceDate` 기준으로 상대 표현을 환산 |
| `recordedAt` | string (datetime) \| null | X | 정확한 시각이 명시된 경우만. 없으면 `null` (추정 금지) |
| `confidence` | number (0~1) | O | 추출 확신도. [공통 규칙](./README.md#공통-규칙) 참고 |
| `evidence` | string (≤200자) | O | 근거가 된 **사용자** 발화 스니펫. 원문 그대로 또는 근접 인용 |

### `type` / `detail` 스키마

| `type` | `detail` 필드 |
|---|---|
| `SLEEP` | `hours` (number, 0~24), `quality` (`"good"\|"normal"\|"bad"\|null`), `bedtime` (`"HH:mm"\|null`), `wakeTime` (`"HH:mm"\|null`) |
| `MEAL` | `menu` (string), `timing` (`"아침"\|"점심"\|"저녁"\|"간식"\|null`), `amount` (`"적음"\|"보통"\|"많음"\|null`) |
| `EXERCISE` | `activity` (string), `duration` (number), `unit` (`"min"` 고정), `intensity` (`"낮음"\|"보통"\|"높음"\|null`) |
| `SKIN` | `condition` (string), `area` (string\|null) |
| `MOOD` | `emotion` (string, 예: `"좋음"`, `"불안"`, `"우울"`, `"평온"`), `note` (string\|null) |
| `WATER` | `amount` (number), `unit` (`"ml"\|"cup"`) |
| `OTHER` | `note` (string) — 위 6종에 안 들어가는 건강 관련 사실의 탈출구 |

`OTHER`은 스키마를 넓히는 대신 두는 캐치올입니다. `OTHER`이 잦으면 새 타입 추가를
검토하되, `HealthRecord.HealthType` enum 변경은 `healthrecord/` 담당자와 합의가
필요합니다.

## 빈 결과

대화에 건강 관련 정보가 전혀 없으면(잡담만 오간 경우) 빈 배열로 응답합니다. 이건
실패가 아니라 정상 처리입니다.

```json
{ "schemaVersion": "health-extraction-v1", "records": [] }
```

## 검증 규칙 (Codex 구현 시 참고)

- `records`는 배열이며 없으면 안 됨(필드 자체 누락 불가, 빈 배열은 허용).
- `type`은 `HealthType` enum 값 중 하나. 그 외 문자열이면 파싱 실패로 처리하고
  `AnalysisStatus.FAILED`.
- `confidence`가 0~1 범위를 벗어나면 클램프하지 말고 검증 실패로 처리 (모델이
  스케일을 잘못 이해했다는 신호이므로 조용히 보정하면 더 큰 오류를 숨김).
- 같은 대화에서 같은 `type` + `recordedDate` 조합이 여러 건 나올 수 있음 (예: 아침
  간식과 저녁 식사는 둘 다 `MEAL`) — 중복 제거하지 않고 모두 저장.

## 프롬프트 초안

```
당신은 노인 돌봄 대화에서 건강 관련 사실만 뽑아내는 추출기입니다.

기준 날짜(referenceDate): {referenceDate}
대화:
{messages를 "화자: 내용" 형식으로 나열}

규칙:
1. 사용자(USER) 발화에 명시적으로 드러난 건강 사실만 추출하세요. 추측하거나
   일반적인 조언을 만들어내지 마세요.
2. "어제", "오늘 아침" 같은 상대 표현은 기준 날짜로 환산해 recordedDate에 절대
   날짜로 적으세요.
3. 정확한 시각이 언급되지 않았으면 recordedAt은 null로 두세요. 추정하지 마세요.
4. evidence에는 근거가 된 사용자 발화를 그대로 또는 거의 그대로 인용하세요.
5. 건강과 무관한 대화면 records를 빈 배열로 반환하세요.
6. 아래 JSON 스키마와 정확히 같은 형태로, 다른 텍스트 없이 JSON만 출력하세요.

{health-extraction-v1 출력 스키마 삽입}
```
