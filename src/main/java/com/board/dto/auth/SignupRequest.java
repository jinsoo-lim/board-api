package com.board.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Getter;

@Getter
public class SignupRequest {
    @NotBlank @Size(min = 3, max = 20)
    private String username;

    @NotBlank @Size(min = 8)
    private String password;

    @NotBlank @Email
    private String email;
}
