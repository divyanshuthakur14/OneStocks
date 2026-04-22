package com.onestocks.app.config;

import com.onestocks.app.model.Stock;
import com.onestocks.app.repository.StockRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedStocks(StockRepository stockRepository) {
        return args -> {
            if (stockRepository.count() > 0) {
                return;
            }

            Instant now = Instant.now();

            List<Stock> demoStocks = List.of(
                buildStock("AAPL",  "Apple Inc.",             "Technology",         "189.50", now),
                buildStock("MSFT",  "Microsoft Corporation",  "Technology",         "415.20", now),
                buildStock("GOOGL", "Alphabet Inc.",          "Communication",      "172.80", now),
                buildStock("AMZN",  "Amazon.com, Inc.",       "Consumer Cyclical",  "185.40", now),
                buildStock("TSLA",  "Tesla, Inc.",            "Consumer Cyclical",  "248.90", now),
                buildStock("META",  "Meta Platforms, Inc.",   "Communication",      "495.70", now),
                buildStock("NVDA",  "NVIDIA Corporation",     "Technology",         "118.25", now),
                buildStock("NFLX",  "Netflix, Inc.",          "Communication",      "680.15", now),
                buildStock("AMD",   "Advanced Micro Devices", "Technology",         "162.40", now),
                buildStock("INTC",  "Intel Corporation",      "Technology",          "31.75", now)
            );

            stockRepository.saveAll(demoStocks);
        };
    }

    private static Stock buildStock(String symbol, String name, String sector, String price, Instant now) {
        BigDecimal p = new BigDecimal(price);
        return Stock.builder()
                .symbol(symbol)
                .name(name)
                .sector(sector)
                .currentPrice(p)
                .previousClose(p)
                .updatedAt(now)
                .build();
    }
}
