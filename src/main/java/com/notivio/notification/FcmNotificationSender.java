package com.notivio.notification;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.*;
import com.notivio.entity.DeviceToken;
import com.notivio.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Firebase Cloud Messaging push notification sender.
 * Handles single and multicast delivery with invalid token cleanup.
 */
@Slf4j
@Component
public class FcmNotificationSender {

    /**
     * Send push notification to a list of device tokens.
     * Returns list of invalid/expired tokens to be deactivated.
     */
    public List<String> send(List<DeviceToken> deviceTokens,
                             String title,
                             String body,
                             Map<String, String> data) {

        if (!isFcmAvailable()) {
            log.warn("FCM not initialized — skipping push notification");
            return List.of();
        }

        if (deviceTokens.isEmpty()) {
            log.debug("No device tokens, skipping push notification");
            return List.of();
        }

        List<String> invalidTokens = new ArrayList<>();

        if (deviceTokens.size() == 1) {
            // Single message
            String token = deviceTokens.get(0).getToken();
            try {
                sendSingleMessage(token, title, body, data);
                log.debug("FCM message sent to token: {}...", token.substring(0, Math.min(20, token.length())));
            } catch (FirebaseMessagingException e) {
                if (isInvalidToken(e)) {
                    invalidTokens.add(token);
                }
                log.error("FCM send failed for token: {}", e.getMessage());
            }
        } else {
            // Multicast
            List<String> tokens = deviceTokens.stream()
                    .map(DeviceToken::getToken)
                    .collect(Collectors.toList());
            invalidTokens.addAll(sendMulticast(tokens, title, body, data));
        }

        return invalidTokens;
    }

    private void sendSingleMessage(String token, String title, String body,
                                   Map<String, String> data) throws FirebaseMessagingException {
        Message.Builder builder = Message.builder()
                .setToken(token)
                .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setNotification(AndroidNotification.builder()
                                .setSound("default")
                                .build())
                        .build());

        if (data != null) {
            builder.putAllData(data);
        }

        FirebaseMessaging.getInstance().send(builder.build());
    }

    private List<String> sendMulticast(List<String> tokens, String title, String body,
                                        Map<String, String> data) {
        List<String> invalid = new ArrayList<>();
        try {
            MulticastMessage.Builder builder = MulticastMessage.builder()
                    .addAllTokens(tokens)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());

            if (data != null) {
                builder.putAllData(data);
            }

            BatchResponse response = FirebaseMessaging.getInstance()
                    .sendEachForMulticast(builder.build());

            log.info("FCM multicast: {}/{} sent successfully",
                    response.getSuccessCount(), tokens.size());

            // Collect invalid tokens
            List<SendResponse> responses = response.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                if (!responses.get(i).isSuccessful()) {
                    FirebaseMessagingException ex = responses.get(i).getException();
                    if (ex != null && isInvalidToken(ex)) {
                        invalid.add(tokens.get(i));
                    }
                }
            }

        } catch (FirebaseMessagingException e) {
            log.error("FCM multicast failed: {}", e.getMessage());
        }
        return invalid;
    }

    private boolean isFcmAvailable() {
        try {
            return !FirebaseApp.getApps().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isInvalidToken(FirebaseMessagingException e) {
        MessagingErrorCode code = e.getMessagingErrorCode();
        return code == MessagingErrorCode.INVALID_ARGUMENT ||
               code == MessagingErrorCode.UNREGISTERED;
    }
}
