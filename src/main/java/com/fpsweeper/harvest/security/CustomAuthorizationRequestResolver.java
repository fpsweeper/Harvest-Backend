package com.fpsweeper.harvest.security;

import com.fpsweeper.harvest.social.OAuthLinkingToken;
import com.fpsweeper.harvest.social.OAuthLinkingTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CustomAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final OAuth2AuthorizationRequestResolver defaultResolver;
    private final OAuthLinkingTokenRepository linkingTokenRepository;

    public CustomAuthorizationRequestResolver(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuthLinkingTokenRepository linkingTokenRepository) {
        this.defaultResolver = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository,
                "/oauth2/authorization"
        );
        this.linkingTokenRepository = linkingTokenRepository;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request);
        return customizeAuthorizationRequest(authorizationRequest, request);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest =
                defaultResolver.resolve(request, clientRegistrationId);
        return customizeAuthorizationRequest(authorizationRequest, request);
    }

    private OAuth2AuthorizationRequest customizeAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest,
            HttpServletRequest request) {

        if (authorizationRequest == null) {
            return null;
        }

        // Check for link_token parameter
        String linkToken = request.getParameter("link_token");

        if (linkToken != null) {
            System.out.println("CustomAuthorizationRequestResolver: Found link_token=" + linkToken);

            // Verify token exists
            Optional<OAuthLinkingToken> tokenOpt = linkingTokenRepository.findByToken(linkToken);

            if (tokenOpt.isPresent()) {
                System.out.println("CustomAuthorizationRequestResolver: Token verified, embedding in state");

                // Append link_token to the OAuth state parameter
                String originalState = authorizationRequest.getState();
                String newState = originalState + ":" + linkToken;

                return OAuth2AuthorizationRequest
                        .from(authorizationRequest)
                        .state(newState)  // ✅ Embed token in state
                        .build();
            } else {
                System.out.println("CustomAuthorizationRequestResolver: Token not found in database");
            }
        }

        return authorizationRequest;
    }
}