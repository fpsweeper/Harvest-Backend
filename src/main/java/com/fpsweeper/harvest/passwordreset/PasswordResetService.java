package com.fpsweeper.harvest.passwordreset;

import com.fpsweeper.harvest.auth.exceptions.UserNotFoundException;
import com.fpsweeper.harvest.auth.exceptions.GoogleSigninException;
import com.fpsweeper.harvest.auth.exceptions.PasswordResetCodeNotFoundException;
import com.fpsweeper.harvest.email.MailService;
import com.fpsweeper.harvest.user.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fpsweeper.harvest.user.UserRepository;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

@Service
public class PasswordResetService {

    @Autowired
    private PasswordResetRepository resetCodeRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private MailService mailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final int CODE_LENGTH = 6;
    private static final int EXPIRATION_HOURS = 1;
    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    @Transactional
    public void sendResetCode(String email) {

        Users user = userRepo.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        // ✅ Check if user signed up with Google
        if ("GOOGLE".equals(user.getAuthProvider())) {
            throw new GoogleSigninException();
        }

        // Delete any existing unused codes for this user
        resetCodeRepo.deleteByUserId(user.getId());

        // Generate 6-digit code
        String code = generateCode();

        // Create reset code entry
        PasswordResetCodes resetCode = new PasswordResetCodes();
        resetCode.setUserId(user.getId());
        resetCode.setCode(code);
        resetCode.setExpiresAt(Instant.now().plusSeconds(EXPIRATION_HOURS * 3600));
        resetCode.setUsed(false);
        resetCode.setCreatedAt(Instant.now());

        resetCodeRepo.save(resetCode);

        // Send email
        String subject = "Password Reset Code - Harvest 3";
        String htmlContent = buildResetEmail(user.getEmail(), code);

        mailService.sendEmail(email, subject, htmlContent);
    }

    @Transactional
    public void resetPassword(String code, String newPassword) {
        // Find valid reset code
        PasswordResetCodes resetCode = resetCodeRepo
                .findByCodeAndUsedFalseAndExpiresAtAfter(code, Instant.now())
                .orElseThrow(PasswordResetCodeNotFoundException::new);

        // Find user
        Users user = userRepo.findById(resetCode.getUserId())
                .orElseThrow(UserNotFoundException::new);

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        // Mark code as used
        resetCode.setUsed(true);
        resetCodeRepo.save(resetCode);
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder(CODE_LENGTH);

        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }

        return code.toString();
    }

    private String buildResetEmail(String firstName, String code) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); 
                             color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }
                    .code-box { background: white; border: 2px dashed #667eea; padding: 20px; 
                               text-align: center; margin: 20px 0; border-radius: 8px; }
                    .code { font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #667eea; }
                    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
                    .warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 12px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>Password Reset Request</h1>
                    </div>
                    <div class="content">
                        <p>Hi %s,</p>
                        <p>We received a request to reset your Harvest 3 account password. Use the code below to reset your password:</p>
                        
                        <div class="code-box">
                            <div class="code">%s</div>
                        </div>
                        
                        <p style="text-align: center; color: #666;">This code will expire in 1 hour.</p>
                        
                        <div class="warning">
                            <strong>⚠️ Security Notice:</strong> If you didn't request this password reset, please ignore this email. 
                            Your password will remain unchanged.
                        </div>
                        
                        <p>For security reasons, never share this code with anyone.</p>
                        
                        <p>Best regards,<br>The Harvest 3 Team</p>
                    </div>
                    <div class="footer">
                        <p>This is an automated message, please do not reply to this email.</p>
                        <p>&copy; 2026 Harvest 3. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(firstName, code);
    }
}