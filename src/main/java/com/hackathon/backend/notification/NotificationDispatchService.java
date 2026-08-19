package com.hackathon.backend.notification;

import java.time.LocalTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hackathon.backend.member.repository.MemberRepository;

/** 예약 시각과 일치하는 회원을 읽어 푸시 발송 경계로 전달합니다. */
@Service
@Transactional(readOnly = true)
public class NotificationDispatchService {

	private static final String TITLE = "안부 확인 시간이에요";
	private static final String BODY = "오늘 하루는 어떠셨나요?";

	private final MemberRepository memberRepository;
	private final PushNotificationSender pushNotificationSender;

	public NotificationDispatchService(MemberRepository memberRepository, PushNotificationSender pushNotificationSender) {
		this.memberRepository = memberRepository;
		this.pushNotificationSender = pushNotificationSender;
	}

	public void dispatchAt(LocalTime notifyTime) {
		memberRepository.findByNotifyEnabledTrueAndNotifyTime(notifyTime)
				.forEach(member -> pushNotificationSender.send(member.getFcmToken(), TITLE, BODY));
	}
}
