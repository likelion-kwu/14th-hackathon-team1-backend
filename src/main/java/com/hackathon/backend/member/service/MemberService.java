package com.hackathon.backend.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hackathon.backend.common.exception.NotFoundException;
import com.hackathon.backend.member.dto.FcmTokenRequest;
import com.hackathon.backend.member.dto.MemberCreateRequest;
import com.hackathon.backend.member.dto.MemberResponse;
import com.hackathon.backend.member.dto.NotificationSettingRequest;
import com.hackathon.backend.member.entity.Member;
import com.hackathon.backend.member.entity.Streak;
import com.hackathon.backend.member.repository.MemberRepository;
import com.hackathon.backend.member.repository.StreakRepository;

@Service
@Transactional(readOnly = true)
public class MemberService {

	private final MemberRepository memberRepository;
	private final StreakRepository streakRepository;

	public MemberService(MemberRepository memberRepository, StreakRepository streakRepository) {
		this.memberRepository = memberRepository;
		this.streakRepository = streakRepository;
	}

	@Transactional
	public MemberResponse create(MemberCreateRequest request) {
		Member member = memberRepository.save(Member.builder().nickname(request.nickname()).phone(request.phone()).build());
		streakRepository.save(Streak.builder().member(member).build());
		return toResponse(member);
	}

	public MemberResponse findById(Long memberId) {
		return toResponse(findMember(memberId));
	}

	@Transactional
	public MemberResponse updateNotificationSetting(Long memberId, NotificationSettingRequest request) {
		Member member = findMember(memberId);
		member.updateNotificationSetting(request.notifyTime(), request.notifyEnabled());
		return toResponse(member);
	}

	@Transactional
	public MemberResponse updateFcmToken(Long memberId, FcmTokenRequest request) {
		Member member = findMember(memberId);
		member.updateFcmToken(request.fcmToken());
		return toResponse(member);
	}

	private Member findMember(Long memberId) {
		return memberRepository.findById(memberId).orElseThrow(() -> new NotFoundException("해당 회원이 없습니다."));
	}

	private MemberResponse toResponse(Member member) {
		return new MemberResponse(member.getId(), member.getNickname(), member.getPhone(), member.getNotifyTime(),
				member.isNotifyEnabled(), member.getFcmToken() != null, member.getCreatedAt(), member.getUpdatedAt());
	}
}
