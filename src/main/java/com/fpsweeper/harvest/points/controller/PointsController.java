package com.fpsweeper.harvest.points.controller;

import com.fpsweeper.harvest.points.PointTransaction;
import com.fpsweeper.harvest.points.PointsService;
import com.fpsweeper.harvest.points.dto.PointsBalanceResponse;
import com.fpsweeper.harvest.user.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/points")
@CrossOrigin(origins = "*")
public class PointsController {

    @Autowired
    private PointsService pointsService;

    /**
     * Get user's current points balance
     * GET /api/points/balance
     */
    @GetMapping("/balance")
    public ResponseEntity<PointsBalanceResponse> getBalance(
            @AuthenticationPrincipal Users user
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        PointsBalanceResponse response = new PointsBalanceResponse(
                pointsService.getBalance(user.getId())
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get user's transaction history
     * GET /api/points/transactions?page=0&size=20
     */
    @GetMapping("/transactions")
    public ResponseEntity<Page<PointTransaction>> getTransactionHistory(
            @AuthenticationPrincipal Users user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        Page<PointTransaction> transactions = pointsService.getTransactionHistory(
                user.getId(),
                page,
                size
        );

        return ResponseEntity.ok(transactions);
    }

    /**
     * Get user's points summary
     * GET /api/points/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @AuthenticationPrincipal Users user
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("balance", pointsService.getBalance(user.getId()));
        summary.put("recentTransactions", pointsService.getTransactionHistory(user.getId(), 0, 5));

        return ResponseEntity.ok(summary);
    }
}