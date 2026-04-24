package com.onestocks.app.service;

import com.onestocks.app.dto.AuthResponse;
import com.onestocks.app.dto.LoginRequest;
import com.onestocks.app.dto.LogoutRequest;
import com.onestocks.app.dto.RefreshRequest;
import com.onestocks.app.dto.SignupRequest;
import com.onestocks.app.model.RefreshToken;
import com.onestocks.app.model.Role;
import com.onestocks.app.model.User;
import com.onestocks.app.repository.UserRepository;
import com.onestocks.app.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthResponse signup(SignupRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return AuthResponse.builder().success(false).message("Passwords do not match").build();
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return AuthResponse.builder().success(false).message("Email is already registered").build();
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            return AuthResponse.builder().success(false).message("Username is already taken").build();
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .rememberMe(request.isRememberMe())
                .role(Role.USER)
                .build();

        userRepository.save(user);

        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return AuthResponse.builder()
                .success(true)
                .message("Account created successfully")
                .token(accessToken)
                .refreshToken(refreshToken.getToken())
                .username(user.getDisplayName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

public AuthResponse login(LoginRequest request) {
    String identifier = request.getIdentifier();

    User user = userRepository.findByEmail(identifier)
            .orElseGet(() -> userRepository.findByUsername(identifier).orElse(null));

    if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
        return AuthResponse.builder().success(false).message("Invalid credentials").build();
    }

    String accessToken = jwtService.generateToken(user);
    RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

    return AuthResponse.builder()
            .success(true)
            .message("Login successful")
            .token(accessToken)
            .refreshToken(refreshToken.getToken())
            .username(user.getDisplayName())
            .email(user.getEmail())
            .role(user.getRole().name())
            .build();
}

    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenService.findByToken(request.getRefreshToken())
                .orElse(null);

        if (refreshToken == null) {
            return AuthResponse.builder().success(false).message("Invalid refresh token").build();
        }
        if (refreshTokenService.isRevoked(refreshToken)) {
            return AuthResponse.builder().success(false).message("Refresh token has been revoked").build();
        }
        if (refreshTokenService.isExpired(refreshToken)) {
            return AuthResponse.builder().success(false).message("Refresh token has expired, please login again").build();
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtService.generateToken(user);

        return AuthResponse.builder()
                .success(true)
                .message("Token refreshed successfully")
                .token(newAccessToken)
                .refreshToken(refreshToken.getToken())
                .username(user.getDisplayName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    public AuthResponse logout(LogoutRequest request) {
        // Blacklist the access token
        tokenBlacklistService.blacklist(request.getToken());

        // Revoke the refresh token
        User user = userRepository.findByEmail(
                jwtService.extractUsername(request.getToken())
        ).orElse(null);

        if (user != null) {
            refreshTokenService.revokeByUser(user);
        }

        return AuthResponse.builder()
                .success(true)
                .message("Logged out successfully")
                .build();
    }
}