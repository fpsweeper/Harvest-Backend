package com.fpsweeper.harvest.auth;

import com.fpsweeper.harvest.auth.dto.*;
import com.fpsweeper.harvest.auth.exceptions.EmailNotVerifiedException;
import com.fpsweeper.harvest.auth.exceptions.GoogleSigninException;
import com.fpsweeper.harvest.auth.exceptions.InvalidCredentialsException;
import com.fpsweeper.harvest.google.GoogleAuthService;
import com.fpsweeper.harvest.passwordreset.PasswordResetService;
import com.fpsweeper.harvest.security.JwtService;
import com.fpsweeper.harvest.user.UserRepository;
import com.fpsweeper.harvest.user.Users;
import com.fpsweeper.harvest.verification.EmailVerificationCodes;
import com.fpsweeper.harvest.verification.EmailVerificationRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final UserRepository userRepo;
    private final EmailVerificationRepository verificationRepo;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    private GoogleAuthService googleAuthService;

    @Autowired
    private PasswordResetService passwordResetService;

    public AuthController(
            AuthService authService,
            JwtService jwtService,
            UserRepository userRepo,
            EmailVerificationRepository verificationRepo,
            PasswordEncoder passwordEncoder
    ) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.userRepo = userRepo;
        this.verificationRepo = verificationRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // ✅ REGISTER
    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody RegisterRequest req) {
        authService.register(req.getEmail(), req.getPassword());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody VerifyCodeRequest req) {
        authService.verifyEmail(req.getEmail(), req.getCode());
        return ResponseEntity.ok("Email verified successfully");
    }

    // ✅ CURRENT USER (JWT REQUIRED)
    @GetMapping("/me")
    public ResponseEntity<UserMeDto> me(@AuthenticationPrincipal Object principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        Users user = null;

        // Handle JWT authentication
        if (principal instanceof Users) {
            user = (Users) principal;
        }
        // Handle OAuth2 authentication (after Twitter login)
        else {
            if (principal instanceof OAuth2User) {
                OAuth2User oauth2User = (OAuth2User) principal;
                String twitterId = (String) oauth2User.getAttributes().get("id");

                // Find user by Twitter ID
                user = userRepo.findByTwitterId(twitterId).orElse(null);
            }
        }

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        return ResponseEntity.ok(new UserMeDto(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getAuthProvider()
        ));
    }


    @PostMapping("/login")
    public ResponseEntity<String> login(
            @RequestBody LoginRequest req,
            HttpServletResponse response) {

        Users user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getAuthProvider().equals("GOOGLE")) {
            throw new GoogleSigninException();
        }

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user);

        // Set cookie directly from Spring Boot
        Cookie cookie = new Cookie("access_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        response.addCookie(cookie);

        // Still return token for Next.js middleware
        return ResponseEntity.ok(token);
    }
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleAuth(@RequestBody GoogleAuthRequest request) {
        AuthResponse response = googleAuthService.authenticateWithGoogle(request.getIdToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @AuthenticationPrincipal Users user,
            @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(user, request);
        return ResponseEntity.ok("Password changed successfully");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        passwordResetService.sendResetCode(request.getEmail());
        return ResponseEntity.ok("Password reset code sent to your email");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getCode(), request.getNewPassword());
        return ResponseEntity.ok("Password reset successfully");
    }

}
