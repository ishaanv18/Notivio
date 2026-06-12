package com.notivio.security;

import com.notivio.entity.User;
import com.notivio.repository.UserRepository;
import com.notivio.service.GmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

/**
 * Loads and processes Google OAuth2 user info.
 * Creates new user accounts or updates existing ones on login.
 * Also saves Gmail OAuth2 tokens so the app can sync emails immediately.
 *
 * BUG FIX: The refresh token is NOT in userRequest.getAdditionalParameters().
 * It is only available via OAuth2AuthorizedClientService after the full auth flow.
 * We now save the refresh token from the OAuth2AuthorizedClient instead.
 *
 * NOTE: The refresh token is fetched in OAuth2SuccessHandler (which runs AFTER
 * this service) via OAuth2AuthorizedClientService, which has access to the fully
 * stored OAuth2AuthorizedClient. We store a placeholder here and update in
 * OAuth2SuccessHandler once the client is stored by Spring Security.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final GmailService   gmailService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        return processOAuth2User(userRequest, oAuth2User);
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String googleId = (String) attributes.get("sub");
        String email    = (String) attributes.get("email");
        String name     = (String) attributes.get("name");
        String picture  = (String) attributes.get("picture");

        log.debug("Processing OAuth2 user: email={}", email);

        User user = userRepository.findByGoogleId(googleId)
                .orElseGet(() -> userRepository.findByEmail(email)
                        .orElseGet(User::new));

        // Update profile fields
        user.setGoogleId(googleId);
        user.setEmail(email);
        user.setName(name);
        user.setProfilePictureUrl(picture);
        if (user.getIsActive() == null) user.setIsActive(true);

        User savedUser = userRepository.save(user);
        log.info("OAuth2 login successful for user: {}", email);

        // Save the access token now.
        // NOTE: The refresh token will be saved by OAuth2SuccessHandler which runs AFTER
        // Spring Security stores the OAuth2AuthorizedClient (making the refresh token accessible).
        try {
            String accessToken = userRequest.getAccessToken().getTokenValue();
            Long expiresIn = null;
            if (userRequest.getAccessToken().getExpiresAt() != null) {
                expiresIn = userRequest.getAccessToken().getExpiresAt().getEpochSecond()
                        - java.time.Instant.now().getEpochSecond();
            }

            // The refresh token is only available via OAuth2AuthorizedClientService
            // AFTER Spring Security stores it. OAuth2SuccessHandler will update it.
            // For now, only update the access token (preserves any existing refresh token).
            gmailService.saveGmailTokens(savedUser, accessToken, null, expiresIn);
            userRepository.save(savedUser);  // persist gmailConnected = true
            log.info("Gmail access token saved during OAuth2 login for: {}", email);
        } catch (Exception e) {
            log.warn("Could not save Gmail token during OAuth2 login for {}: {}", email, e.getMessage());
        }

        return new DefaultOAuth2User(
                Collections.emptyList(),
                attributes,
                "sub"
        );
    }
}
