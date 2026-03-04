package com.fpsweeper.harvest.points.controller;

import com.fpsweeper.harvest.points.ChainService;
import com.fpsweeper.harvest.points.DepositService;
import com.fpsweeper.harvest.points.SupportedChain;
import com.fpsweeper.harvest.points.dto.ChainInfoResponse;
import com.fpsweeper.harvest.points.dto.PricingInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chains")
@CrossOrigin(origins = "*")
public class ChainController {

    @Autowired
    private ChainService chainService;

    @Autowired
    private DepositService depositService;

    /**
     * Get all supported chains
     * GET /api/chains/supported
     */
    @GetMapping("/supported")
    public ResponseEntity<List<ChainInfoResponse>> getSupportedChains() {
        List<SupportedChain> chains = chainService.getActiveChains();

        List<ChainInfoResponse> response = chains.stream()
                .map(chain -> new ChainInfoResponse(
                        chain.getChainName(),
                        chain.getPlatformWalletAddress(),
                        chain.getMinDepositUsd(),
                        chain.getUsdcTokenAddress()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get pricing information
     * GET /api/chains/pricing
     */
    @GetMapping("/pricing")
    public ResponseEntity<PricingInfo> getPricing() {
        // Conversion rate: $1 = 0.5 points (so $100 = 50 points)
        BigDecimal conversionRate = new BigDecimal("0.5");

        // Define pricing packages
        List<PricingInfo.Package> packages = new ArrayList<>();

        packages.add(new PricingInfo.Package(
                "Starter",
                new BigDecimal("10.00"),
                depositService.calculatePoints(new BigDecimal("10.00")),
                BigDecimal.ZERO
        ));

        packages.add(new PricingInfo.Package(
                "Basic",
                new BigDecimal("25.00"),
                depositService.calculatePoints(new BigDecimal("25.00")),
                BigDecimal.ZERO
        ));

        packages.add(new PricingInfo.Package(
                "Pro",
                new BigDecimal("50.00"),
                depositService.calculatePoints(new BigDecimal("50.00")),
                new BigDecimal("2.5")  // 5% bonus
        ));

        packages.add(new PricingInfo.Package(
                "Premium",
                new BigDecimal("100.00"),
                depositService.calculatePoints(new BigDecimal("100.00")),
                new BigDecimal("10.0")  // 10% bonus
        ));

        packages.add(new PricingInfo.Package(
                "Elite",
                new BigDecimal("250.00"),
                depositService.calculatePoints(new BigDecimal("250.00")),
                new BigDecimal("37.5")  // 15% bonus
        ));

        PricingInfo pricingInfo = new PricingInfo(conversionRate, packages);

        return ResponseEntity.ok(pricingInfo);
    }

    /**
     * Calculate points for a given USD amount
     * GET /api/chains/calculate?amount=100
     */
    @GetMapping("/calculate")
    public ResponseEntity<Map<String, BigDecimal>> calculatePoints(
            @RequestParam BigDecimal amount
    ) {
        BigDecimal points = depositService.calculatePoints(amount);

        return ResponseEntity.ok(Map.of(
                "amountUsd", amount,
                "points", points
        ));
    }
}