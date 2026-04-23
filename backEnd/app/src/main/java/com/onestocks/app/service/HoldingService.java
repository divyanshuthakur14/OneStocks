package com.onestocks.app.service;

import com.onestocks.app.dto.HoldingDTO;
import com.onestocks.app.exception.ResourceNotFoundException;
import com.onestocks.app.model.Holding;
import com.onestocks.app.model.User;
import com.onestocks.app.repository.HoldingRepository;
import com.onestocks.app.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
//@Transactional(readOnly = true)
public class HoldingService {

    private final HoldingRepository holdingRepository;
    private final UserRepository userRepository;

    public List<HoldingDTO> getHoldingsForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        return holdingRepository.findByUserId(user.getId())
                .stream()
                .map(h -> toHoldingDTO(h, h.getStock().getCurrentPrice()))
                .toList();
    }

    private HoldingDTO toHoldingDTO(Holding h, BigDecimal currentPrice) {
        BigDecimal qty = BigDecimal.valueOf(h.getQuantity());
        BigDecimal currentValue = currentPrice.multiply(qty);
        BigDecimal costBasis = h.getAverageBuyPrice().multiply(qty);
        BigDecimal profitLoss = currentValue.subtract(costBasis);
        BigDecimal profitLossPercent = costBasis.compareTo(BigDecimal.ZERO) > 0
                ? profitLoss.multiply(new BigDecimal("100"))
                .divide(costBasis, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new HoldingDTO(
                h.getStock().getSymbol(),
                h.getStock().getName(),
                h.getQuantity(),
                h.getAverageBuyPrice(),
                currentPrice,
                currentValue,
                profitLoss,
                profitLossPercent
        );
    }
}