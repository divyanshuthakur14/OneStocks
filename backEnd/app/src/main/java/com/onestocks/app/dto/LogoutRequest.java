package com.onestocks.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogoutRequest {

    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
    
}
