package com.fpsweeper.harvest.auth;

import com.fpsweeper.harvest.auth.dto.ChangePasswordRequest;
import com.fpsweeper.harvest.auth.exceptions.*;
import com.fpsweeper.harvest.email.MailService;
import com.fpsweeper.harvest.security.JwtService;
import com.fpsweeper.harvest.user.UserRepository;
import com.fpsweeper.harvest.user.Users;
import com.fpsweeper.harvest.verification.EmailVerificationCodes;
import com.fpsweeper.harvest.verification.EmailVerificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Random;

@Service
public class AuthService {

    private final UserRepository userRepo;
    private final EmailVerificationRepository verificationRepo;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Autowired
    JwtService jwtService;

    public AuthService(UserRepository userRepo,
                       EmailVerificationRepository verificationRepo,
                       PasswordEncoder passwordEncoder,
                       MailService mailService) {
        this.userRepo = userRepo;
        this.verificationRepo = verificationRepo;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
    }

    public void register(String email, String password) {

        Users userr = userRepo.findByEmail(email).orElse(null);

        if (userr != null) { // in case registered but didnt used the code for a while
            if (userr.isEmailVerified()) {
                throw new EmailAlreadyRegisteredException();
            }

            SecureRandom random = new SecureRandom();
            String code = String.valueOf(100000 + random.nextInt(900000));

            EmailVerificationCodes v = new EmailVerificationCodes();
            v.setUserId(userr.getId());
            v.setCode(code);
            v.setExpiresAt(Instant.now().plusSeconds(900));
            v.setCreatedAt(Instant.now());

            verificationRepo.save(v);

            // 🔥 email comes from User table
            mailService.sendVerificationCode(userr.getEmail(), code);
            return;
        }

        Users user = new Users();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        userRepo.save(user);

        SecureRandom random = new SecureRandom();
        String code = String.valueOf(100000 + random.nextInt(900000));

        EmailVerificationCodes v = new EmailVerificationCodes();
        v.setUserId(user.getId());
        v.setCode(code);
        v.setExpiresAt(Instant.now().plusSeconds(900));
        v.setCreatedAt(Instant.now());

        verificationRepo.save(v);

        // 🔥 email comes from User table
        mailService.sendVerificationCode(user.getEmail(), code);

        // TODO: send email
        System.out.println("Verification code: " + code);
    }

    public void verifyEmail(String email, String code) {

        Users user = userRepo.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException());

        EmailVerificationCodes token =
                verificationRepo
                        .findByUserIdAndCodeAndUsedFalse(user.getId(), code)
                        .orElseThrow(() -> new InvalidCodeException());

        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new CodeExpiredException();
        }

        token.setUsed(true);
        verificationRepo.save(token);

        user.setEmailVerified(true);
        userRepo.save(user);
    }

    public void changePassword(Users userr, ChangePasswordRequest request) {
        // Extract email from token
        // Find user
        Users user = userRepo.findByEmail(userr.getEmail())
                .orElseThrow(UserNotFoundException::new);

        // Check if user uses Google Sign-In
        if ("GOOGLE".equals(user.getAuthProvider())) {
            throw new GoogleSigninException();
        }

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IncorrectPasswordException();
        }

        // Check if new password is same as current
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new RuntimeException("New password must be different from current password");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepo.save(user);
    }
}

