package com.hackathon.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

/**
 * /health 는 배포 스모크 테스트가 의존하는 경로입니다.
 * 응답 형태가 바뀌면 배포 파이프라인이 조용히 깨지므로 계약을 테스트로 고정합니다.
 *
 * 주의: @WebMvcTest 는 서블릿 필터 체인을 전부 태우지 않습니다. CORS 나 인증을
 * 추가한 뒤에는 이 테스트가 통과해도 실제 /health 가 거부될 수 있으므로,
 * 최종 보증은 배포 후 스모크 테스트가 담당합니다.
 */
@WebMvcTest(HealthController.class)
class HealthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("status 는 UP 을 반환한다")
	void returnsUp() throws Exception {
		mockMvc.perform(get("/health"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	@DisplayName("응답은 래핑되지 않은 세 개의 필드로 고정된다")
	void responseIsNotWrapped() throws Exception {
		// 공통 응답 래퍼(ApiResponse)를 도입할 때 /health 까지 래핑되면
		// 배포 스모크 테스트가 조용히 깨집니다. 그 회귀를 여기서 잡습니다.
		mockMvc.perform(get("/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.*", hasSize(3)))
				.andExpect(jsonPath("$.data").doesNotExist())
				.andExpect(jsonPath("$.status").exists())
				.andExpect(jsonPath("$.serverTime").exists())
				.andExpect(jsonPath("$.timeZone").exists());
	}

	@Test
	@DisplayName("serverTime 은 오프셋을 포함하고 브라우저가 파싱할 수 있다")
	void serverTimeIsParseableWithOffset() throws Exception {
		String body = mockMvc.perform(get("/health"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String serverTime = JsonPath.read(body, "$.serverTime");

		// 프론트가 실제로 하는 일과 같은 검증입니다. 오프셋이 없으면 파싱에 실패합니다.
		assertThatCode(() -> OffsetDateTime.parse(serverTime)).doesNotThrowAnyException();
		// Jackson 이 [Asia/Seoul] 같은 존 ID 접미사를 붙이면 브라우저 Date 파싱이 깨집니다.
		assertThat(serverTime).doesNotContain("[");
	}

	@Test
	@DisplayName("timeZone 은 유효한 타임존 ID 다")
	void timeZoneIsValidZoneId() throws Exception {
		String body = mockMvc.perform(get("/health"))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String timeZone = JsonPath.read(body, "$.timeZone");

		// 어떤 값인지는 실행 환경에 달렸으므로 단정하지 않습니다.
		// 값이 실제 타임존 ID 로 해석되는지만 확인합니다.
		assertThatCode(() -> ZoneId.of(timeZone)).doesNotThrowAnyException();
	}
}
