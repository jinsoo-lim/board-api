package com.board.focus.domain;

import com.board.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "focus_daily_tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyTask {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_candidate_id")
    private TaskCandidate taskCandidate;

    private LocalDate taskDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    // 왜 이게 선택됐는지 (중간 신뢰도일 때 표시)
    @Column(columnDefinition = "TEXT")
    private String reason;

    private double confidenceScore;

    @Enumerated(EnumType.STRING)
    private DailyTaskStatus status;

    // 신뢰도 낮을 때 대안 태스크 (JSON)
    @Column(columnDefinition = "TEXT")
    private String alternativeJson;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    @Builder
    public DailyTask(User user, TaskCandidate taskCandidate, LocalDate taskDate,
                     String description, String reason, double confidenceScore,
                     String alternativeJson) {
        this.user = user;
        this.taskCandidate = taskCandidate;
        this.taskDate = taskDate;
        this.description = description;
        this.reason = reason;
        this.confidenceScore = confidenceScore;
        this.alternativeJson = alternativeJson;
        this.status = DailyTaskStatus.SHOWN;
        this.createdAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = DailyTaskStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void skip() {
        this.status = DailyTaskStatus.SKIPPED;
    }
}
