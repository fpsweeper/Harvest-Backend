package com.fpsweeper.harvest.logging;

import com.fpsweeper.harvest.user.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/logs")
@CrossOrigin(origins = "*")
public class LogController {

    @Autowired
    private AppLogRepository logRepository;

    // ── Auth guard ─────────────────────────────────────────────────────────

    private ResponseEntity<?> requireAdmin(Users user) {
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        if (!"ADMIN".equals(user.getRole())) return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        return null;
    }

    // ── GET /api/admin/logs ────────────────────────────────────────────────

    /**
     * Query logs with optional filters.
     *
     * Query params:
     *   page      (default 0)
     *   size      (default 50, max 200)
     *   level     ERROR | WARN | INFO | DEBUG | ALL
     *   search    free-text search in message and logger name
     *   since     last15m | last1h | last6h | last24h | all (default last1h)
     */
    @GetMapping
    public ResponseEntity<?> getLogs(
            @AuthenticationPrincipal Users user,
            @RequestParam(defaultValue = "0")    int    page,
            @RequestParam(defaultValue = "50")   int    size,
            @RequestParam(required = false)      String level,
            @RequestParam(required = false)      String search,
            @RequestParam(defaultValue = "last1h") String since
    ) {
        ResponseEntity<?> guard = requireAdmin(user);
        if (guard != null) return guard;

        try {
            size = Math.min(size, 200);
            PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

            String levelFilter = (level == null || level.isBlank() || "ALL".equalsIgnoreCase(level))
                    ? null : level.toUpperCase();
            String searchFilter = (search == null || search.isBlank()) ? null : search;

            Instant sinceInstant = parseSince(since);

            Page<AppLog> logs = sinceInstant != null
                    ? logRepository.findFilteredSince(levelFilter, searchFilter, sinceInstant, pageable)
                    : logRepository.findFiltered(levelFilter, searchFilter, pageable);

            // Summary counts for the badges
            Instant last24h = Instant.now().minus(24, ChronoUnit.HOURS);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("logs",          logs.getContent());
            result.put("totalPages",    logs.getTotalPages());
            result.put("totalElements", logs.getTotalElements());
            result.put("page",          page);
            result.put("size",          size);
            result.put("summary", Map.of(
                    "errors",   logRepository.countByLevel("ERROR"),
                    "warns",    logRepository.countByLevel("WARN"),
                    "infos",    logRepository.countByLevel("INFO"),
                    "debugs",   logRepository.countByLevel("DEBUG"),
                    "recent",   logRepository.countErrorsSince(last24h)
            ));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── DELETE /api/admin/logs ─────────────────────────────────────────────

    /**
     * Delete old logs.
     * Query param: olderThan = 7d | 30d | 90d (default 30d)
     */
    @DeleteMapping
    public ResponseEntity<?> purgeLogs(
            @AuthenticationPrincipal Users user,
            @RequestParam(defaultValue = "30d") String olderThan
    ) {
        ResponseEntity<?> guard = requireAdmin(user);
        if (guard != null) return guard;

        try {
            long days = switch (olderThan) {
                case "7d"  -> 7;
                case "90d" -> 90;
                default    -> 30;
            };
            Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
            int deleted = logRepository.deleteOlderThan(cutoff);
            return ResponseEntity.ok(Map.of(
                    "message", "Deleted " + deleted + " log entries older than " + days + " days",
                    "deleted", deleted
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private Instant parseSince(String since) {
        if (since == null) return Instant.now().minus(1, ChronoUnit.HOURS);
        return switch (since) {
            case "last15m" -> Instant.now().minus(15, ChronoUnit.MINUTES);
            case "last1h"  -> Instant.now().minus(1,  ChronoUnit.HOURS);
            case "last6h"  -> Instant.now().minus(6,  ChronoUnit.HOURS);
            case "last24h" -> Instant.now().minus(24, ChronoUnit.HOURS);
            case "all"     -> null;
            default        -> Instant.now().minus(1, ChronoUnit.HOURS);
        };
    }
}