package com.onestocks.app.controller;

import com.onestocks.app.model.User;
import com.onestocks.app.service.WalletService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

import java.util.Map;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/balance")
    public Map<String, BigDecimal> getBalance(@AuthenticationPrincipal User user) {
        return Map.of("balance", walletService.getBalance(user.getId()));
    }

}
