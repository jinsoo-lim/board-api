package com.board.focus.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MemoRepository extends JpaRepository<Memo, Long> {
    List<Memo> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Memo> findByUserIdAndAnalyzedFalseOrderByCreatedAtAsc(Long userId);
    long countByUserId(Long userId);
}
