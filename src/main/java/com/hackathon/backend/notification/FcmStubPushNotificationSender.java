package com.hackathon.backend.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** 시연 환경용 FCM 대체 구현입니다. 외부 호출 없이 발송 내용을 로그로만 남깁니다. */
@Component
public class FcmStubPushNotificationSender implements PushNotificationSender {

	private static final Logger log = LoggerFactory.getLogger(FcmStubPushNotificationSender.class);

	@Override
	public void send(String token, String title, String body) {
		log.info("[FCM-STUB] token={} title={} body={}", token, title, body);
	}
}
