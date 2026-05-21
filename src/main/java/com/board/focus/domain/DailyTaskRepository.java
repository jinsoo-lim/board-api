package com.board.focus.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyTaskRepository extends JpaRepository<DailyTask, Long> {
    Optional<DailyTask> findByUserIdAndTaskDate(Long userId, LocalDate date);
    List<DailyTask> findByUserIdOrderByTaskDateDesc(Long userId);
    List<DailyTask> findByUserId(Long userId);
}
