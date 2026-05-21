package com.board.focus.domain;

import com.board.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "focus_task_candidates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskCandidate {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private TaskCategory category;

    // 우선순위 엔진 점수 (0.0 ~ 1.0)
    private double goalAlignmentScore;
    private double urgencyScore;
    private double avoidanceScore;
    private double executionCostScore;
    private double totalScore;

    private Integer estimatedMinutes;
    private LocalDateTime deadline;
    private int mentionCount;

    @Enumerated(EnumType.STRING)
    private CandidateStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public TaskCandidate(User user, String description, TaskCategory category,
                         double avoidanceScore, Integer estimatedMinutes,
                         LocalDateTime deadline, int mentionCount) {
        this.user = user;
        this.description = description;
        this.category = category;
        this.avoidanceScore = avoidanceScore;
        this.estimatedMinutes = estimatedMinutes;
        this.deadline = deadline;
        this.mentionCount = mentionCount;
        this.status = CandidateStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void applyScore(double goalAlignment, double urgency, double executionCost, double total) {
        this.goalAlignmentScore = goalAlignment;
        this.urgencyScore = urgency;
        this.executionCostScore = executionCost;
        this.totalScore = total;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateStatus(CandidateStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementMention() {
        this.mentionCount++;
        this.updatedAt = LocalDateTime.now();
    }
}
