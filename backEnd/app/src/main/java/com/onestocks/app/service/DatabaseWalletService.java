package com.onestocks.app.service;

import com.onestocks.app.exception.InsufficientFundsException;
import com.onestocks.app.model.Wallet;
import com.onestocks.app.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class DatabaseWalletService implements WalletService {

    private static final BigDecimal SEED_BALANCE = new BigDecimal("100000.0000");

    private final WalletRepository walletRepository;

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long userId) {
        return walletRepository.findByUserId(userId)
                .map(Wallet::getBalance)
                .orElse(SEED_BALANCE);
    }

    @Override
    @Transactional
    public void debit(Long userId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> createSeedWallet(userId));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                    "Insufficient funds: requested " + amount + ", available " + wallet.getBalance());
        }

        wallet.setBalance(wallet.getBalance().subtract(amount));
        walletRepository.save(wallet);
    }

    @Override
    @Transactional
    public void credit(Long userId, BigDecimal amount) {
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> createSeedWallet(userId));

        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);
    }

    private Wallet createSeedWallet(Long userId) {
        Wallet wallet = Wallet.builder()
                .userId(userId)
                .balance(SEED_BALANCE)
                .build();
        return walletRepository.save(wallet);
    }
    
}
