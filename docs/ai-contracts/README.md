# AI 계약 문서

`AiAnalysis`(`ai/entity/AiAnalysis.java`)가 처리하는 5개 `TaskType`이 AI 모델에게 어떤
입력을 주고 어떤 출력을 기대하는지 정의합니다. **여기 있는 것은 스키마와 프롬프트
설계이지 구현이 아닙니다.** 실제 검증기·DTO·엔티티 저장 로직은 `ai/` 패키지에서
구현합니다.

| 문서 | TaskType | schemaVersion |
|---|---|---|
| [health-extraction-v1.md](./health-extraction-v1.md) | `HEALTH_EXTRACTION` | `health-extraction-v1` |
| [conversation-summary-v1.md](./conversation-summary-v1.md) | `DAILY_SUMMARY` / `WEEKLY_SUMMARY` / `MONTHLY_SUMMARY` | `daily-summary-v1` / `weekly-summary-v1` / `monthly-summary-v1` |
| [overall-report-v1.md](./overall-report-v1.md) | `OVERALL_REPORT` | `overall-report-v1` |

## 공통 규칙

- **출력은 JSON 객체 하나만.** 마크다운 코드펜스(```json)나 설명 문장을 앞뒤에 붙이지
  않습니다. `AiAnalysis.rawResponse`(TEXT 컬럼, 모델 원문을 그대로 보관)에 저장되는
  값입니다.
- **`schemaVersion` 필드를 최상위에 반드시 포함합니다.** 위 표의 값과 정확히 일치해야
  하며, `AiAnalysis.schemaVersion` 컬럼에 그대로 기록됩니다. 스키마를 깨는 변경을 하면
  버전을 올립니다 (`v1` → `v2`), 필드를 추가만 하는 하위호환 변경은 버전을 유지합니다.
- **날짜/시각은 ISO-8601, 타임존은 Asia/Seoul(KST) 기준입니다.** `LocalDate`는
  `YYYY-MM-DD`, `LocalDateTime`은 `YYYY-MM-DDTHH:mm:ss`.
- **모를 때는 `null`, 지어내지 않습니다.** 대화에 근거가 없는 값은 필드를 생략하지 말고
  `null`로 채웁니다 (Jackson 역직렬화 시 필드 누락과 명시적 null을 구분해야 하는 부담을
  없애기 위함).
- **추출할 게 없으면 빈 결과로 응답합니다.** 각 문서의 "빈 결과" 절 참고. 실패
  (`AnalysisStatus.FAILED`)와는 다릅니다 — 이건 정상 처리 결과입니다.
- **신뢰도(confidence)는 0.0 ~ 1.0, 소수점 2자리 권장.** DB 컬럼은
  `BigDecimal(precision=5, scale=4)`이므로 상위 레이어에서 반올림합니다.
