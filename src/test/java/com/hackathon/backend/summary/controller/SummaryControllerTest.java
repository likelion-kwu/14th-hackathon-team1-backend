package com.hackathon.backend.summary.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.hackathon.backend.common.exception.NotFoundException;
import com.hackathon.backend.summary.dto.DailySummaryResponse;
import com.hackathon.backend.summary.dto.OverallReportResponse;
import com.hackathon.backend.summary.dto.MonthlySummaryResponse;
import com.hackathon.backend.summary.dto.WeeklySummaryResponse;
import com.hackathon.backend.summary.service.SummaryService;

/**
 * 요약·종합 리포트 조회 API 의 웹 계층 계약을 고정합니다.
 *
 * 서비스는 대역으로 두고, 파라미터 검증(400) · 미존재(404) · 응답 JSON 형태만
 * 확인합니다. 특히 overall 의 detail 이 문자열이 아니라 JSON 객체로 나가는지를
 * 확인하는 것이 이 클래스의 핵심입니다 (@JsonRawValue 가 실제로 동작하는지).
 */
@WebMvcTest(SummaryController.class)
class SummaryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SummaryService summaryService;

	@Nested
	@DisplayName("GET /api/summaries/daily")
	class FindDaily {

		@Test
		@DisplayName("정상 조회 시 200 과 일일 요약 JSON 을 반환합니다")
		void returnsDailySummary() throws Exception {
			given(summaryService.findDaily(1L, LocalDate.of(2026, 8, 16))).willReturn(new DailySummaryResponse(
					1L, LocalDate.of(2026, 8, 16), "새벽 3시에 잤고 저녁으로 야식을 먹었습니다.", 1, 1420,
					LocalDateTime.of(2026, 8, 16, 21, 0), LocalDateTime.of(2026, 8, 16, 21, 0)));

			mockMvc.perform(get("/api/summaries/daily")
							.param("memberId", "1")
							.param("date", "2026-08-16"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.memberId").value(1))
					.andExpect(jsonPath("$.data.summaryDate").value("2026-08-16"))
					.andExpect(jsonPath("$.data.summary").value("새벽 3시에 잤고 저녁으로 야식을 먹었습니다."))
					.andExpect(jsonPath("$.data.conversationCount").value(1))
					.andExpect(jsonPath("$.data.tokenCount").value(1420))
					.andExpect(jsonPath("$.error").doesNotExist());
		}

		@Test
		@DisplayName("memberId 가 없으면 400 을 반환합니다")
		void rejectsMissingMemberId() throws Exception {
			mockMvc.perform(get("/api/summaries/daily"))
					.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("memberId 가 0 이면 400 과 VALIDATION_FAILED 를 반환합니다")
		void rejectsZeroMemberId() throws Exception {
			mockMvc.perform(get("/api/summaries/daily").param("memberId", "0"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
		}

		@Test
		@DisplayName("memberId 가 음수이면 400 과 VALIDATION_FAILED 를 반환합니다")
		void rejectsNegativeMemberId() throws Exception {
			mockMvc.perform(get("/api/summaries/daily").param("memberId", "-1"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
		}

		@Test
		@DisplayName("일일 요약이 없으면 404 와 NOT_FOUND 를 반환합니다")
		void returnsNotFound() throws Exception {
			given(summaryService.findDaily(anyLong(), any()))
					.willThrow(new NotFoundException("일일 요약을 찾을 수 없습니다."));

			mockMvc.perform(get("/api/summaries/daily").param("memberId", "1"))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
		}
	}

	@Nested
	@DisplayName("GET /api/summaries/weekly")
	class FindWeekly {

		@Test
		@DisplayName("정상 조회 시 200 과 주간 요약 JSON 을 반환합니다")
		void returnsWeeklySummary() throws Exception {
			given(summaryService.findWeekly(1L, LocalDate.of(2026, 8, 10))).willReturn(new WeeklySummaryResponse(
					1L, LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 16), "주간 요약", 7, 9800,
					LocalDateTime.of(2026, 8, 16, 21, 0), LocalDateTime.of(2026, 8, 16, 21, 0)));

			mockMvc.perform(get("/api/summaries/weekly")
							.param("memberId", "1")
							.param("periodStart", "2026-08-10"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.memberId").value(1))
					.andExpect(jsonPath("$.data.periodStart").value("2026-08-10"))
					.andExpect(jsonPath("$.data.periodEnd").value("2026-08-16"))
					.andExpect(jsonPath("$.data.summary").value("주간 요약"))
					.andExpect(jsonPath("$.data.dailySummaryCount").value(7))
					.andExpect(jsonPath("$.data.tokenCount").value(9800));
		}

		@Test
		@DisplayName("memberId 가 없으면 400 을 반환합니다")
		void rejectsMissingMemberId() throws Exception {
			mockMvc.perform(get("/api/summaries/weekly"))
					.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("memberId 가 0 이거나 음수이면 400 과 VALIDATION_FAILED 를 반환합니다")
		void rejectsNonPositiveMemberId() throws Exception {
			mockMvc.perform(get("/api/summaries/weekly").param("memberId", "0"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

			mockMvc.perform(get("/api/summaries/weekly").param("memberId", "-5"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
		}

		@Test
		@DisplayName("주간 요약이 없으면 404 와 NOT_FOUND 를 반환합니다")
		void returnsNotFound() throws Exception {
			given(summaryService.findWeekly(anyLong(), any()))
					.willThrow(new NotFoundException("주간 요약을 찾을 수 없습니다."));

			mockMvc.perform(get("/api/summaries/weekly").param("memberId", "1"))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
		}
	}

	@Nested
	@DisplayName("GET /api/summaries/monthly")
	class FindMonthly {

		@Test
		@DisplayName("정상 조회 시 200 과 월간 요약 JSON 을 반환합니다")
		void returnsMonthlySummary() throws Exception {
			given(summaryService.findMonthly(1L, LocalDate.of(2026, 8, 1))).willReturn(new MonthlySummaryResponse(
					1L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), "월간 요약", 4, 30000,
					LocalDateTime.of(2026, 8, 16, 21, 0), LocalDateTime.of(2026, 8, 16, 21, 0)));

			mockMvc.perform(get("/api/summaries/monthly")
							.param("memberId", "1")
							.param("periodStart", "2026-08-01"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.memberId").value(1))
					.andExpect(jsonPath("$.data.periodStart").value("2026-08-01"))
					.andExpect(jsonPath("$.data.periodEnd").value("2026-08-31"))
					.andExpect(jsonPath("$.data.summary").value("월간 요약"))
					.andExpect(jsonPath("$.data.weeklySummaryCount").value(4))
					.andExpect(jsonPath("$.data.tokenCount").value(30000));
		}

		@Test
		@DisplayName("memberId 가 없으면 400 을 반환합니다")
		void rejectsMissingMemberId() throws Exception {
			mockMvc.perform(get("/api/summaries/monthly"))
					.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("memberId 가 0 이거나 음수이면 400 과 VALIDATION_FAILED 를 반환합니다")
		void rejectsNonPositiveMemberId() throws Exception {
			mockMvc.perform(get("/api/summaries/monthly").param("memberId", "0"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

			mockMvc.perform(get("/api/summaries/monthly").param("memberId", "-5"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
		}

		@Test
		@DisplayName("월간 요약이 없으면 404 와 NOT_FOUND 를 반환합니다")
		void returnsNotFound() throws Exception {
			given(summaryService.findMonthly(anyLong(), any()))
					.willThrow(new NotFoundException("월간 요약을 찾을 수 없습니다."));

			mockMvc.perform(get("/api/summaries/monthly").param("memberId", "1"))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
		}
	}

	@Nested
	@DisplayName("GET /api/summaries/overall")
	class FindOverall {

		@Test
		@DisplayName("정상 조회 시 200 과 종합 리포트 JSON 을 반환합니다")
		void returnsOverallReport() throws Exception {
			given(summaryService.findOverall(1L)).willReturn(new OverallReportResponse(
					1L, "종합 리포트 본문", "{\"score\":80,\"grade\":\"A\"}", 3,
					LocalDateTime.of(2026, 8, 16, 21, 0),
					LocalDateTime.of(2026, 8, 16, 21, 0), LocalDateTime.of(2026, 8, 16, 21, 0)));

			mockMvc.perform(get("/api/summaries/overall").param("memberId", "1"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.memberId").value(1))
					.andExpect(jsonPath("$.data.summary").value("종합 리포트 본문"))
					.andExpect(jsonPath("$.data.monthlySummaryCount").value(3))
					// detail 이 문자열이 아니라 JSON 객체로 직렬화되는지 확인합니다.
					// 문자열로 이스케이프되어 나가면 이 경로 자체가 존재하지 않습니다.
					.andExpect(jsonPath("$.data.detail.score").value(80))
					.andExpect(jsonPath("$.data.detail.grade").value("A"));
		}

		@Test
		@DisplayName("detail 이 없으면 null 로 내려갑니다")
		void returnsNullDetailWhenAbsent() throws Exception {
			given(summaryService.findOverall(1L)).willReturn(new OverallReportResponse(
					1L, "종합 리포트 본문", null, 0, null,
					LocalDateTime.of(2026, 8, 16, 21, 0), LocalDateTime.of(2026, 8, 16, 21, 0)));

			// doesNotExist() 로 두면 키가 아예 빠진 경우까지 통과합니다. 우리가 지키려는
			// 것은 "키는 있고 값이 null" 이므로 value(nullValue()) 로 단정합니다.
			mockMvc.perform(get("/api/summaries/overall").param("memberId", "1"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.detail").value(nullValue()));
		}

		@Test
		@DisplayName("memberId 가 없으면 400 을 반환합니다")
		void rejectsMissingMemberId() throws Exception {
			mockMvc.perform(get("/api/summaries/overall"))
					.andExpect(status().isBadRequest());
		}

		@Test
		@DisplayName("memberId 가 0 이거나 음수이면 400 과 VALIDATION_FAILED 를 반환합니다")
		void rejectsNonPositiveMemberId() throws Exception {
			mockMvc.perform(get("/api/summaries/overall").param("memberId", "0"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));

			mockMvc.perform(get("/api/summaries/overall").param("memberId", "-5"))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
		}

		@Test
		@DisplayName("종합 리포트가 없으면 404 와 NOT_FOUND 를 반환합니다")
		void returnsNotFound() throws Exception {
			given(summaryService.findOverall(1L))
					.willThrow(new NotFoundException("종합 리포트를 찾을 수 없습니다."));

			mockMvc.perform(get("/api/summaries/overall").param("memberId", "1"))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
		}
	}
}
