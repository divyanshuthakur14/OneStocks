package com.onestocks.app.repository;

import com.onestocks.app.model.Transaction;
import com.onestocks.app.model.TransactionType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByUserId(Long userId, Pageable pageable);

    Page<Transaction> findByUserIdAndType(Long userId, TransactionType type, Pageable pageable);

    Page<Transaction> findByUserIdAndStock_Symbol(Long userId, String symbol, Pageable pageable);

    Page<Transaction> findByUserIdAndTypeAndStock_Symbol(Long userId, TransactionType type, String symbol, Pageable pageable);

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    List<Transaction> findByUserIdOrderByCreatedAtDesc(Long userId);

}
