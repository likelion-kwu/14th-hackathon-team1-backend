package com.hackathon.backend.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import com.hackathon.backend.health.HealthController;

/**
 * CORS 는 curl 로 재현되지 않으므로 테스트로 고정합니다.
 *
 * 특히 preflight(OPTIONS)가 허용되는지를 봅니다. allowedMethods 에서 OPTIONS 가
 * 빠지면 GET 은 되는데 POST 만 실패하는 상태가 되고, 그 시점에는 브라우저
 * 콘솔에서만 재현되어 원인을 찾기 어렵습니다.
 */
@WebMvcTest(HealthController.class)
class CorsConfigTest {

	private static final String ALLOWED_ORIGIN = "http://localhost:5173";

	@Autowired
	private MockMvc mockMvc;

	@ParameterizedTest
	@ValueSource(strings = { "http://localhost:5173", "http://localhost:3000" })
	@DisplayName("허용된 origin 의 preflight 를 통과시킵니다")
	void allowsPreflightFromAllowedOrigin(String origin) throws Exception {
		// 설정값이 쉼표로 구분된 문자열이므로, 파싱이 깨져 첫 항목만 살아남는
		// 회귀를 잡기 위해 두 origin 을 모두 확인합니다.
		mockMvc.perform(options("/health")
						.header("Origin", origin)
						.header("Access-Control-Request-Method", "GET"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", origin));
	}

	@Test
	@DisplayName("POST preflight 를 통과시킵니다")
	void allowsPostPreflight() throws Exception {
		// POST 는 preflight 를 거치므로 OPTIONS 가 허용되지 않으면 여기서 막힙니다.
		mockMvc.perform(options("/health")
						.header("Origin", ALLOWED_ORIGIN)
						.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Methods",
						org.hamcrest.Matchers.containsString("POST")));
	}

	@Test
	@DisplayName("/api 경로의 POST preflight 도 통과시킵니다")
	void allowsApiPostPreflight() throws Exception {
		// 문서 8장이 요구하는 검증 대상은 /api 의 POST 입니다.
		// 매핑이 /** 이므로 함께 덮이지만, 실제 사용 경로로 한 번 더 확인합니다.
		mockMvc.perform(options("/api/items")
						.header("Origin", ALLOWED_ORIGIN)
						.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
	}

	@Test
	@DisplayName("허용되지 않은 origin 은 거부합니다")
	void rejectsUnknownOrigin() throws Exception {
		// 설정이 무의미하게 모든 origin 을 허용하고 있지 않은지 확인합니다.
		mockMvc.perform(options("/health")
						.header("Origin", "https://evil.example.com")
						.header("Access-Control-Request-Method", "GET"))
				.andExpect(status().isForbidden());
	}
}
