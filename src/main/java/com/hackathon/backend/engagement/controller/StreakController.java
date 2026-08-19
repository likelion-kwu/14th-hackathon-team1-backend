package com.hackathon.backend.engagement.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.backend.common.exception.NotFoundException;
import com.hackathon.backend.common.response.ApiNotFound;
import com.hackathon.backend.common.response.ApiResponse;
import com.hackathon.backend.engagement.dto.StreakResponse;
import com.hackathon.backend.member.repository.MemberRepository;
import com.hackathon.backend.member.repository.StreakRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

/**
 * 연속 대화 기록(스트릭) API 입니다.
 *
 * 경로가 /api/members 아래인데 컨트롤러가 MemberController 와 분리돼 있습니다.
 * 스트릭은 engagement 기능이고 스케줄러·푸시가 함께 붙을 예정이라 패키지를 나눴습니다.
 * Spring 은 같은 prefix 를 쓰는 컨트롤러가 여럿이어도 하위 경로가 겹치지 않으면 정상 매핑합니다.
 */
@Tag(name = "engagement", description = "연속 대화 기록(스트릭) API 입니다.")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class StreakController {

    private final StreakRepository streakRepository;
    private final MemberRepository memberRepository;

    @Operation(summary = "스트릭 조회",
            description = "연속 대화 일수를 조회합니다. 아직 기록이 없는 회원은 404 가 아니라 currentStreak 0 으로 응답합니다. "
                    + "가입 직후 화면이 오류 분기를 타지 않게 하기 위해서입니다.")
    @ApiNotFound("해당 회원이 없습니다.")
    @GetMapping("/{memberId}/streak")
    public ApiResponse<StreakResponse> findByMemberId(
            @Parameter(description = "회원 식별자입니다", required = true)
            @PathVariable @Positive Long memberId) {

        if (!memberRepository.existsById(memberId)) {
            throw new NotFoundException("해당 회원이 없습니다.");
        }

        return ApiResponse.success(
                streakRepository.findById(memberId)
                        .map(s -> new StreakResponse(
                                s.getMemberId(),
                                s.getCurrentStreak(),
                                s.getLongestStreak(),
                                s.getLastActiveDate(),
                                s.getUpdatedAt()))
                        .orElseGet(() -> new StreakResponse(memberId, 0, 0, null, null))
        );
    }
}
