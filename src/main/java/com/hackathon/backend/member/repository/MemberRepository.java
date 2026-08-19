package com.hackathon.backend.member.repository;

import com.hackathon.backend.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.time.LocalTime;
import java.util.List;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByPhone(String phone);

    boolean existsByPhone(String phone);

    /** 알림이 켜져 있고 지정한 KST 시각(분)에 해당하는 회원만 조회합니다. */
    List<Member> findByNotifyEnabledTrueAndNotifyTime(LocalTime notifyTime);
}
