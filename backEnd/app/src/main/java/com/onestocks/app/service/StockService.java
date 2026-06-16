package com.onestocks.app.service;

import com.onestocks.app.dto.StockDetailResponse;
import com.onestocks.app.dto.StockSummaryResponse;
import com.onestocks.app.exception.ResourceNotFoundException;
import com.onestocks.app.model.Holding;
import com.onestocks.app.model.Stock;
import com.onestocks.app.repository.HoldingRepository;
import com.onestocks.app.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class StockService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final StockRepository stockRepository;
    private final HoldingRepository holdingRepository;
    private final WalletService walletService;

    @Transactional(readOnly = true)
    public List<StockSummaryResponse> listAll() {
        return stockRepository.findAll().stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public StockDetailResponse getDetail(String symbol, Long userId) {
        Stock stock = stockRepository.findBySymbol(symbol)
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found: " + symbol));

        StockDetailResponse.HoldingInfo holdingInfo = holdingRepository
                .findByUserIdAndStockId(userId, stock.getId())
                .map(h -> buildHoldingInfo(h, stock.getCurrentPrice()))
                .orElse(null);

        BigDecimal walletBalance = walletService.getBalance(userId);

        return new StockDetailResponse(
                stock.getSymbol(),
                stock.getName(),
                stock.getSector(),
                stock.getDescription(),
                stock.getCurrentPrice(),
                stock.getPreviousClose(),
                computeDayChangePercent(stock),
                holdingInfo,
                walletBalance
        );
    }

    @Scheduled(fixedRate = 5000)
    public void simulatePriceUpdates() {
        List<Stock> stocks = stockRepository.findAll();
        if (stocks.isEmpty()) return;

        Instant now = Instant.now();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (Stock stock : stocks) {
            BigDecimal oldPrice = stock.getCurrentPrice();
            if (oldPrice == null) continue;

            double fluctuationPercent = random.nextDouble(-0.005, 0.005);
            BigDecimal newPrice = oldPrice
                    .add(oldPrice.multiply(BigDecimal.valueOf(fluctuationPercent)))
                    .max(BigDecimal.ZERO)
                    .setScale(4, RoundingMode.HALF_UP);

            stock.setPreviousClose(oldPrice);
            stock.setCurrentPrice(newPrice);
            stock.setUpdatedAt(now);
        }

        stockRepository.saveAll(stocks);
    }

    private StockSummaryResponse toSummary(Stock stock) {
        return new StockSummaryResponse(
                stock.getId(),
                stock.getSymbol(),
                stock.getName(),
                stock.getSector(),
                stock.getCurrentPrice(),
                stock.getPreviousClose(),
                computeDayChangePercent(stock),
                stock.getUpdatedAt()
        );
    }

    private BigDecimal computeDayChangePercent(Stock stock) {
        BigDecimal prev = stock.getPreviousClose();
        if (prev == null || prev.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return stock.getCurrentPrice()
                .subtract(prev)
                .multiply(HUNDRED)
                .divide(prev, 2, RoundingMode.HALF_UP);
    }

    private StockDetailResponse.HoldingInfo buildHoldingInfo(Holding holding, BigDecimal currentPrice) {
        BigDecimal qty = BigDecimal.valueOf(holding.getQuantity());
        BigDecimal currentValue = currentPrice.multiply(qty);
        BigDecimal costBasis = holding.getAverageBuyPrice().multiply(qty);
        BigDecimal profitLoss = currentValue.subtract(costBasis);

        BigDecimal profitLossPercent = BigDecimal.ZERO;
        if (costBasis.compareTo(BigDecimal.ZERO) > 0) {
            profitLossPercent = profitLoss
                    .multiply(HUNDRED)
                    .divide(costBasis, 2, RoundingMode.HALF_UP);
        }

        return new StockDetailResponse.HoldingInfo(
                holding.getQuantity(),
                holding.getAverageBuyPrice(),
                currentValue,
                profitLoss,
                profitLossPercent
        );
    }
}
