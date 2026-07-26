package com.hackathon.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * DB 확인 분기를 검증합니다. 이 기능의 존재 이유는 503 이므로 그 경로를 고정합니다.
 *
 * HealthControllerTest 와 분리한 이유가 있습니다. DataSource 목을 주입하면
 * 클래스의 모든 테스트에 적용되어, DataSource 가 없을 때의 경로
 * (NOT_CONFIGURED + 200)를 같은 클래스에서 검증할 수 없습니다.
 */
@WebMvcTest(HealthController.class)
class HealthControllerDatabaseTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DataSource dataSource;

	@Test
	@DisplayName("커넥션이 유효하면 200 과 UP 을 반환합니다")
	void validConnectionReturnsUp() throws Exception {
		Connection connection = mock(Connection.class);
		given(connection.isValid(anyInt())).willReturn(true);
		given(dataSource.getConnection()).willReturn(connection);

		mockMvc.perform(get("/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.database").value("UP"));
	}

	@Test
	@DisplayName("DB 접속에 실패하면 503 과 DOWN 을 반환합니다")
	void connectionFailureReturnsServiceUnavailable() throws Exception {
		// DB 가 끊겼는데 200 을 반환하면 배포는 성공으로 판정되고
		// 앱은 아무 요청도 처리하지 못하는 상태가 됩니다.
		given(dataSource.getConnection()).willThrow(new SQLException("접속 실패"));

		mockMvc.perform(get("/health"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.status").value("DOWN"))
				.andExpect(jsonPath("$.database").value("DOWN"));
	}

	@Test
	@DisplayName("커넥션이 유효하지 않으면 503 을 반환합니다")
	void invalidConnectionReturnsServiceUnavailable() throws Exception {
		Connection connection = mock(Connection.class);
		given(connection.isValid(anyInt())).willReturn(false);
		given(dataSource.getConnection()).willReturn(connection);

		mockMvc.perform(get("/health"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.database").value("DOWN"));
	}

	@Test
	@DisplayName("접속 실패 응답에 예외 메시지가 노출되지 않습니다")
	void hidesConnectionErrorDetails() throws Exception {
		// 접속 문자열과 자격증명이 예외 메시지에 섞여 나올 수 있습니다.
		given(dataSource.getConnection())
				.willThrow(new SQLException("Access denied for user 'admin'@'10.0.0.1'"));

		String body = mockMvc.perform(get("/health"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertThat(body).doesNotContain("Access denied").doesNotContain("admin");
	}
}
