package com.hackathon.backend.member.repository;

import com.hackathon.backend.member.entity.Streak;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StreakRepository extends JpaRepository<Streak, Long> {
}
