package com.board.focus.domain;

import com.board.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "focus_memos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Memo {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private boolean analyzed;
    private LocalDateTime analyzedAt;
    private LocalDateTime createdAt;

    @Builder
    public Memo(User user, String content) {
        this.user = user;
        this.content = content;
        this.analyzed = false;
        this.createdAt = LocalDateTime.now();
    }

    public void markAnalyzed() {
        this.analyzed = true;
        this.analyzedAt = LocalDateTime.now();
    }
}
