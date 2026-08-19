package com.hackathon.backend.notification;

/** 푸시 제공자와 무관한 발송 경계입니다. */
public interface PushNotificationSender {

	void send(String token, String title, String body);
}
