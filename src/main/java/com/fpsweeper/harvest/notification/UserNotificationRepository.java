package com.fpsweeper.harvest.notification;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {

    // Get latest notifications for a user (paginated, newest first)
    List<UserNotification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    // Count unread notifications
    long countByUserIdAndReadFalse(UUID userId);

    // Mark all as read
    @Modifying
    @Query("UPDATE UserNotification n SET n.read = true WHERE n.userId = :userId AND n.read = false")
    void markAllAsRead(@Param("userId") UUID userId);

    // Mark single as read
    @Modifying
    @Query("UPDATE UserNotification n SET n.read = true WHERE n.id = :id AND n.userId = :userId")
    void markAsRead(@Param("id") UUID id, @Param("userId") UUID userId);

    // Delete all for user (clear all)
    void deleteByUserId(UUID userId);

    // Delete old read notifications (cleanup)
    @Modifying
    @Query("DELETE FROM UserNotification n WHERE n.userId = :userId AND n.read = true AND n.createdAt < :cutoff")
    void deleteOldReadNotifications(@Param("userId") UUID userId, @Param("cutoff") java.time.Instant cutoff);
}