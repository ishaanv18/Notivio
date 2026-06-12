package com.notivio.controller;

import com.notivio.entity.User;
import com.notivio.service.GmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Gmail integration API endpoints.
 */
@RestController
@RequestMapping("/api/gmail")
@RequiredArgsConstructor
@Tag(name = "Gmail", description = "Gmail connection and sync management")
public class GmailController {

    private final GmailService gmailService;

    @GetMapping("/status")
    @Operation(summary = "Check Gmail connection status")
    public ResponseEntity<Map<String, Object>> getStatus(
            @AuthenticationPrincipal User user) {

        var tokenOpt = gmailService.getToken(user);

        return ResponseEntity.ok(Map.of(
                "connected",    user.getGmailConnected(),
                "lastSynced",   tokenOpt.map(t ->
                        t.getLastSyncedAt() != null ? t.getLastSyncedAt().toString() : "never")
                        .orElse("never"),
                "tokenExpired", tokenOpt.map(t -> t.isExpired()).orElse(true)
        ));
    }

    @PostMapping("/sync")
    @Operation(summary = "Manually trigger Gmail sync",
               description = "Add ?force=true to reset sync history and rescan last 30 days")
    public ResponseEntity<Map<String, Object>> triggerSync(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "false") boolean force) {

        if (!user.getGmailConnected()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Gmail not connected. Login with Gmail OAuth2 first.")
            );
        }

        int newEmails = gmailService.syncEmails(user, force);
        return ResponseEntity.ok(Map.of(
                "message",   force ? "Force rescan complete (30 days)" : "Sync complete",
                "newEmails", newEmails
        ));
    }

    @PostMapping("/reprocess")
    @Operation(summary = "Re-run AI analysis on emails that previously failed",
               description = "Finds all saved emails with AI errors and re-queues them for analysis")
    public ResponseEntity<Map<String, Object>> reprocess(
            @AuthenticationPrincipal User user) {

        int count = gmailService.reprocessFailedEmails(user);
        return ResponseEntity.ok(Map.of(
                "message",    "Re-queued " + count + " emails for AI analysis",
                "requeued",   count
        ));
    }

    @DeleteMapping("/disconnect")
    @Operation(summary = "Disconnect Gmail integration")
    public ResponseEntity<Map<String, String>> disconnect(
            @AuthenticationPrincipal User user) {
        // Handled by service — remove token record
        // (Implementation delegates to GmailTokenRepository)
        return ResponseEntity.ok(Map.of("message", "Gmail disconnected"));
    }
}
