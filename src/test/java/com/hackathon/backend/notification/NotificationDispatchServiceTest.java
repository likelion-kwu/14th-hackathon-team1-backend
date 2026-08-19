package com.hackathon.backend.notification;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hackathon.backend.member.entity.Member;
import com.hackathon.backend.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchServiceTest {

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private PushNotificationSender pushNotificationSender;

	@InjectMocks
	private NotificationDispatchService notificationDispatchService;

	@Test
	void dispatchesOnlyMembersReturnedForTheCurrentMinute() {
		LocalTime notifyTime = LocalTime.of(21, 0);
		Member member = Member.builder().nickname("회원").phone("010-9000-0001").fcmToken("demo-token")
				.notifyTime(notifyTime).notifyEnabled(true).build();
		given(memberRepository.findByNotifyEnabledTrueAndNotifyTime(notifyTime)).willReturn(List.of(member));

		notificationDispatchService.dispatchAt(notifyTime);

		then(memberRepository).should().findByNotifyEnabledTrueAndNotifyTime(notifyTime);
		then(pushNotificationSender).should().send("demo-token", "안부 확인 시간이에요", "오늘 하루는 어떠셨나요?");
	}
}
