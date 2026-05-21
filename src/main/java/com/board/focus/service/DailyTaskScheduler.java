package com.board.focus.service;

import com.board.domain.user.UserRepository;
import com.board.focus.domain.UserGoalWeightsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyTaskScheduler {

    private final DailyTaskService dailyTaskService;
    private final UserGoalWeightsRepository weightsRepository;

    // 매일 새벽 5시 실행 → 사용자가 기상 전에 준비 완료
    @Scheduled(cron = "0 0 5 * * *")
    public void generateDailyTasks() {
        log.info("Daily task generation started");

        weightsRepository.findAll().stream()
            .filter(w -> w.isOnboardingComplete())
            .forEach(w -> {
                try {
                    dailyTaskService.generateForUser(w.getUser().getId());
                } catch (Exception e) {
                    log.error("Failed to generate task for userId={}: {}",
                        w.getUser().getId(), e.getMessage());
                }
            });

        log.info("Daily task generation completed");
    }
}
