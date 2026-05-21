package com.board.focus.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TaskFeedbackRepository extends JpaRepository<TaskFeedback, Long> {
    Optional<TaskFeedback> findByDailyTaskId(Long dailyTaskId);
    boolean existsByDailyTaskId(Long dailyTaskId);

    @Query("SELECT f FROM TaskFeedback f WHERE f.user.id = :userId AND f.createdAt >= :since ORDER BY f.createdAt DESC")
    List<TaskFeedback> findRecentByUserId(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
