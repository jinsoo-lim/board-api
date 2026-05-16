package com.board.domain.user;

import com.board.dto.auth.LoginRequest;
import com.board.dto.auth.SignupRequest;
import com.board.dto.auth.TokenResponse;
import com.board.exception.CustomException;
import com.board.exception.ErrorCode;
import com.board.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks UserService userService;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtProvider jwtProvider;

    @Test
    @DisplayName("회원가입 성공")
    void signup_success() {
        SignupRequest req = mockSignupRequest("user1", "pass1234", "user1@test.com");
        given(userRepository.existsByUsername("user1")).willReturn(false);
        given(userRepository.existsByEmail("user1@test.com")).willReturn(false);
        given(passwordEncoder.encode("pass1234")).willReturn("encoded");

        userService.signup(req);

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 아이디 중복")
    void signup_fail_username_duplicate() {
        SignupRequest req = mockSignupRequest("dup", "pass1234", "dup@test.com");
        given(userRepository.existsByUsername("dup")).willReturn(true);

        assertThatThrownBy(() -> userService.signup(req))
            .isInstanceOf(CustomException.class)
            .extracting(e -> ((CustomException) e).getErrorCode())
            .isEqualTo(ErrorCode.USERNAME_DUPLICATE);
    }

    @Test
    @DisplayName("회원가입 실패 - 이메일 중복")
    void signup_fail_email_duplicate() {
        SignupRequest req = mockSignupRequest("user2", "pass1234", "dup@test.com");
        given(userRepository.existsByUsername("user2")).willReturn(false);
        given(userRepository.existsByEmail("dup@test.com")).willReturn(true);

        assertThatThrownBy(() -> userService.signup(req))
            .isInstanceOf(CustomException.class)
            .extracting(e -> ((CustomException) e).getErrorCode())
            .isEqualTo(ErrorCode.EMAIL_DUPLICATE);
    }

    @Test
    @DisplayName("로그인 성공 - JWT 토큰 반환")
    void login_success() {
        User user = User.builder().username("user1").password("encoded").email("u@t.com").build();
        LoginRequest req = mockLoginRequest("user1", "pass1234");

        given(userRepository.findByUsername("user1")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("pass1234", "encoded")).willReturn(true);
        given(jwtProvider.generate("user1")).willReturn("jwt-token");

        TokenResponse response = userService.login(req);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUsername()).isEqualTo("user1");
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 사용자")
    void login_fail_user_not_found() {
        LoginRequest req = mockLoginRequest("ghost", "pass1234");
        given(userRepository.findByUsername("ghost")).willReturn(Optional.empty());

        assertThatThrownBy(() -> userService.login(req))
            .isInstanceOf(CustomException.class)
            .extracting(e -> ((CustomException) e).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_fail_wrong_password() {
        User user = User.builder().username("user1").password("encoded").email("u@t.com").build();
        LoginRequest req = mockLoginRequest("user1", "wrongpass");

        given(userRepository.findByUsername("user1")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongpass", "encoded")).willReturn(false);

        assertThatThrownBy(() -> userService.login(req))
            .isInstanceOf(CustomException.class)
            .extracting(e -> ((CustomException) e).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }

    private SignupRequest mockSignupRequest(String username, String password, String email) {
        try {
            SignupRequest req = new SignupRequest();
            setField(req, "username", username);
            setField(req, "password", password);
            setField(req, "email", email);
            return req;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private LoginRequest mockLoginRequest(String username, String password) {
        try {
            LoginRequest req = new LoginRequest();
            setField(req, "username", username);
            setField(req, "password", password);
            return req;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setField(Object obj, String field, String value) throws Exception {
        var f = obj.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(obj, value);
    }
}
