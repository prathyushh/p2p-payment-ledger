package com.p2ppayment.controller;

import com.p2ppayment.dto.NotificationResponse;
import com.p2ppayment.entity.Notification;
import com.p2ppayment.util.JwtTokenProvider;
import com.p2ppayment.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("hasRole('USER')")
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtTokenProvider jwtTokenProvider;

    public NotificationController(NotificationService notificationService,
                                 JwtTokenProvider jwtTokenProvider) {
        this.notificationService = notificationService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * GET /api/notifications
     * Returns paginated list of notifications for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("Authorization") String token) {
        
        Long userId = extractUserIdFromToken(token);
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        
        Page<Notification> notifications = notificationService.getNotifications(userId, pageable);
        Page<NotificationResponse> response = notifications.map(this::mapToDto);
        
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/notifications/unread-count
     * Returns count of unread notifications for the authenticated user.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @RequestHeader("Authorization") String token) {
        
        Long userId = extractUserIdFromToken(token);
        Long count = notificationService.getUnreadCount(userId);
        
        return ResponseEntity.ok(UnreadCountResponse.builder()
                .unreadCount(count)
                .build());
    }

    /**
     * PUT /api/notifications/{id}/read
     * Marks a specific notification as read.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        
        Long userId = extractUserIdFromToken(token);
        boolean updated = notificationService.markAsRead(id, userId);
        
        if (!updated) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok().build();
    }

    /**
     * PUT /api/notifications/read-all
     * Marks all notifications as read for the authenticated user.
     */
    @PutMapping("/read-all")
    public ResponseEntity<ReadAllResponse> markAllAsRead(
            @RequestHeader("Authorization") String token) {
        
        Long userId = extractUserIdFromToken(token);
        int updated = notificationService.markAllAsRead(userId);
        
        return ResponseEntity.ok(ReadAllResponse.builder()
                .markedAsReadCount(updated)
                .build());
    }

    private Long extractUserIdFromToken(String token) {
        String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        return jwtTokenProvider.getUserIdFromToken(cleanToken);
    }

    private NotificationResponse mapToDto(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .relatedTransactionId(
                    notification.getRelatedTransaction() != null 
                        ? notification.getRelatedTransaction().getId() 
                        : null
                )
                .createdAt(notification.getCreatedAt())
                .build();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UnreadCountResponse {
        private Long unreadCount;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ReadAllResponse {
        private Integer markedAsReadCount;
    }
}
