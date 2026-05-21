package com.board.focus.domain;

import com.board.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "focus_task_feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskFeedback {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_task_id", unique = true)
    private DailyTask dailyTask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackResult result;

    @Enumerated(EnumType.STRING)
    private FeedbackReason reason; // nullable

    private LocalDateTime createdAt;

    @Builder
    public TaskFeedback(DailyTask dailyTask, User user, FeedbackResult result, FeedbackReason reason) {
        this.dailyTask = dailyTask;
        this.user = user;
        this.result = result;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }
}
