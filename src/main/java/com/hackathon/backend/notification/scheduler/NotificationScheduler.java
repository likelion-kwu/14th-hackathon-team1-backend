package com.hackathon.backend.notification.scheduler;

import java.time.LocalTime;
import java.time.ZoneId;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.hackathon.backend.notification.NotificationDispatchService;

/** 매 분 KST 현재 시각과 회원 알림 시각이 일치하는 대상을 발송합니다. */
@Component
public class NotificationScheduler {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private final NotificationDispatchService notificationDispatchService;

	public NotificationScheduler(NotificationDispatchService notificationDispatchService) {
		this.notificationDispatchService = notificationDispatchService;
	}

	@Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
	public void dispatchDueNotifications() {
		LocalTime currentMinute = LocalTime.now(KST).withSecond(0).withNano(0);
		notificationDispatchService.dispatchAt(currentMinute);
	}
}
