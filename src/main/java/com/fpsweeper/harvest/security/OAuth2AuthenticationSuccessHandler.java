package com.fpsweeper.harvest.security;

import com.fpsweeper.harvest.social.OAuthLinkingToken;
import com.fpsweeper.harvest.social.OAuthLinkingTokenRepository;
import com.fpsweeper.harvest.social.TwitterService;
import com.fpsweeper.harvest.user.UserRepository;
import com.fpsweeper.harvest.user.Users;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private TwitterService twitterService;

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OAuthLinkingTokenRepository linkingTokenRepository;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.environment:development}")
    private String environment;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        System.out.println("===== Custom OAuth2 Success Handler =====");
        System.out.println("Frontend URL: " + frontendUrl);
        System.out.println("Environment: " + environment);

        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            response.sendRedirect(frontendUrl + "/profile");
            return;
        }

        OAuth2User oauth2User = oauthToken.getPrincipal();
        String twitterId = (String) oauth2User.getAttributes().get("id");
        String username  = (String) oauth2User.getAttributes().get("username");

        System.out.println("Twitter ID: " + twitterId);
        System.out.println("Twitter Username: " + username);

        // ✅ Check for linking token first
        String linkToken = request.getParameter("link_token");
        String userEmail = null;

        if (linkToken != null) {
            System.out.println("Found link_token parameter: " + linkToken);
            Optional<OAuthLinkingToken> tokenOpt = linkingTokenRepository.findByToken(linkToken);

            if (tokenOpt.isPresent()) {
                OAuthLinkingToken token = tokenOpt.get();

                if (token.getExpiresAt().isAfter(Instant.now())) {
                    userEmail = token.getUserEmail();
                    System.out.println("Found email from linking token: " + userEmail);
                    linkingTokenRepository.delete(token);
                } else {
                    System.out.println("Linking token expired");
                    linkingTokenRepository.delete(token);
                }
            }
        }

        // ✅ Fallback to JWT cookie
        if (userEmail == null) {
            userEmail = extractEmailFromJwtCookie(request);
            System.out.println("Extracted email from JWT: " + userEmail);
        }

        if (userEmail == null || userEmail.isEmpty()) {
            System.out.println("No logged-in user found (JWT missing)");
            response.sendRedirect(
                    frontendUrl + "/login?error=" +
                            URLEncoder.encode(
                                    "Please log in before linking Twitter",
                                    StandardCharsets.UTF_8
                            )
            );
            return;
        }

        try {
            OAuth2AuthorizedClient client =
                    authorizedClientService.loadAuthorizedClient(
                            oauthToken.getAuthorizedClientRegistrationId(),
                            oauthToken.getName()
                    );

            twitterService.linkTwitterAccount(userEmail, client);
            System.out.println("Successfully linked Twitter to user: " + userEmail);

            Users user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalStateException("User not found after Twitter link"));

            String token = jwtService.generateToken(user);

            // ✅ Set cookie with production settings and correct domain
            // ✅ Remove Domain parameter
            String cookieValue = String.format(
                    "access_token=%s; Path=/; Max-Age=%d; HttpOnly; Secure; SameSite=None",
                    token,
                    7 * 24 * 60 * 60
            );
            response.addHeader("Set-Cookie", cookieValue);

            System.out.println("Twitter linked successfully");
            System.out.println("Redirecting to: " + frontendUrl + "/profile?twitter=success");

            response.sendRedirect(frontendUrl + "/profile?twitter=success");

        } catch (Exception e) {
            System.err.println("Failed to link Twitter: " + e.getMessage());
            e.printStackTrace();

            String errorMessage =
                    URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);

            response.sendRedirect(
                    frontendUrl + "/profile?twitter=error&message=" + errorMessage
            );
        }
    }
    private String extractEmailFromLinkingToken(HttpServletRequest request) {
        // Get state parameter (contains our token)
        String state = request.getParameter("state");

        if (state != null) {
            try {
                Optional<OAuthLinkingToken> tokenOpt = linkingTokenRepository.findByToken(state);

                if (tokenOpt.isPresent()) {
                    OAuthLinkingToken token = tokenOpt.get();

                    // Check if expired
                    if (token.getExpiresAt().isAfter(Instant.now())) {
                        String email = token.getUserEmail();

                        // Delete token after use
                        linkingTokenRepository.delete(token);

                        System.out.println("Found email from linking token: " + email);
                        return email;
                    } else {
                        System.out.println("Linking token expired");
                        linkingTokenRepository.delete(token);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error reading linking token: " + e.getMessage());
            }
        }

        return null;
    }

    private String extractEmailFromJwtCookie(HttpServletRequest request) {
        // First, try to get from session (set before OAuth redirect)

        String emailFromToken = extractEmailFromLinkingToken(request);
        if (emailFromToken != null) {
            return emailFromToken;
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            String emailFromSession = (String) session.getAttribute("linking_user_email");
            if (emailFromSession != null) {
                System.out.println("Found email in session: " + emailFromSession);
                // Clean up after use
                session.removeAttribute("linking_user_email");
                return emailFromSession;
            }
        }

        // Fallback: try to get from JWT cookie
        if (request.getCookies() == null) {
            System.out.println("No cookies in request");
            return null;
        }

        System.out.println("Searching for JWT cookie...");
        for (Cookie cookie : request.getCookies()) {
            System.out.println("Found cookie: " + cookie.getName());

            if ("access_token".equals(cookie.getName()) ||
                    "authToken".equals(cookie.getName())) {

                try {
                    String email = jwtService.extractEmail(cookie.getValue());
                    System.out.println("Successfully extracted email from JWT: " + email);
                    return email;
                } catch (Exception e) {
                    System.err.println("Failed to extract email from cookie: " + e.getMessage());
                    return null;
                }
            }
        }

        System.out.println("JWT cookie not found");
        return null;
    }
}
