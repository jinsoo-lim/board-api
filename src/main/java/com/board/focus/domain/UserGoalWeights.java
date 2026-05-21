package com.board.focus.domain;

import com.board.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "focus_user_goal_weights")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserGoalWeights {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    // 목표 카테고리별 가중치 (합계 = 1.0)
    private double businessWeight;
    private double familyWeight;
    private double healthWeight;
    private double selfGrowthWeight;

    @Enumerated(EnumType.STRING)
    private FocusPeriod focusPeriod;

    @Enumerated(EnumType.STRING)
    private TaskDuration preferredDuration;

    private String wakeTime; // "07:00"

    @Column(columnDefinition = "TEXT")
    private String yearlyGoalsJson;

    @Column(columnDefinition = "TEXT")
    private String avoidanceTopicsJson;

    @Column(columnDefinition = "TEXT")
    private String importantPeopleJson;

    private boolean onboardingComplete;
    private LocalDateTime updatedAt;

    @Builder
    public UserGoalWeights(User user, double businessWeight, double familyWeight,
                           double healthWeight, double selfGrowthWeight,
                           FocusPeriod focusPeriod, TaskDuration preferredDuration,
                           String wakeTime, String yearlyGoalsJson,
                           String avoidanceTopicsJson, String importantPeopleJson) {
        this.user = user;
        this.businessWeight = businessWeight;
        this.familyWeight = familyWeight;
        this.healthWeight = healthWeight;
        this.selfGrowthWeight = selfGrowthWeight;
        this.focusPeriod = focusPeriod;
        this.preferredDuration = preferredDuration;
        this.wakeTime = wakeTime;
        this.yearlyGoalsJson = yearlyGoalsJson;
        this.avoidanceTopicsJson = avoidanceTopicsJson;
        this.importantPeopleJson = importantPeopleJson;
        this.onboardingComplete = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void update(double businessWeight, double familyWeight, double healthWeight,
                       double selfGrowthWeight, FocusPeriod focusPeriod,
                       TaskDuration preferredDuration, String wakeTime,
                       String yearlyGoalsJson, String avoidanceTopicsJson,
                       String importantPeopleJson) {
        this.businessWeight = businessWeight;
        this.familyWeight = familyWeight;
        this.healthWeight = healthWeight;
        this.selfGrowthWeight = selfGrowthWeight;
        this.focusPeriod = focusPeriod;
        this.preferredDuration = preferredDuration;
        this.wakeTime = wakeTime;
        this.yearlyGoalsJson = yearlyGoalsJson;
        this.avoidanceTopicsJson = avoidanceTopicsJson;
        this.importantPeopleJson = importantPeopleJson;
        this.updatedAt = LocalDateTime.now();
    }

    public void adjustWeights(double businessWeight, double familyWeight,
                              double healthWeight, double selfGrowthWeight) {
        this.businessWeight = businessWeight;
        this.familyWeight = familyWeight;
        this.healthWeight = healthWeight;
        this.selfGrowthWeight = selfGrowthWeight;
        this.updatedAt = LocalDateTime.now();
    }
}
