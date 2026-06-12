package com.notivio.controller;

import com.notivio.entity.DeviceToken;
import com.notivio.entity.Notification;
import com.notivio.entity.User;
import com.notivio.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Push notification and device token management")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/device-token")
    @Operation(summary = "Register a device token for push notifications")
    public ResponseEntity<DeviceToken> registerToken(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody RegisterTokenRequest request) {

        DeviceToken.DevicePlatform platform;
        try {
            platform = DeviceToken.DevicePlatform.valueOf(
                    request.getPlatform().toUpperCase());
        } catch (Exception e) {
            platform = DeviceToken.DevicePlatform.ANDROID;
        }

        DeviceToken token = notificationService.registerDeviceToken(
                user, request.getToken(), platform, request.getDeviceName());
        return ResponseEntity.ok(token);
    }

    @DeleteMapping("/device-token")
    @Operation(summary = "Remove a device token")
    public ResponseEntity<Map<String, String>> deregisterToken(
            @AuthenticationPrincipal User user,
            @RequestParam String token) {
        notificationService.deregisterDeviceToken(user, token);
        return ResponseEntity.ok(Map.of("message", "Token deregistered"));
    }

    @GetMapping
    @Operation(summary = "Get notification history (paginated)")
    public ResponseEntity<Page<Notification>> getNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                notificationService.getNotifications(user.getId(), PageRequest.of(page, size))
        );
    }

    @PostMapping("/mark-read")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<Map<String, Integer>> markAllRead(
            @AuthenticationPrincipal User user) {
        int count = notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(Map.of("markedAsRead", count));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                Map.of("unreadCount", notificationService.getUnreadCount(user.getId()))
        );
    }

    @Data
    static class RegisterTokenRequest {
        @NotBlank(message = "Token is required")
        private String token;
        private String platform = "ANDROID";
        private String deviceName;
    }
}
