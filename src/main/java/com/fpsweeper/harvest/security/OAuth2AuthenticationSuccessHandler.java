package com.fpsweeper.harvest.security;

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

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.environment:development}")
    private String environment;

    /*@Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {



        System.out.println("===== Custom OAuth2 Success Handler =====");

        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            response.sendRedirect(frontendUrl + "/profile");
            return;
        }


        OAuth2User oauth2User = oauthToken.getPrincipal();
        String twitterId = (String) oauth2User.getAttributes().get("id");
        String username  = (String) oauth2User.getAttributes().get("username");

        System.out.println("Twitter ID: " + twitterId);
        System.out.println("Twitter Username: " + username);

        String userEmail = extractEmailFromJwtCookie(request);

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

            // 🔗 Link Twitter to existing user
            twitterService.linkTwitterAccount(userEmail, client);

            // 🔍 Reload user from DB (now with twitter_id set)
            Users user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalStateException("User not found after Twitter link"));

            // 🔐 Generate fresh JWT
            String token = jwtService.generateToken(user);

            Cookie cookie = new Cookie("access_token", token);
            cookie.setHttpOnly(true);
            cookie.setSecure(false); // true in production
            cookie.setPath("/");
            cookie.setMaxAge(7 * 24 * 60 * 60);
            response.addCookie(cookie);

            // 🔥 CRITICAL FIX: replace OAuth2User with Users principal
            Authentication newAuth =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            authentication.getAuthorities()
                    );

            SecurityContextHolder.getContext().setAuthentication(newAuth);

            System.out.println("Twitter linked and security context unified");

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
    }*/

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

        String userEmail = extractEmailFromJwtCookie(request);
        System.out.println("Extracted email from JWT: " + userEmail);

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

            // 🔗 Link Twitter to existing user
            twitterService.linkTwitterAccount(userEmail, client);
            System.out.println("Successfully linked Twitter to user: " + userEmail);

            // 🔍 Reload user from DB (now with twitter_id set)
            Users user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalStateException("User not found after Twitter link"));

            // 🔐 Generate fresh JWT
            String token = jwtService.generateToken(user);

            // ✅ Set cookie with production-ready settings
            boolean isProduction = "production".equals(environment);

            String cookieValue;
            if (isProduction) {
                // Production: Secure + SameSite=None for cross-origin
                cookieValue = String.format(
                        "access_token=%s; Path=/; Max-Age=%d; HttpOnly; Secure; SameSite=None",
                        token,
                        7 * 24 * 60 * 60
                );
            } else {
                // Development: No Secure, SameSite=Lax
                cookieValue = String.format(
                        "access_token=%s; Path=/; Max-Age=%d; HttpOnly; SameSite=Lax",
                        token,
                        7 * 24 * 60 * 60
                );
            }
            response.addHeader("Set-Cookie", cookieValue);

            Authentication newAuth =
                    new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            authentication.getAuthorities()
                    );

            SecurityContextHolder.getContext().setAuthentication(newAuth);

            System.out.println("Twitter linked and security context unified");
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

    private String extractEmailFromJwtCookie(HttpServletRequest request) {
        // First, try to get from session (set before OAuth redirect)
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
