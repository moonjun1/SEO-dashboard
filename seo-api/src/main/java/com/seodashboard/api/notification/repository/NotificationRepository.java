package com.seodashboard.api.notification.repository;

import com.seodashboard.common.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Notification> findByUserIdAndIsReadOrderByCreatedAtDesc(Long userId, boolean isRead, Pageable pageable);

    Page<Notification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, String type, Pageable pageable);

    Page<Notification> findByUserIdAndSeverityOrderByCreatedAtDesc(Long userId, String severity, Pageable pageable);

    long countByUserIdAndIsRead(Long userId, boolean isRead);

    @Query("SELECT n.severity, COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false GROUP BY n.severity")
    java.util.List<Object[]> countUnreadBySeverity(@Param("userId") Long userId);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);
}
