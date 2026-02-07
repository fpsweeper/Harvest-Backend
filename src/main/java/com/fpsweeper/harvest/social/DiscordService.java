package com.fpsweeper.harvest.social;

import com.fpsweeper.harvest.auth.exceptions.UserNotFoundException;
import com.fpsweeper.harvest.social.dto.DiscordAccountResponse;
import com.fpsweeper.harvest.user.UserRepository;
import com.fpsweeper.harvest.user.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;

@Service
public class DiscordService {

    @Autowired
    private DiscordAccountRepository discordRepository;

    @Autowired
    private UserRepository userRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public DiscordAccountResponse linkDiscordAccount(String email, OAuth2AuthorizedClient authorizedClient) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        // Check if user already has Discord linked
        Optional<DiscordAccounts> existingAccount = discordRepository.findByUserId(user.getId());
        if (existingAccount.isPresent()) {
            throw new RuntimeException("You already have a Discord account linked");
        }

        // Get Discord user info
        String accessToken = authorizedClient.getAccessToken().getTokenValue();

        try {
            String url = "https://discord.com/api/users/@me";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> data = mapper.readValue(response.getBody(), Map.class);

            String discordId = String.valueOf(data.get("id"));
            String username = (String) data.get("username");
            String discriminator = (String) data.get("discriminator");
            String globalName = (String) data.get("global_name");
            String avatar = (String) data.get("avatar");

            System.out.println("Linking Discord account:");
            System.out.println("  Discord ID: " + discordId);
            System.out.println("  Username: " + username);
            System.out.println("  User Email: " + email);

            // Check if Discord account is already linked to another user
            if (discordRepository.existsByDiscordId(discordId)) {
                throw new RuntimeException("This Discord account is already linked to another user");
            }

            // Update user's discord_id
            user.setDiscordId(discordId);
            userRepository.save(user);

            // Create Discord account record
            DiscordAccounts discordAccount = new DiscordAccounts(user.getId(), discordId, username);
            discordAccount.setDiscriminator(discriminator);
            discordAccount.setDisplayName(globalName != null ? globalName : username);

            // Build avatar URL
            if (avatar != null) {
                String avatarUrl = String.format(
                        "https://cdn.discordapp.com/avatars/%s/%s.png",
                        discordId, avatar
                );
                discordAccount.setAvatarUrl(avatarUrl);
            }

            discordAccount.setAccessToken(accessToken);

            if (authorizedClient.getRefreshToken() != null) {
                discordAccount.setRefreshToken(authorizedClient.getRefreshToken().getTokenValue());
            }

            if (authorizedClient.getAccessToken().getExpiresAt() != null) {
                discordAccount.setTokenExpiresAt(authorizedClient.getAccessToken().getExpiresAt());
            }

            discordRepository.save(discordAccount);
            System.out.println("Saved Discord account to database");

            return new DiscordAccountResponse(discordAccount);

        } catch (Exception e) {
            System.err.println("Failed to link Discord account: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to link Discord account: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void unlinkDiscordAccount(String email) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        DiscordAccounts account = discordRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("No Discord account linked"));

        System.out.println("Unlinking Discord account for user: " + email);

        // Clear discord_id from users table
        user.setDiscordId(null);
        userRepository.save(user);

        // Delete from discord_accounts table
        discordRepository.delete(account);

        System.out.println("Successfully unlinked Discord account");
    }

    public Optional<DiscordAccountResponse> getLinkedDiscordAccount(String email) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        return discordRepository.findByUserId(user.getId())
                .map(DiscordAccountResponse::new);
    }
}