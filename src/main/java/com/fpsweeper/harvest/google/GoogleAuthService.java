package com.fpsweeper.harvest.google;

import com.fpsweeper.harvest.auth.dto.AuthResponse;
import com.fpsweeper.harvest.security.JwtService;
import com.fpsweeper.harvest.user.UserRepository;
import com.fpsweeper.harvest.user.Users;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GoogleAuthService {

    @Autowired
    private GoogleTokenVerifierService tokenVerifier;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtTokenProvider;

    public AuthResponse authenticateWithGoogle(String idToken) {
        // 1. Verify Google token
        Payload payload = tokenVerifier.verifyToken(idToken);
        if (payload == null) {
            throw new RuntimeException("Invalid Google token");
        }

        // 2. Extract user info from Google
        String email = payload.getEmail();
        String googleId = payload.getSubject();
        String firstName = (String) payload.get("given_name");
        String lastName = (String) payload.get("family_name");
        boolean emailVerified = payload.getEmailVerified();

        // 3. Find or create user
        Users user = userRepository.findByEmail(email)
                .map(existingUser -> {
                    // User exists - update if needed
                    if (existingUser.getAuthProvider().equals("LOCAL")) {
                        existingUser.setProviderId(googleId);
                        existingUser.setAuthProvider("GOOGLE");
                    }
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    // New user - create account
                    Users newUser = new Users();
                    newUser.setEmail(email);
                    newUser.setProviderId(googleId);
                    /*newUser.setFirstName(firstName);
                    newUser.setLastName(lastName);*/
                    newUser.setAuthProvider("GOOGLE");
                    newUser.setEmailVerified(true); // Google verified it
                    return userRepository.save(newUser);
                });

        // 4. Generate your JWT token
        String jwtToken = jwtTokenProvider.generateToken(user);

        return new AuthResponse(jwtToken, "Bearer", user);
    }
}