package com.fpsweeper.harvest.social;

import com.fpsweeper.harvest.auth.exceptions.TwitterAlreadyLinkedException;
import com.fpsweeper.harvest.auth.exceptions.TwitterNotLinkedException;
import com.fpsweeper.harvest.auth.exceptions.UserNotFoundException;
import com.fpsweeper.harvest.social.dto.TwitterAccountResponse;
import com.fpsweeper.harvest.user.UserRepository;
import com.fpsweeper.harvest.user.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;

@Service
public class TwitterService {

    @Autowired
    private TwitterAccountRepository twitterRepository;

    @Autowired
    private UserRepository userRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public TwitterAccountResponse linkTwitterAccount(String email, OAuth2AuthorizedClient authorizedClient) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        // Check if user already has Twitter linked
        Optional<TwitterAccounts> existingAccount = twitterRepository.findByUserId(user.getId());
        if (existingAccount.isPresent()) {
            throw new TwitterAlreadyLinkedException("You already have a Twitter account linked.");
        }

        // Get Twitter user info from the OAuth client
        String accessToken = authorizedClient.getAccessToken().getTokenValue();

        try {
            String url = "https://api.twitter.com/2/users/me?user.fields=id,name,username,profile_image_url";

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
            Map<String, Object> responseMap = mapper.readValue(response.getBody(), Map.class);
            Map<String, Object> data = (Map<String, Object>) responseMap.get("data");

            String twitterId = String.valueOf(data.get("id"));
            String username = (String) data.get("username");
            String name = (String) data.get("name");
            String profileImage = (String) data.get("profile_image_url");

            System.out.println("Linking Twitter account:");
            System.out.println("  Twitter ID: " + twitterId);
            System.out.println("  Username: " + username);
            System.out.println("  User Email: " + email);

            // Check if Twitter account is already linked to another user
            if (twitterRepository.existsByTwitterId(twitterId)) {
                throw new TwitterAlreadyLinkedException("This Twitter account is already linked to another user");
            }

            // Update user's twitter_id in the users table
            user.setTwitterId(twitterId);
            user.setTwitterHandle(username); // Assuming you have this field
            userRepository.save(user);
            System.out.println("Updated user's twitter_id in users table");

            // Create new Twitter account link in twitter_accounts table
            TwitterAccounts twitterAccount = new TwitterAccounts(user.getId(), twitterId, username);
            twitterAccount.setDisplayName(name);
            twitterAccount.setProfileImageUrl(profileImage);
            twitterAccount.setAccessToken(accessToken);

            if (authorizedClient.getRefreshToken() != null) {
                twitterAccount.setRefreshToken(authorizedClient.getRefreshToken().getTokenValue());
            }

            if (authorizedClient.getAccessToken().getExpiresAt() != null) {
                twitterAccount.setTokenExpiresAt(authorizedClient.getAccessToken().getExpiresAt());
            }

            twitterRepository.save(twitterAccount);
            System.out.println("Saved TwitterAccount to twitter_accounts table");

            return new TwitterAccountResponse(twitterAccount);

        } catch (TwitterAlreadyLinkedException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Failed to link Twitter account: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to link Twitter account: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void unlinkTwitterAccount(String email) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        TwitterAccounts account = twitterRepository.findByUserId(user.getId())
                .orElseThrow(TwitterNotLinkedException::new);

        System.out.println("Unlinking Twitter account for user: " + email);

        // Clear twitter_id and twitter_handle from users table
        user.setTwitterId(null);
        user.setTwitterHandle(null);
        userRepository.save(user);

        // Delete from twitter_accounts table
        twitterRepository.delete(account);

        System.out.println("Successfully unlinked Twitter account");
    }

    public Optional<TwitterAccountResponse> getLinkedTwitterAccount(String email) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        return twitterRepository.findByUserId(user.getId())
                .map(TwitterAccountResponse::new);
    }

    public boolean hasLinkedTwitter(String email) {
        Users user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        return twitterRepository.existsByUserId(user.getId());
    }
}