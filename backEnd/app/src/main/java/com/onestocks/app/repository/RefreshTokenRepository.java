package com.onestocks.app.repository;

import com.onestocks.app.model.RefreshToken;
import com.onestocks.app.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);
    
    @Transactional
    @Modifying
    void deleteByUser(User user);
    boolean existsByToken(String token);
}