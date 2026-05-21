package com.board.focus.service;

import com.board.domain.user.User;
import com.board.domain.user.UserRepository;
import com.board.exception.CustomException;
import com.board.exception.ErrorCode;
import com.board.focus.domain.*;
import com.board.focus.dto.MemoRequest;
import com.board.focus.dto.MemoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemoService {

    private final MemoRepository memoRepository;
    private final UserRepository userRepository;

    @Transactional
    public MemoResponse save(String username, MemoRequest request) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Memo memo = memoRepository.save(Memo.builder()
            .user(user)
            .content(request.content())
            .build());

        return MemoResponse.from(memo);
    }

    @Transactional(readOnly = true)
    public List<MemoResponse> getAll(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return memoRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
            .stream().map(MemoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long countUnanalyzed(Long userId) {
        return memoRepository.findByUserIdAndAnalyzedFalseOrderByCreatedAtAsc(userId).size();
    }
}
