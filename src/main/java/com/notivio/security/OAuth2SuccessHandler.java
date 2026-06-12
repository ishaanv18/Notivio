package com.notivio.security;

import com.notivio.entity.User;
import com.notivio.repository.UserRepository;
import com.notivio.service.GmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

/**
 * Handles successful OAuth2 authentication.
 *
 * SECURITY: Tokens are NOT passed as URL query params.
 * Uses a short-lived (2 min) signed stateless exchange code instead.
 *
 * REFRESH TOKEN FIX:
 * The refresh token is ONLY available AFTER Spring Security stores the
 * OAuth2AuthorizedClient. This handler runs after that storage, so we use
 * OAuth2AuthorizedClientService to fetch the refresh token and persist it here.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider          jwtTokenProvider;
    private final UserRepository            userRepository;
    private final GmailService              gmailService;
    private final OAuth2AuthorizedClientService authorizedClientService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String googleId = oAuth2User.getAttribute("sub");
        String email    = oAuth2User.getAttribute("email");

        User user = userRepository.findByGoogleId(googleId)
                .orElseThrow(() -> new RuntimeException("User not found after OAuth2 login"));

        // ── Save refresh token ────────────────────────────────────────────────────
        // OAuth2AuthorizedClientService has the full token response (access + refresh)
        // because Spring Security stores it right before calling this handler.
        try {
            OAuth2AuthorizedClient authorizedClient =
                    authorizedClientService.loadAuthorizedClient("google", authentication.getName());

            if (authorizedClient != null
                    && authorizedClient.getRefreshToken() != null
                    && authorizedClient.getRefreshToken().getTokenValue() != null) {

                String refreshToken = authorizedClient.getRefreshToken().getTokenValue();
                // Update the existing token row with the real refresh token
                gmailService.updateRefreshToken(user, refreshToken);
                log.info("Refresh token saved for user: {}", email);
            } else {
                log.warn("No refresh token available from OAuth2AuthorizedClient for: {}", email);
            }
        } catch (Exception e) {
            log.warn("Could not save refresh token for {}: {}", email, e.getMessage());
        }

        // ── Generate stateless exchange code ───────────────────────────────────────
        // Short-lived (2 min) signed JWT — only contains userId.
        // React StrictMode safe: stateless, each validation generates fresh tokens.
        String exchangeCode = jwtTokenProvider.generateExchangeCode(user.getId());
        log.info("OAuth2 success for {} — exchange code issued (stateless, 2 min TTL)", email);

        String targetUrl = UriComponentsBuilder
                .fromUriString(frontendUrl + "/auth/callback")
                .queryParam("code", exchangeCode)
                .build().toUriString();

        validateRedirectTarget(targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /** Prevent open-redirect: only allow redirects to our own frontend origin. */
    private void validateRedirectTarget(String url) {
        try {
            URI uri     = URI.create(url);
            URI allowed = URI.create(frontendUrl);
            if (!uri.getHost().equalsIgnoreCase(allowed.getHost())) {
                throw new IllegalArgumentException("Redirect host mismatch: " + uri.getHost());
            }
        } catch (IllegalArgumentException e) {
            throw new SecurityException("Invalid redirect target: " + url, e);
        }
    }
}
