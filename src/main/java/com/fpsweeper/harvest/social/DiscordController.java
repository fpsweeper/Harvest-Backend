package com.fpsweeper.harvest.social;

import com.fpsweeper.harvest.social.dto.DiscordAccountResponse;
import com.fpsweeper.harvest.user.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/social/discord")
public class DiscordController {

    @Autowired
    private DiscordService discordService;

    @Autowired
    private OAuthLinkingTokenRepository linkingTokenRepository;

    @GetMapping
    public ResponseEntity<?> getDiscordAccount(@AuthenticationPrincipal Users user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        return discordService.getLinkedDiscordAccount(user.getEmail())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/prepare")
    public ResponseEntity<?> prepareDiscordLink(@AuthenticationPrincipal Users user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        // Create linking token
        OAuthLinkingToken linkingToken = new OAuthLinkingToken(user.getEmail());
        linkingTokenRepository.save(linkingToken);

        System.out.println("Created Discord linking token: " + linkingToken.getToken() + " for user: " + user.getEmail());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "token", linkingToken.getToken()
        ));
    }

    @DeleteMapping("/unlink")
    public ResponseEntity<?> unlinkDiscord(@AuthenticationPrincipal Users user) {
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        try {
            discordService.unlinkDiscordAccount(user.getEmail());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}