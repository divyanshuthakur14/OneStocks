package com.onestocks.app.service;

import com.onestocks.app.model.Stock;
import com.onestocks.app.repository.StockRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class StockService {

    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public List<Stock> getAllStocks() {
        return stockRepository.findAll();
    }

    
    @Scheduled(fixedRate = 5000)
    public void simulatePriceUpdates() {
        List<Stock> stocks = stockRepository.findAll();
        if (stocks.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (Stock stock : stocks) {
            BigDecimal oldPrice = stock.getCurrentPrice();
            if (oldPrice == null) {
                continue;
            }

            double fluctuationPercent = random.nextDouble(-0.05, 0.05);
            BigDecimal delta = oldPrice.multiply(BigDecimal.valueOf(fluctuationPercent));

            BigDecimal newPrice = oldPrice.add(delta);
            if (newPrice.signum() < 0) {
                newPrice = BigDecimal.ZERO;
            }
            newPrice = newPrice.setScale(2, RoundingMode.HALF_UP);

            stock.setPreviousClose(oldPrice);
            stock.setCurrentPrice(newPrice);
            stock.setUpdatedAt(now);
        }

        stockRepository.saveAll(stocks);
    }
}
