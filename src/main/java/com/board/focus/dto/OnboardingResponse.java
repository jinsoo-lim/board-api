package com.board.focus.dto;

import com.board.focus.domain.UserGoalWeights;

public record OnboardingResponse(
    double businessWeight,
    double familyWeight,
    double healthWeight,
    double selfGrowthWeight,
    String focusPeriod,
    String preferredDuration,
    String wakeTime,
    boolean onboardingComplete
) {
    public static OnboardingResponse from(UserGoalWeights w) {
        return new OnboardingResponse(
            w.getBusinessWeight(), w.getFamilyWeight(),
            w.getHealthWeight(), w.getSelfGrowthWeight(),
            w.getFocusPeriod().name(), w.getPreferredDuration().name(),
            w.getWakeTime(), w.isOnboardingComplete()
        );
    }
}
