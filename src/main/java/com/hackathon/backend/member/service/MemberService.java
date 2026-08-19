package com.hackathon.backend.member.service;

import com.hackathon.backend.common.exception.NotFoundException;
import com.hackathon.backend.member.dto.FcmTokenRequest;
import com.hackathon.backend.member.dto.MemberCreateRequest;
import com.hackathon.backend.member.dto.MemberResponse;
import com.hackathon.backend.member.dto.NotificationSettingRequest;
import com.hackathon.backend.member.entity.Member;
import com.hackathon.backend.member.entity.Streak;
import com.hackathon.backend.member.repository.MemberRepository;
import com.hackathon.backend.member.repository.StreakRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 도메인 비즈니스 로직입니다.
 *
 * 유니크 중복(전화번호)은 existsByPhone 검사를 하지 않고 DB 제약에 맡깁니다.
 * 검사 후 저장 사이에 다른 요청이 끼어들면 어차피 예외가 터지고,
 * GlobalExceptionHandler 의 DataIntegrityViolationException 핸들러가
 * 409 CONFLICT 로 변환합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final StreakRepository streakRepository;

    @Transactional
    public MemberResponse create(MemberCreateRequest request) {
        Member member = Member.builder()
                .nickname(request.nickname())
                .phone(request.phone())
                .build();
        member = memberRepository.save(member);

        // 가입과 동시에 스트릭 레코드를 초기화합니다.
        // @MapsId 로 memberId == member.id 이므로 member 만 세팅하면 됩니다.
        Streak streak = Streak.builder().member(member).build();
        streakRepository.save(streak);

        return toResponse(member);
    }

    public MemberResponse findById(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("해당 회원이 없습니다."));
        return toResponse(member);
    }

    @Transactional
    public MemberResponse updateNotificationSetting(Long memberId, NotificationSettingRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("해당 회원이 없습니다."));
        member.updateNotificationSetting(request.notifyTime(), request.notifyEnabled());
        return toResponse(member);
    }

    @Transactional
    public MemberResponse updateFcmToken(Long memberId, FcmTokenRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("해당 회원이 없습니다."));
        member.updateFcmToken(request.fcmToken());
        return toResponse(member);
    }

    private MemberResponse toResponse(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getNickname(),
                member.getPhone(),
                member.getNotifyTime(),
                member.isNotifyEnabled(),
                member.getFcmToken() != null,
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}
