package com.fpsweeper.harvest.points.controller;

import com.fpsweeper.harvest.points.jobs.DepositVerificationJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private DepositVerificationJob verificationJob;

    /**
     * Manually trigger deposit verification
     */
    @PostMapping("/verify-deposits")
    public ResponseEntity<?> triggerVerification() {
        try {
            verificationJob.verifyPendingDeposits();
            return ResponseEntity.ok(Map.of(
                    "message", "Verification job triggered successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to trigger verification: " + e.getMessage()
            ));
        }
    }
}