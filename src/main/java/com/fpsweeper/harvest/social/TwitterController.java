package com.fpsweeper.harvest.social;

import com.fpsweeper.harvest.security.JwtService;
import com.fpsweeper.harvest.social.dto.TwitterAccountResponse;
import com.fpsweeper.harvest.user.UserRepository;
import com.fpsweeper.harvest.user.Users;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/social/twitter")
public class TwitterController {

    @Autowired
    private TwitterService twitterService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @GetMapping
    public ResponseEntity<?> getLinkedTwitter(Authentication authentication, HttpServletRequest request) {
        System.out.println("===== GET TWITTER ACCOUNT =====");
        System.out.println("Authentication type: " + authentication.getClass().getName());
        System.out.println("Principal: " + authentication.getPrincipal());

        String userEmail = null;

        // Check if this is OAuth2 authentication (after Twitter linking)
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
            OAuth2User oauth2User = oauthToken.getPrincipal();
            String twitterId = (String) oauth2User.getAttributes().get("id");

            System.out.println("OAuth2 authentication detected - Twitter ID: " + twitterId);

            // Try to find user by Twitter ID
            Optional<Users> userByTwitter = userRepository.findByTwitterId(twitterId);
            if (userByTwitter.isPresent()) {
                userEmail = userByTwitter.get().getEmail();
                System.out.println("Found user by Twitter ID: " + userEmail);
            } else {
                // Try to extract email from JWT cookie as fallback
                userEmail = extractEmailFromJwtCookie(request);
                System.out.println("User not found by Twitter ID, tried JWT: " + userEmail);
            }
        } else {
            // Regular JWT authentication
            userEmail = extractEmailFromJwtCookie(request);
            System.out.println("JWT authentication - Email: " + userEmail);
        }

        if (userEmail == null || userEmail.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Not authenticated or no user found"
            ));
        }

        Optional<TwitterAccountResponse> twitter = twitterService.getLinkedTwitterAccount(userEmail);
        return twitter.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/unlink")
    public ResponseEntity<?> unlinkTwitter(Authentication authentication, HttpServletRequest request) {
        System.out.println("===== UNLINK TWITTER =====");

        String userEmail = extractEmailFromJwtCookie(request);

        if (userEmail == null || userEmail.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                    "error", "Not authenticated"
            ));
        }

        twitterService.unlinkTwitterAccount(userEmail);
        return ResponseEntity.ok(Map.of("message", "Twitter account unlinked successfully"));
    }

    private String extractEmailFromJwtCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            System.out.println("No cookies in request");
            return null;
        }

        for (Cookie cookie : cookies) {
            // ✅ Check for BOTH possible cookie names
            if ("authToken".equals(cookie.getName()) || "access_token".equals(cookie.getName())) {
                try {
                    String token = cookie.getValue();
                    String email = jwtService.extractEmail(token);
                    System.out.println("Extracted email from JWT cookie '" + cookie.getName() + "': " + email);
                    return email;
                } catch (Exception e) {
                    System.err.println("Failed to extract email from JWT: " + e.getMessage());
                    return null;
                }
            }
        }

        System.out.println("JWT cookie not found");
        return null;
    }
}