package com.board.focus.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserGoalWeightsRepository extends JpaRepository<UserGoalWeights, Long> {
    Optional<UserGoalWeights> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
