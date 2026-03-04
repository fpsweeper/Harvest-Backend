package com.fpsweeper.harvest.points.controller;

import com.fpsweeper.harvest.points.PendingDeposit;
import com.fpsweeper.harvest.points.PendingDepositService;
import com.fpsweeper.harvest.points.dto.PendingDepositResponse;
import com.fpsweeper.harvest.user.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pending-deposits")
@CrossOrigin(origins = "*")
public class PendingDepositController {

    @Autowired
    private PendingDepositService pendingDepositService;

    /**
     * Get active pending deposits for current user
     * GET /api/pending-deposits
     */
    @GetMapping
    public ResponseEntity<List<PendingDepositResponse>> getActivePendingDeposits(
            @AuthenticationPrincipal Users user
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        List<PendingDeposit> pending = pendingDepositService.getActivePendingDeposits(user.getId());
        List<PendingDepositResponse> response = pending.stream()
                .map(PendingDepositResponse::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific pending deposit
     * GET /api/pending-deposits/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPendingDeposit(
            @AuthenticationPrincipal Users user,
            @PathVariable UUID id
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return pendingDepositService.getPendingDeposit(id, user.getId())
                .map(pending -> ResponseEntity.ok(new PendingDepositResponse(pending)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cancel a pending deposit
     * DELETE /api/pending-deposits/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelPendingDeposit(
            @AuthenticationPrincipal Users user,
            @PathVariable UUID id
    ) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            pendingDepositService.cancelPendingDeposit(id, user.getId());
            return ResponseEntity.ok(Map.of("message", "Pending deposit cancelled"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}