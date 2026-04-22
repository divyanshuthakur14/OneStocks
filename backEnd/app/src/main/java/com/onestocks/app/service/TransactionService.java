package com.onestocks.app.service;

import com.onestocks.app.dto.ExecuteTransactionRequest;
import com.onestocks.app.dto.TransactionResponse;
import com.onestocks.app.exception.InvalidTransactionException;
import com.onestocks.app.exception.ResourceNotFoundException;
import com.onestocks.app.model.Holding;
import com.onestocks.app.model.Stock;
import com.onestocks.app.model.Transaction;
import com.onestocks.app.model.TransactionStatus;
import com.onestocks.app.model.TransactionType;
import com.onestocks.app.repository.HoldingRepository;
import com.onestocks.app.repository.StockRepository;
import com.onestocks.app.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final StockRepository stockRepository;
    private final TransactionRepository transactionRepository;
    private final HoldingRepository holdingRepository;
    private final WalletService walletService;

    @Transactional
    public TransactionResponse execute(Long userId, ExecuteTransactionRequest req) {
        log.info("Executing {} for user={} symbol={} qty={}",
                req.type(), userId, req.symbol(), req.quantity());

        Stock stock = stockRepository.findBySymbol(req.symbol())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock not found: " + req.symbol()));

        BigDecimal pricePerShare = stock.getCurrentPrice();
        BigDecimal totalAmount = pricePerShare.multiply(BigDecimal.valueOf(req.quantity()));

        if (req.type() == TransactionType.BUY) {
            executeBuy(userId, stock, req.quantity(), totalAmount);
        } else {
            executeSell(userId, stock, req.quantity(), totalAmount);
        }

        Transaction saved = transactionRepository.save(Transaction.builder()
                .userId(userId)
                .stock(stock)
                .type(req.type())
                .quantity(req.quantity())
                .pricePerShare(pricePerShare)
                .totalAmount(totalAmount)
                .status(TransactionStatus.EXECUTED)
                .build());

        BigDecimal balanceAfter = walletService.getBalance(userId);
        return toResponse(saved, balanceAfter);
    }

    private void executeBuy(Long userId, Stock stock, int qty, BigDecimal total) {
        walletService.debit(userId, total);

        Holding holding = holdingRepository
                .findByUserIdAndStockId(userId, stock.getId())
                .orElseGet(() -> Holding.builder()
                        .userId(userId)
                        .stock(stock)
                        .quantity(0)
                        .averageBuyPrice(BigDecimal.ZERO)
                        .build());

        BigDecimal existingValue = holding.getAverageBuyPrice()
                .multiply(BigDecimal.valueOf(holding.getQuantity()));
        BigDecimal newTotalValue = existingValue.add(total);
        int newQty = holding.getQuantity() + qty;
        BigDecimal newAvg = newTotalValue.divide(BigDecimal.valueOf(newQty), 4, RoundingMode.HALF_UP);

        holding.setQuantity(newQty);
        holding.setAverageBuyPrice(newAvg);
        holdingRepository.save(holding);
    }

    private void executeSell(Long userId, Stock stock, int qty, BigDecimal total) {
        Holding holding = holdingRepository
                .findByUserIdAndStockId(userId, stock.getId())
                .orElseThrow(() -> new InvalidTransactionException(
                        "You don't own any shares of " + stock.getSymbol()));

        if (holding.getQuantity() < qty) {
            throw new InvalidTransactionException(
                    "Insufficient shares. You own " + holding.getQuantity()
                            + " but tried to sell " + qty);
        }

        int remaining = holding.getQuantity() - qty;
        if (remaining == 0) {
            holdingRepository.delete(holding);
        } else {
            holding.setQuantity(remaining);
            holdingRepository.save(holding);
        }

        walletService.credit(userId, total);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> listUserTransactions(
            Long userId, TransactionType type, String symbol, Pageable pageable) {

        Page<Transaction> page;
        if (type != null && symbol != null) {
            page = transactionRepository.findByUserIdAndTypeAndStock_Symbol(userId, type, symbol, pageable);
        } else if (type != null) {
            page = transactionRepository.findByUserIdAndType(userId, type, pageable);
        } else if (symbol != null) {
            page = transactionRepository.findByUserIdAndStock_Symbol(userId, symbol, pageable);
        } else {
            page = transactionRepository.findByUserId(userId, pageable);
        }
        return page.map(txn -> toResponse(txn, null));
    }

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long id, Long userId) {
        Transaction txn = transactionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + id));
        return toResponse(txn, null);
    }

    private TransactionResponse toResponse(Transaction txn, BigDecimal balanceAfter) {
        return new TransactionResponse(
                txn.getId(),
                txn.getStock().getSymbol(),
                txn.getStock().getName(),
                txn.getType(),
                txn.getQuantity(),
                txn.getPricePerShare(),
                txn.getTotalAmount(),
                txn.getStatus(),
                txn.getCreatedAt(),
                balanceAfter
        );
    }
}
