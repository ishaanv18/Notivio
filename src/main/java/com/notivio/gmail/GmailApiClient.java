package com.notivio.gmail;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.notivio.entity.GmailToken;
import com.notivio.entity.User;
import com.notivio.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Gmail API client wrapper.
 * Handles fetching messages, extracting content from MIME parts,
 * and building the Gmail service with user credentials.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GmailApiClient {

    private final EncryptionUtil encryptionUtil;

    @Value("${google.client.id:${GOOGLE_CLIENT_ID:}}")
    private String clientId;

    @Value("${google.client.secret:${GOOGLE_CLIENT_SECRET:}}")
    private String clientSecret;

    @Value("${gmail.max-emails-per-sync:50}")
    private Integer maxEmailsPerSync;

    private static final String APPLICATION_NAME = "Notivio";
    private static final String USER = "me";

    /**
     * Fetch a list of message IDs from Gmail inbox (unread, important).
     */
    public List<String> listMessageIds(GmailToken token, String query) throws Exception {
        Gmail service = buildGmailService(token);
        List<String> messageIds = new ArrayList<>();

        ListMessagesResponse response = service.users().messages()
                .list(USER)
                .setQ(query)
                .setMaxResults((long) maxEmailsPerSync)
                .execute();

        if (response.getMessages() != null) {
            response.getMessages().forEach(msg -> messageIds.add(msg.getId()));
        }

        log.debug("Listed {} message IDs for user query: {}", messageIds.size(), query);
        return messageIds;
    }

    /**
     * Fetch full message details by message ID.
     */
    public Message getMessage(GmailToken token, String messageId) throws Exception {
        Gmail service = buildGmailService(token);
        return service.users().messages()
                .get(USER, messageId)
                .setFormat("full")
                .execute();
    }

    /**
     * Extract the plain text body from a Gmail message.
     */
    public String extractPlainTextBody(Message message) {
        if (message.getPayload() == null) {
            return message.getSnippet();
        }
        String body = extractBodyFromPart(message.getPayload());
        return body != null ? body : message.getSnippet();
    }

    /**
     * Extract specific header value from message.
     */
    public String getHeader(Message message, String headerName) {
        if (message.getPayload() == null || message.getPayload().getHeaders() == null) {
            return null;
        }
        return message.getPayload().getHeaders().stream()
                .filter(h -> headerName.equalsIgnoreCase(h.getName()))
                .map(MessagePartHeader::getValue)
                .findFirst()
                .orElse(null);
    }

    private String extractBodyFromPart(MessagePart part) {
        if (part == null) return null;

        String mimeType = part.getMimeType();

        // Direct text/plain
        if ("text/plain".equals(mimeType) && part.getBody() != null) {
            String data = part.getBody().getData();
            if (data != null) {
                return new String(Base64.getUrlDecoder().decode(data));
            }
        }

        // Recurse into parts
        if (part.getParts() != null) {
            for (MessagePart subPart : part.getParts()) {
                String result = extractBodyFromPart(subPart);
                if (result != null && !result.isBlank()) {
                    return result;
                }
            }
        }

        return null;
    }

    /**
     * Build a Gmail service with credentials that support automatic token refresh.
     *
     * ROOT CAUSE FIX: OAuth2Credentials.create(accessToken) is access-token-only and
     * throws "does not support refreshing" when the token expires.
     *
     * FIX: Use UserCredentials, which holds the clientId + clientSecret + refreshToken
     * and lets the Google SDK automatically obtain a new access token when needed.
     */
    private Gmail buildGmailService(GmailToken token) throws GeneralSecurityException, IOException {
        String decryptedAccessToken  = encryptionUtil.decrypt(token.getAccessToken());
        String decryptedRefreshToken = token.getRefreshToken() != null
                ? encryptionUtil.decrypt(token.getRefreshToken())
                : null;

        com.google.auth.oauth2.OAuth2Credentials credentials;

        if (decryptedRefreshToken != null && !decryptedRefreshToken.isBlank()
                && clientId != null && !clientId.isBlank()
                && clientSecret != null && !clientSecret.isBlank()) {

            // Full UserCredentials: supports automatic access token refresh via refresh token
            com.google.auth.oauth2.AccessToken accessToken =
                    new com.google.auth.oauth2.AccessToken(
                            decryptedAccessToken,
                            token.getExpiresAt() != null
                                    ? java.util.Date.from(token.getExpiresAt().toInstant())
                                    : null);

            credentials = com.google.auth.oauth2.UserCredentials.newBuilder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRefreshToken(decryptedRefreshToken)
                    .setAccessToken(accessToken)
                    .build();

            log.debug("Gmail credentials built with refresh token support");
        } else {
            // Fallback: no refresh token stored — will fail when access token expires
            // This happens only on the very first sync before a re-login stores the refresh token
            log.warn("No refresh token available for Gmail credentials — token refresh will not work");
            credentials = com.google.auth.oauth2.OAuth2Credentials.create(
                    new com.google.auth.oauth2.AccessToken(
                            decryptedAccessToken,
                            token.getExpiresAt() != null
                                    ? java.util.Date.from(token.getExpiresAt().toInstant())
                                    : null));
        }

        com.google.api.client.http.HttpRequestInitializer requestInitializer =
                new com.google.auth.http.HttpCredentialsAdapter(credentials);

        return new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                requestInitializer)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}
