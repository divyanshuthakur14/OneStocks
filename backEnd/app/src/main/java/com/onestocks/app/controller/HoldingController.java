package com.onestocks.app.controller;

import com.onestocks.app.dto.HoldingDTO;
import com.onestocks.app.service.HoldingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/holdings")
public class HoldingController {

    @Autowired
    private HoldingService holdingService;

    @GetMapping("/my")
    public ResponseEntity<List<HoldingDTO>> getMyHoldings(@RequestParam String username) {
        return ResponseEntity.ok(holdingService.getHoldingsForUser(username));
    }
}