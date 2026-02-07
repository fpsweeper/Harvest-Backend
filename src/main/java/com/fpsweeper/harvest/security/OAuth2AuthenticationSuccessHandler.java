package com.fpsweeper.harvest.security;

import com.fpsweeper.harvest.social.OAuthLinkingToken;
import com.fpsweeper.harvest.social.OAuthLinkingTokenRepository;
import com.fpsweeper.harvest.social.DiscordService;
import com.fpsweeper.harvest.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private DiscordService discordService;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OAuthLinkingTokenRepository linkingTokenRepository;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        System.out.println("===== Custom OAuth2 Success Handler =====");

        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            System.out.println("Not an OAuth2 authentication, redirecting to profile");
            response.sendRedirect(frontendUrl + "/profile");
            return;
        }

        OAuth2User oauth2User = oauthToken.getPrincipal();
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();

        System.out.println("OAuth Provider: " + registrationId);

        // Extract link_token from state parameter
        String state = request.getParameter("state");
        String userEmail = extractEmailFromState(state);

        if (userEmail == null) {
            System.out.println("No user email found - redirecting to login");
            response.sendRedirect(
                    frontendUrl + "/login?error=" +
                            URLEncoder.encode("Please log in before linking " + registrationId, StandardCharsets.UTF_8)
            );
            return;
        }

        // Handle different OAuth providers
        try {
            OAuth2AuthorizedClient client =
                    authorizedClientService.loadAuthorizedClient(
                            oauthToken.getAuthorizedClientRegistrationId(),
                            oauthToken.getName()
                    );
             if ("discord".equals(registrationId)) {
                handleDiscordLink(oauth2User, client, userEmail, response);
            } else {
                System.err.println("Unknown OAuth provider: " + registrationId);
                response.sendRedirect(frontendUrl + "/profile?error=unknown_provider");
            }

        } catch (Exception e) {
            System.err.println("Failed to link " + registrationId + ": " + e.getMessage());
            e.printStackTrace();

            String errorMessage = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
            response.sendRedirect(frontendUrl + "/profile?" + registrationId + "=error&message=" + errorMessage);
        }
    }

    /**
     * Extract user email from OAuth state parameter
     */
    private String extractEmailFromState(String state) {
        if (state == null || !state.contains(":")) {
            System.out.println("No valid state parameter found");
            return null;
        }

        // State format: "random_oauth_state:link_token"
        String[] parts = state.split(":", 2);
        if (parts.length != 2) {
            System.out.println("State parameter doesn't contain link token");
            return null;
        }

        String linkToken = parts[1];
        System.out.println("Extracted link_token from state: " + linkToken);

        Optional<OAuthLinkingToken> tokenOpt = linkingTokenRepository.findByToken(linkToken);

        if (tokenOpt.isEmpty()) {
            System.out.println("Token not found in database");
            return null;
        }

        OAuthLinkingToken token = tokenOpt.get();
        System.out.println("Token found: email=" + token.getUserEmail() + ", expires=" + token.getExpiresAt());

        if (token.getExpiresAt().isBefore(Instant.now())) {
            System.out.println("Token expired");
            linkingTokenRepository.delete(token);
            return null;
        }

        String userEmail = token.getUserEmail();
        System.out.println("Resolved user: " + userEmail);
        linkingTokenRepository.delete(token);

        return userEmail;
    }

    /**
     * Handle Discord account linking
     */
    private void handleDiscordLink(
            OAuth2User oauth2User,
            OAuth2AuthorizedClient client,
            String userEmail,
            HttpServletResponse response
    ) throws IOException {
        String discordId = (String) oauth2User.getAttributes().get("id");
        String username = (String) oauth2User.getAttributes().get("username");
        String discriminator = (String) oauth2User.getAttributes().get("discriminator");
        String globalName = (String) oauth2User.getAttributes().get("global_name");

        System.out.println("Linking Discord account:");
        System.out.println("  Discord ID: " + discordId);
        System.out.println("  Username: " + username);
        System.out.println("  Discriminator: " + discriminator);
        System.out.println("  Global Name: " + globalName);
        System.out.println("  User Email: " + userEmail);

        discordService.linkDiscordAccount(userEmail, client);
        System.out.println("✅ Successfully linked Discord to user: " + userEmail);

        response.sendRedirect(frontendUrl + "/profile?discord=success");
    }
}