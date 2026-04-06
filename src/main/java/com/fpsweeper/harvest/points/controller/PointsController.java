package com.fpsweeper.harvest.points.controller;

import com.fpsweeper.harvest.points.PointTransaction;
import com.fpsweeper.harvest.points.PointsPackage;
import com.fpsweeper.harvest.points.PointsPackageRepository;
import com.fpsweeper.harvest.points.PointsService;
import com.fpsweeper.harvest.points.dto.PointsBalanceResponse;
import com.fpsweeper.harvest.user.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/points")
@CrossOrigin(origins = "*")
public class PointsController {

    @Autowired private PointsService pointsService;
    @Autowired private PointsPackageRepository packagesRepository;

    /**
     * GET /api/points/packages
     * Public — returns all active packages ordered by price.
     * No auth required so the landing page can fetch them.
     */
    @GetMapping("/packages")
    public ResponseEntity<?> getPackages() {
        List<PointsPackage> packages = packagesRepository.findByActiveTrueOrderBySortOrderAsc();
        return ResponseEntity.ok(Map.of("packages", packages));
    }

    /**
     * GET /api/points/balance
     */
    @GetMapping("/balance")
    public ResponseEntity<PointsBalanceResponse> getBalance(
            @AuthenticationPrincipal Users user) {
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(new PointsBalanceResponse(pointsService.getBalance(user.getId())));
    }

    /**
     * GET /api/points/transactions?page=0&size=20
     */
    @GetMapping("/transactions")
    public ResponseEntity<Page<PointTransaction>> getTransactionHistory(
            @AuthenticationPrincipal Users user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (user == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(pointsService.getTransactionHistory(user.getId(), page, size));
    }

    /**
     * GET /api/points/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @AuthenticationPrincipal Users user) {
        if (user == null) return ResponseEntity.status(401).build();
        Map<String, Object> summary = new HashMap<>();
        summary.put("balance", pointsService.getBalance(user.getId()));
        summary.put("recentTransactions", pointsService.getTransactionHistory(user.getId(), 0, 5));
        return ResponseEntity.ok(summary);
    }
}