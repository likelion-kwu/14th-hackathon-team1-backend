package com.hackathon.backend.member.repository;

import com.hackathon.backend.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByPhone(String phone);

    boolean existsByPhone(String phone);
}
