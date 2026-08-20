package com.hackathon.backend.engagement.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hackathon.backend.common.exception.NotFoundException;
import com.hackathon.backend.engagement.dto.StreakResponse;
import com.hackathon.backend.member.entity.Member;
import com.hackathon.backend.member.entity.Streak;
import com.hackathon.backend.member.repository.MemberRepository;
import com.hackathon.backend.member.repository.StreakRepository;

@Service
public class StreakService {

	private final MemberRepository memberRepository;
	private final StreakRepository streakRepository;

	public StreakService(MemberRepository memberRepository, StreakRepository streakRepository) {
		this.memberRepository = memberRepository;
		this.streakRepository = streakRepository;
	}

	@Transactional(readOnly = true)
	public StreakResponse findStreak(Long memberId) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new NotFoundException("해당 회원이 없습니다."));
		return streakRepository.findById(memberId).map(this::toResponse)
				.orElseGet(() -> new StreakResponse(member.getId(), 0, 0, null, null));
	}

	@Transactional
	public void recordActivity(Member member, LocalDate activeDate) {
		Streak streak = streakRepository.findById(member.getId())
				.orElseGet(() -> streakRepository.save(Streak.builder().member(member).build()));
		streak.recordActivity(activeDate);
	}

	private StreakResponse toResponse(Streak streak) {
		return new StreakResponse(streak.getMemberId(), streak.getCurrentStreak(), streak.getLongestStreak(),
				streak.getLastActiveDate(), streak.getUpdatedAt());
	}
}
