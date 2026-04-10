package com.fpsweeper.harvest.points.controller;

import com.fpsweeper.harvest.notification.NotificationType;
import com.fpsweeper.harvest.notification.UserNotification;
import com.fpsweeper.harvest.notification.UserNotificationRepository;
import com.fpsweeper.harvest.points.*;
import com.fpsweeper.harvest.points.jobs.DepositVerificationJob;
import com.fpsweeper.harvest.trading.BotStatus;
import com.fpsweeper.harvest.trading.TradingBot;
import com.fpsweeper.harvest.trading.TradingBotRepository;
import com.fpsweeper.harvest.user.UserRepository;
import com.fpsweeper.harvest.user.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    // ── Repositories & Services ──────────────────────────────────────────────

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPointsRepository userPointsRepository;

    @Autowired
    private PointTransactionRepository pointTransactionRepository;

    @Autowired
    private PointDepositRepository depositRepository;          // PointDeposit repository

    @Autowired
    private TradingBotRepository tradingBotRepository;

    @Autowired
    private PointsService pointsService;

    @Autowired
    private PointsPackageRepository pointsPackageRepository;

    @Autowired
    private DepositVerificationJob verificationJob;

    // ── Auth guard helper ─────────────────────────────────────────────────────

    /**
     * Returns 403 if the current user is not ADMIN.
     * Every endpoint calls this first.
     */
    private ResponseEntity<?> requireAdmin(Users user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }
        if (!"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden — admin only"));
        }
        return null; // null = allowed
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  VERIFY
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * GET /api/admin/verify
     * Called by the frontend to check if the current user has admin access.
     */
    @GetMapping("/verify")
    public ResponseEntity<?> verify(@AuthenticationPrincipal Users user) {
        ResponseEntity<?> guard = requireAdmin(user);
        if (guard != null) return guard;
        return ResponseEntity.ok(Map.of("role", user.getRole(), "email", user.getEmail()));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PLATFORM STATS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * GET /api/admin/stats
     * Returns platform-wide aggregate statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@AuthenticationPrincipal Users user) {
        ResponseEntity<?> guard = requireAdmin(user);
        if (guard != null) return guard;

        try {
            long totalUsers        = userRepository.count();
            long totalBots         = tradingBotRepository.count();
            long activeBotsCount   = tradingBotRepository.countByStatus(BotStatus.SIMULATING);
            long totalTrades       = tradingBotRepository.findAll().stream()
                    .mapToLong(b -> b.getTrades().size()).sum();

            BigDecimal totalDepositsUsd    = depositRepository.sumConfirmedAmounts()
                    .orElse(BigDecimal.ZERO);
            BigDecimal totalPointsIssued   = depositRepository.sumConfirmedPoints()
                    .orElse(BigDecimal.ZERO);
            long confirmedDeposits         = depositRepository.countByStatus("CONFIRMED");
            long pendingDeposits           = depositRepository.countByStatus("PENDING");

            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("totalUsers",            totalUsers);
            stats.put("totalBots",             totalBots);
            stats.put("activeBotsCount",       activeBotsCount);
            stats.put("totalTradesExecuted",   totalTrades);
            stats.put("totalDepositsUsd",      totalDepositsUsd);
            stats.put("totalPointsIssued",     totalPointsIssued);
            stats.put("confirmedDepositsCount",confirmedDeposits);
            stats.put("pendingDepositsCount",  pendingDeposits);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  USERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * GET /api/admin/users?page=0&size=20&search=email
     * Paginated list of all users with their points balance, bot count, deposit count.
     */
    @GetMapping("/users")
    public ResponseEntity<?> getUsers(
            @AuthenticationPrincipal Users user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search
    ) {
        ResponseEntity<?> guard = requireAdmin(user);
        if (guard != null) return guard;

        try {
            List<Users> all;
            if (search != null && !search.isBlank()) {
                all = userRepository.findByEmailContainingIgnoreCase(search);
            } else {
                all = userRepository.findAll(Sort.by("createdAt").descending());
            }

            // Build enriched DTOs
            List<Map<String, Object>> enriched = all.stream().map(u -> {
                Map<String, Object> dto = new LinkedHashMap<>();
                dto.put("id",           u.getId());
                dto.put("email",        u.getEmail());
                dto.put("authProvider", u.getAuthProvider());
                dto.put("role",         u.getRole());
                dto.put("emailVerified",u.isEmailVerified());
                dto.put("enabled",      "ACTIVE".equals(u.getStatus()));
                dto.put("createdAt",    u.getCreatedAt());

                BigDecimal pts = userPointsRepository.findByUserId(u.getId())
                        .map(UserPoints::getPoints).orElse(BigDecimal.ZERO);
                dto.put("pointsBalance", pts);

                long botCount = tradingBotRepository.findByUserIdOrderByCreatedAtDesc(u.getId()).size();
                dto.put("totalBots", botCount);

                long depositCount = depositRepository.countByUserId(u.getId());
                dto.put("totalDeposits", depositCount);

                return dto;
            }).collect(Collectors.toList());

            // Manual pagination
            int start = page * size;
            int end   = Math.min(start + size, enriched.size());
            List<Map<String, Object>> pageContent = start >= enriched.size()
                    ? Collections.emptyList() : enriched.subList(start, end);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("users",      pageContent);
            result.put("totalPages", (int) Math.ceil((double) enriched.size() / size));
            result.put("totalElements", enriched.size());
            result.put("page",       page);
            result.put("size",       size);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/admin/users/{userId}/points
     * Body: { "operation": "add" | "deduct", "amount": 100 }
     * Manually credit or deduct points for a user.
     */
    @PostMapping("/users/{userId}/points")
    public ResponseEntity<?> adjustUserPoints(
            @AuthenticationPrincipal Users admin,
            @PathVariable UUID userId,
            @RequestBody Map<String, Object> body
    ) {
        ResponseEntity<?> guard = requireAdmin(admin);
        if (guard != null) return guard;

        try {
            String operation = (String) body.get("operation");
            BigDecimal amount = new BigDecimal(body.get("amount").toString());

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Amount must be positive"));
            }

            BigDecimal adjustedAmount = "deduct".equals(operation) ? amount.negate() : amount;
            String reason = "Admin " + operation + " by " + admin.getEmail();

            pointsService.adminAdjustPoints(userId, adjustedAmount, reason);

            BigDecimal newBalance = pointsService.getBalance(userId);
            return ResponseEntity.ok(Map.of(
                    "message", "Points " + operation + "ed successfully",
                    "newBalance", newBalance
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/admin/users/{userId}/enable
     * Re-enable a disabled user account.
     */
    @PutMapping("/users/{userId}/enable")
    public ResponseEntity<?> enableUser(
            @AuthenticationPrincipal Users admin,
            @PathVariable UUID userId
    ) {
        ResponseEntity<?> guard = requireAdmin(admin);
        if (guard != null) return guard;

        return setUserStatus(userId, "ACTIVE");
    }

    /**
     * PUT /api/admin/users/{userId}/disable
     * Disable a user account.
     */
    @PutMapping("/users/{userId}/disable")
    public ResponseEntity<?> disableUser(
            @AuthenticationPrincipal Users admin,
            @PathVariable UUID userId
    ) {
        ResponseEntity<?> guard = requireAdmin(admin);
        if (guard != null) return guard;

        return setUserStatus(userId, "DISABLED");
    }

    private ResponseEntity<?> setUserStatus(UUID userId, String status) {
        Optional<Users> opt = userRepository.findById(userId);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }
        Users u = opt.get();
        u.setStatus(status);
        u.setUpdatedAt(Instant.now());
        userRepository.save(u);
        return ResponseEntity.ok(Map.of("message", "User status set to " + status));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DEPOSITS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * GET /api/admin/deposits?page=0&size=20&status=PENDING
     * Paginated list of all deposits across all users.
     */
    @GetMapping("/deposits")
    public ResponseEntity<?> getDeposits(
            @AuthenticationPrincipal Users admin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status
    ) {
        ResponseEntity<?> guard = requireAdmin(admin);
        if (guard != null) return guard;

        try {
            PageRequest pageable = PageRequest.of(page, size, Sort.by("submittedAt").descending());

            Page<PointDeposit> deposits;
            if (status != null && !status.isBlank() && !"ALL".equals(status)) {
                deposits = depositRepository.findByStatusOrderBySubmittedAtDesc(status, pageable);
            } else {
                deposits = depositRepository.findAllByOrderBySubmittedAtDesc(pageable);
            }

            // Enrich with user email
            Page<Map<String, Object>> enriched = deposits.map(d -> {
                Map<String, Object> dto = new LinkedHashMap<>();
                dto.put("id",               d.getId());
                dto.put("userId",           d.getUserId());
                dto.put("transactionHash",  d.getTransactionHash());
                dto.put("chain",            d.getChain());
                dto.put("amountUsd",        d.getAmountUsd());
                dto.put("exactAmountUsd",   d.getExactAmountUsd() != null ? d.getExactAmountUsd() : d.getAmountUsd());
                dto.put("pointsIssued",     d.getPointsIssued());
                dto.put("status",           d.getStatus());
                dto.put("submittedAt",      d.getSubmittedAt());
                dto.put("confirmedAt",      d.getConfirmedAt());
                dto.put("failureReason",    d.getFailureReason());

                String email = userRepository.findById(d.getUserId())
                        .map(Users::getEmail).orElse("unknown");
                dto.put("userEmail", email);

                return dto;
            });

            return ResponseEntity.ok(enriched);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/admin/deposits/{depositId}/confirm
     * Manually confirm a PENDING deposit and credit points.
     */
    @PostMapping("/deposits/{depositId}/confirm")
    public ResponseEntity<?> confirmDeposit(
            @AuthenticationPrincipal Users admin,
            @PathVariable UUID depositId
    ) {
        ResponseEntity<?> guard = requireAdmin(admin);
        if (guard != null) return guard;

        try {
            Optional<PointDeposit> opt = depositRepository.findById(depositId);
            if (opt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Deposit not found"));
            }

            PointDeposit deposit = opt.get();

            if ("CONFIRMED".equals(deposit.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Deposit already confirmed"));
            }

            // Credit the points
            pointsService.addPoints(
                    deposit.getUserId(),
                    deposit.getPointsIssued(),
                    "Manual admin confirmation of deposit " + deposit.getTransactionHash(),
                    deposit.getId()
            );

            // Mark deposit as confirmed
            deposit.setStatus("CONFIRMED");
            deposit.setConfirmedAt(Instant.now());
            depositRepository.save(deposit);

            return ResponseEntity.ok(Map.of(
                    "message", "Deposit confirmed and " + deposit.getPointsIssued() + " points credited",
                    "pointsIssued", deposit.getPointsIssued()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/admin/deposits/{depositId}/fail
     * Body: { "reason": "string" }
     * Mark a PENDING deposit as FAILED.
     */
    @PostMapping("/deposits/{depositId}/fail")
    public ResponseEntity<?> failDeposit(
            @AuthenticationPrincipal Users admin,
            @PathVariable UUID depositId,
            @RequestBody(required = false) Map<String, String> body
    ) {
        ResponseEntity<?> guard = requireAdmin(admin);
        if (guard != null) return guard;

        try {
            Optional<PointDeposit> opt = depositRepository.findById(depositId);
            if (opt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Deposit not found"));
            }

            PointDeposit deposit = opt.get();

            if ("CONFIRMED".equals(deposit.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cannot fail a confirmed deposit"));
            }

            String reason = (body != null && body.get("reason") != null)
                    ? body.get("reason") : "Manual rejection by admin";

            deposit.setStatus("FAILED");
            deposit.setFailureReason(reason);
            depositRepository.save(deposit);

            return ResponseEntity.ok(Map.of("message", "Deposit marked as failed"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  BOTS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * GET /api/admin/bots?page=0&size=20&status=SIMULATING&search=name
     * Paginated list of all bots across all users.
     */
    @GetMapping("/bots")
    public ResponseEntity<?> getBots(
            @AuthenticationPrincipal Users admin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search
    ) {
        ResponseEntity<?> guard = requireAdmin(admin);
        if (guard != null) return guard;

        try {
            List<TradingBot> all;

            if (status != null && !status.isBlank() && !"ALL".equals(status)) {
                all = tradingBotRepository.findByStatus(BotStatus.valueOf(status));
            } else {
                all = tradingBotRepository.findAll(Sort.by("createdAt").descending());
            }

            // Optional name/email search
            if (search != null && !search.isBlank()) {
                String q = search.toLowerCase();
                List<UUID> matchingUserIds = userRepository
                        .findByEmailContainingIgnoreCase(search)
                        .stream().map(Users::getId).collect(Collectors.toList());

                all = all.stream().filter(b ->
                        b.getName().toLowerCase().contains(q) ||
                                matchingUserIds.contains(b.getUserId())
                ).collect(Collectors.toList());
            }

            // Enrich with user email
            List<Map<String, Object>> enriched = all.stream().map(b -> {
                Map<String, Object> dto = new LinkedHashMap<>();
                dto.put("id",             b.getId());
                dto.put("userId",         b.getUserId());
                dto.put("name",           b.getName());
                dto.put("strategyType",   b.getStrategyType());
                dto.put("tradingPair",    b.getTradingPair());
                dto.put("timeframe",      b.getTimeframe());
                dto.put("status",         b.getStatus());
                dto.put("initialBalance", b.getInitialBalance());
                dto.put("currentBalance", b.getCurrentBalance());
                dto.put("totalPnl",       b.getCurrentBalance().subtract(b.getInitialBalance()));
                dto.put("totalTrades",    b.getTrades().size());
                dto.put("createdAt",      b.getCreatedAt());
                dto.put("startedAt",      b.getStartedAt());

                String email = userRepository.findById(b.getUserId())
                        .map(Users::getEmail).orElse("unknown");
                dto.put("userEmail", email);

                return dto;
            }).collect(Collectors.toList());

            // Manual pagination
            int start = page * size;
            int end   = Math.min(start + size, enriched.size());
            List<Map<String, Object>> pageContent = start >= enriched.size()
                    ? Collections.emptyList() : enriched.subList(start, end);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("bots",         pageContent);
            result.put("totalPages",   (int) Math.ceil((double) enriched.size() / size));
            result.put("totalElements",enriched.size());
            result.put("page",         page);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/admin/bots/{botId}/stop
     * Force-stop any running bot.
     */
    @PutMapping("/bots/{botId}/stop")
    public ResponseEntity<?> forceStopBot(
            @AuthenticationPrincipal Users admin,
            @PathVariable UUID botId
    ) {
        ResponseEntity<?> guard = requireAdmin(admin);
        if (guard != null) return guard;

        try {
            Optional<TradingBot> opt = tradingBotRepository.findById(botId);
            if (opt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Bot not found"));
            }

            TradingBot bot = opt.get();
            bot.setStatus(BotStatus.STOPPED);
            bot.setStoppedAt(Instant.now());
            tradingBotRepository.save(bot);

            return ResponseEntity.ok(Map.of("message", "Bot \"" + bot.getName() + "\" force-stopped by admin"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  NOTIFICATIONS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * POST /api/admin/notifications/broadcast
     * Body: { "type": "INFO", "title": "...", "message": "..." }
     * Send a notification to ALL users.
     */
    @PostMapping("/notifications/broadcast")
    public ResponseEntity<?> broadcastNotification(
            @AuthenticationPrincipal Users admin,
            @RequestBody Map<String, String> body
    ) {
        ResponseEntity<?> guard = requireAdmin(admin);
        if (guard != null) return guard;

        try {
            String type    = body.getOrDefault("type", "INFO");
            String title   = body.get("title");
            String message = body.get("message");

            if (title == null || title.isBlank() || message == null || message.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "title and message are required"));
            }

            List<Users> allUsers = userRepository.findAll();
            int count = 0;

            NotificationType notifType = parseNotifType(type);
            for (Users u : allUsers) {
                UserNotification notif = new UserNotification(u.getId(), notifType, title, message);
                userNotificationRepository.save(notif);
                count++;
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Notification broadcast to " + count + " users",
                    "count", count
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/admin/notifications/user/{userId}
     * Body: { "type": "INFO", "title": "...", "message": "..." }
     * Send a notification to a specific user.
     */
    @PostMapping("/notifications/user/{userId}")
    public ResponseEntity<?> sendUserNotification(
            @AuthenticationPrincipal Users admin,
            @PathVariable UUID userId,
            @RequestBody Map<String, String> body
    ) {
        ResponseEntity<?> guard = requireAdmin(admin);
        if (guard != null) return guard;

        try {
            if (!userRepository.existsById(userId)) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            String type    = body.getOrDefault("type", "INFO");
            String title   = body.get("title");
            String message = body.get("message");

            if (title == null || title.isBlank() || message == null || message.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "title and message are required"));
            }

            UserNotification notif = new UserNotification(userId, parseNotifType(type), title, message);
            userNotificationRepository.save(notif);

            return ResponseEntity.ok(Map.of("message", "Notification sent to user " + userId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/admin/notifications/recent?size=20
     * Recent platform notifications for admin review.
     */
    @GetMapping("/notifications/recent")
    public ResponseEntity<?> getRecentNotifications(
            @AuthenticationPrincipal Users admin,
            @RequestParam(defaultValue = "20") int size
    ) {
        ResponseEntity<?> guard = requireAdmin(admin);
        if (guard != null) return guard;

        try {
            PageRequest pageable = PageRequest.of(0, size, Sort.by("createdAt").descending());
            Page<UserNotification> page = userNotificationRepository.findAll(pageable);
            return ResponseEntity.ok(Map.of(
                    "notifications", page.getContent(),
                    "total", page.getTotalElements()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  POINTS PACKAGES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * POST /api/admin/packages
     * Create a new points package.
     */
    @PostMapping("/packages")
    public ResponseEntity<?> createPackage(
            @AuthenticationPrincipal Users admin,
            @RequestBody Map<String, Object> body
    ) {
        ResponseEntity<?> guard = requireAdmin(admin);
        if (guard != null) return guard;

        try {
            PointsPackage pkg = buildPackageFromBody(body, null);
            PointsPackage saved = pointsPackageRepository.save(pkg);
            return ResponseEntity.status(201).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/admin/packages/{packageId}
     * Update an existing points package.
     */
    @PutMapping("/packages/{packageId}")
    public ResponseEntity<?> updatePackage(
            @AuthenticationPrincipal Users admin,
            @PathVariable UUID packageId,
            @RequestBody Map<String, Object> body
    ) {
        ResponseEntity<?> guard = requireAdmin(admin);
        if (guard != null) return guard;

        try {
            Optional<PointsPackage> opt = pointsPackageRepository.findById(packageId);
            if (opt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "Package not found"));
            }
            PointsPackage pkg = buildPackageFromBody(body, opt.get());
            PointsPackage saved = pointsPackageRepository.save(pkg);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/admin/packages/{packageId}
     * Delete a points package.
     */
    @DeleteMapping("/packages/{packageId}")
    public ResponseEntity<?> deletePackage(
            @AuthenticationPrincipal Users admin,
            @PathVariable UUID packageId
    ) {
        ResponseEntity<?> guard = requireAdmin(admin);
        if (guard != null) return guard;

        try {
            if (!pointsPackageRepository.existsById(packageId)) {
                return ResponseEntity.status(404).body(Map.of("error", "Package not found"));
            }
            pointsPackageRepository.deleteById(packageId);
            return ResponseEntity.ok(Map.of("message", "Package deleted"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private PointsPackage buildPackageFromBody(Map<String, Object> body, PointsPackage existing) {
        PointsPackage pkg = existing != null ? existing : new PointsPackage();
        if (body.containsKey("name"))        pkg.setName((String) body.get("name"));
        if (body.containsKey("description")) pkg.setDescription((String) body.get("description"));
        if (body.containsKey("points"))      pkg.setPoints(new BigDecimal(body.get("points").toString()));
        if (body.containsKey("priceUsd"))    pkg.setPriceUsd(new BigDecimal(body.get("priceUsd").toString()));
        if (body.containsKey("popular"))     pkg.setPopular(Boolean.TRUE.equals(body.get("popular")));
        if (body.containsKey("sortOrder"))   pkg.setSortOrder(Integer.parseInt(body.get("sortOrder").toString()));
        return pkg;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  LEGACY — kept for backward compatibility
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * POST /api/admin/verify-deposits
     * Manually trigger the deposit verification job.
     */
    @PostMapping("/verify-deposits")
    public ResponseEntity<?> triggerVerification(@AuthenticationPrincipal Users admin) {
        ResponseEntity<?> guard = requireAdmin(admin);
        if (guard != null) return guard;
        try {
            verificationJob.verifyPendingDeposits();
            return ResponseEntity.ok(Map.of("message", "Verification job triggered successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Failed to trigger verification: " + e.getMessage()));
        }
    }

    // ── Notification type helper ──────────────────────────────────────────────

    /**
     * Safely parse a String to NotificationType.
     * Falls back to INFO if the value is unknown or null.
     */
    private NotificationType parseNotifType(String type) {
        if (type == null) return NotificationType.INFO;
        try {
            return NotificationType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NotificationType.INFO;
        }
    }

    // ── Injected repositories used only in this controller ────────────────

    @Autowired
    private UserNotificationRepository userNotificationRepository;
}