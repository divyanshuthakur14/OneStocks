package com.onestocks.app.service;

import java.math.BigDecimal;

public interface WalletService {

    BigDecimal getBalance(Long userId);

    void debit(Long userId, BigDecimal amount);

    void credit(Long userId, BigDecimal amount);
}
