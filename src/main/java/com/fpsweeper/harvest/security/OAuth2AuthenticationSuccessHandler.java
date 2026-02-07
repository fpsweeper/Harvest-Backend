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
        System.out.println("Redirecting to frontend");

        // Just redirect to frontend - let frontend handle the completion
        response.sendRedirect(frontendUrl + "/profile?twitter=success");
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
