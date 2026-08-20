package com.hackathon.backend.engagement.service;

import com.hackathon.backend.common.exception.NotFoundException;
import com.hackathon.backend.engagement.dto.StreakResponse;
import com.hackathon.backend.member.repository.MemberRepository;
import com.hackathon.backend.member.repository.StreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StreakService {

    private final MemberRepository memberRepository;
    private final StreakRepository streakRepository;

    @Transactional(readOnly = true)
    public StreakResponse findStreak(Long memberId) {
        memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("해당 회원이 없습니다."));

        return streakRepository.findById(memberId)
                .map(streak -> new StreakResponse(
                        memberId,
                        streak.getCurrentStreak(),
                        streak.getLongestStreak(),
                        streak.getLastActiveDate(),
                        streak.getUpdatedAt()
                ))
                .orElse(new StreakResponse(memberId, 0, 0, null, null));
    }
}