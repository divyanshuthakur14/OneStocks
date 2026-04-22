package com.onestocks.app.controller;

import com.onestocks.app.dto.StockDetailResponse;
import com.onestocks.app.dto.StockSummaryResponse;
import com.onestocks.app.model.User;
import com.onestocks.app.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping
    public List<StockSummaryResponse> list() {
        return stockService.listAll();
    }

    @GetMapping("/{symbol}")
    public StockDetailResponse get(
            @AuthenticationPrincipal User user,
            @PathVariable String symbol) {
        return stockService.getDetail(symbol.toUpperCase(), user.getId());
    }
}
