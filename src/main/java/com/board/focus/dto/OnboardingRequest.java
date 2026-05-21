package com.board.focus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OnboardingRequest(
    @NotBlank String yearGoalAnswer,       // "1년 후 뭘 이뤘으면?"
    @NotBlank String avoidanceAnswer,      // "지금 뭘 피하고 있어?"
    @NotBlank String importantPeopleAnswer, // "중요한 사람 3명?"
    @NotNull  String wakeTime,             // "07:00"
    @NotNull  String focusPeriod,          // MORNING / AFTERNOON / EVENING
    @NotNull  String preferredDuration     // SHORT / MEDIUM / LONG
) {}
