package com.board.focus.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TaskCandidateRepository extends JpaRepository<TaskCandidate, Long> {
    List<TaskCandidate> findByUserIdAndStatusOrderByTotalScoreDesc(Long userId, CandidateStatus status);
    List<TaskCandidate> findByUserIdOrderByTotalScoreDesc(Long userId);
}
