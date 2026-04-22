package com.onestocks.app.service;

import com.onestocks.app.model.TokenBlacklist;
import com.onestocks.app.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final TokenBlacklistRepository tokenBlacklistRepository;

    // Blacklist a token when user logs out
    public void blacklist(String token) {
        if (!tokenBlacklistRepository.existsByToken(token)) {
            TokenBlacklist entry = TokenBlacklist.builder()
                    .token(token)
                    .blacklistedAt(Instant.now())
                    .build();
            tokenBlacklistRepository.save(entry);
        }
    }

    // Check if a token has been blacklisted
    public boolean isBlacklisted(String token) {
        return tokenBlacklistRepository.existsByToken(token);
    }
}