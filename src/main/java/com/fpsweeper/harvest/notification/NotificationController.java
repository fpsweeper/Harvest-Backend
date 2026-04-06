package com.fpsweeper.harvest.notification;

import com.fpsweeper.harvest.user.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * GET /api/notifications?limit=20
     * Returns latest notifications + unread count
     */
    @GetMapping
    public ResponseEntity<?> getNotifications(
            @AuthenticationPrincipal Users user,
            @RequestParam(defaultValue = "30") int limit
    ) {
        if (user == null) return unauthorized();
        try {
            List<UserNotification> notifications = notificationService.getNotifications(user.getId(), limit);
            long unreadCount = notificationService.getUnreadCount(user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("notifications", notifications);
            response.put("unreadCount", unreadCount);
            response.put("total", notifications.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/notifications/unread-count
     * Lightweight endpoint for polling the badge count only
     */
    @GetMapping("/unread-count")
    public ResponseEntity<?> getUnreadCount(@AuthenticationPrincipal Users user) {
        if (user == null) return unauthorized();
        try {
            long count = notificationService.getUnreadCount(user.getId());
            return ResponseEntity.ok(Map.of("unreadCount", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/notifications/read-all
     * Mark all notifications as read
     */
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllRead(@AuthenticationPrincipal Users user) {
        if (user == null) return unauthorized();
        try {
            notificationService.markAllRead(user.getId());
            return ResponseEntity.ok(Map.of("success", true, "message", "All notifications marked as read"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PUT /api/notifications/{id}/read
     * Mark single notification as read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markRead(
            @AuthenticationPrincipal Users user,
            @PathVariable UUID id
    ) {
        if (user == null) return unauthorized();
        try {
            notificationService.markRead(id, user.getId());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/notifications
     * Clear all notifications for user
     */
    @DeleteMapping
    public ResponseEntity<?> clearAll(@AuthenticationPrincipal Users user) {
        if (user == null) return unauthorized();
        try {
            notificationService.clearAll(user.getId());
            return ResponseEntity.ok(Map.of("success", true, "message", "All notifications cleared"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
    }
}