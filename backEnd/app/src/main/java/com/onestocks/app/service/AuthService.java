package com.onestocks.app.service;

import com.onestocks.app.dto.AuthResponse;
import com.onestocks.app.dto.LoginRequest;
import com.onestocks.app.dto.SignupRequest;
import com.onestocks.app.model.User;
import com.onestocks.app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthResponse signup(SignupRequest request) {
        
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return new AuthResponse(false, "Passwords do not match", null, null);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            return new AuthResponse(false, "Email is already registered", null, null);
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            return new AuthResponse(false, "Username is already taken", null, null);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRememberMe(request.isRememberMe());

        userRepository.save(user);

        return new AuthResponse(true, "Account created successfully", user.getUsername(), user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null) {
            return new AuthResponse(false, "Invalid email or password", null, null);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return new AuthResponse(false, "Invalid email or password", null, null);
        }

        return new AuthResponse(true, "Login successful", user.getUsername(), user.getEmail());
    }
}