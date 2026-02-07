package com.fpsweeper.harvest.social;

import com.fpsweeper.harvest.auth.exceptions.UserNotFoundException;
import com.fpsweeper.harvest.security.JwtService;
import com.fpsweeper.harvest.social.dto.TwitterAccountResponse;
import com.fpsweeper.harvest.user.UserRepository;
import com.fpsweeper.harvest.user.Users;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/social/twitter")
public class TwitterController {

    @Autowired
    private TwitterService twitterService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OAuthLinkingTokenRepository linkingTokenRepository;

    @GetMapping
    public ResponseEntity<?> getLinkedTwitter(@AuthenticationPrincipal Users user) {
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        return twitterService
                .getLinkedTwitterAccount(user.getEmail())
                .map(ResponseEntity::ok)
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

    @PostMapping("/prepare")
    public ResponseEntity<?> prepareTwitterLink(@RequestHeader("Authorization") String bearer) {
        String email = jwtService.extractEmail(bearer.substring(7));

        OAuthLinkingToken token = new OAuthLinkingToken();
        token.setUserEmail(email);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiresAt(Instant.now().plusSeconds(600)); // 10min
        linkingTokenRepository.save(token);
        System.out.println("Sending the linking token : " + token.getToken() + " With email " + email);
        Map<String, String> res = Map.of("token", token.getToken());
        return ResponseEntity.ok(res);
    }

    @PostMapping("/complete-link")
    public ResponseEntity<?> completeTwitterLink(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Object principal) {

        String linkToken = body.get("linkToken");

        if (linkToken == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing token"));
        }

        // Verify token exists and is valid
        Optional<OAuthLinkingToken> tokenOpt = linkingTokenRepository.findByToken(linkToken);

        if (tokenOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Token not found"));
        }

        OAuthLinkingToken token = tokenOpt.get();

        if (token.getExpiresAt().isBefore(Instant.now())) {
            linkingTokenRepository.delete(token);
            return ResponseEntity.status(410).body(Map.of("error", "Token expired"));
        }

        // Get user from principal (should be OAuth2User after Twitter login)
        if (!(principal instanceof OAuth2User)) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated with Twitter"));
        }

        OAuth2User oauth2User = (OAuth2User) principal;
        String twitterId = (String) oauth2User.getAttributes().get("id");

        // Verify user owns this token
        Users user = userRepository.findByEmail(token.getUserEmail())
                .orElseThrow(() -> new UserNotFoundException());

        // Update user with Twitter ID
        user.setTwitterId(twitterId);
        user.setTwitterHandle((String) oauth2User.getAttributes().get("username"));
        userRepository.save(user);

        // Delete token
        linkingTokenRepository.delete(token);

        return ResponseEntity.ok(Map.of("success", true));
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