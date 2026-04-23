package com.onestocks.app.controller;

import com.onestocks.app.dto.ExecuteTransactionRequest;
import com.onestocks.app.dto.TransactionDTO;
import com.onestocks.app.dto.TransactionResponse;
import com.onestocks.app.model.TransactionType;
import com.onestocks.app.model.User;
import com.onestocks.app.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponse> execute(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ExecuteTransactionRequest request) {
        TransactionResponse response = transactionService.execute(user.getId(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public Page<TransactionResponse> list(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String symbol,
            @PageableDefault(size = 20, sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return transactionService.listUserTransactions(user.getId(), type, symbol, pageable);
    }

    @GetMapping("/{id}")
    public TransactionResponse get(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return transactionService.getTransaction(id, user.getId());
    }

    @GetMapping("/my")
    public ResponseEntity<List<TransactionDTO>> getMyTransactions(@RequestParam String username) {
        return ResponseEntity.ok(transactionService.getTransactionsForUser(username));
    }

}
