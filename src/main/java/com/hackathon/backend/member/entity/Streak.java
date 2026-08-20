package com.hackathon.backend.member.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "streak")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Streak {

    @Id
    private Long memberId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false)
    @Builder.Default
    private int currentStreak = 0;

    @Column(nullable = false)
    @Builder.Default
    private int longestStreak = 0;

    private LocalDate lastActiveDate;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public void recordActivity(LocalDate activeDate) {
        if (activeDate.equals(lastActiveDate)) {
            return;
        }
        currentStreak = lastActiveDate != null && lastActiveDate.plusDays(1).equals(activeDate)
                ? currentStreak + 1
                : 1;
        longestStreak = Math.max(longestStreak, currentStreak);
        lastActiveDate = activeDate;
    }
}
