package com.fpsweeper.harvest.security;

import com.fpsweeper.harvest.social.TwitterService;
import com.fpsweeper.harvest.user.UserRepository;
import com.fpsweeper.harvest.user.Users;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    @Override
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
    }

    private String extractEmailFromJwtCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if ("access_token".equals(cookie.getName()) ||
                    "authToken".equals(cookie.getName())) {

                try {
                    return jwtService.extractEmail(cookie.getValue());
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }
}
