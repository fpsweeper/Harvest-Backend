package com.fpsweeper.harvest.email;

import com.resend.*;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class ResendEmailService {

    @Value("${resend.api.key}")
    private String apiKey;

    @Async
    public void sendVerificationEmail(String toEmail, String code) {
        System.out.println("📧 Sending verification email to: " + toEmail);

        Resend resend = new Resend(apiKey);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("support@harvest3.com")
                .to(toEmail)
                .subject("Verify your Harvest3 account")
                .html(buildVerificationHtml(code))
                .build();

        try {
            CreateEmailResponse response = resend.emails().send(params);
            System.out.println("✅ Verification email sent successfully. ID: " + response.getId());
        } catch (ResendException e) {
            System.err.println("❌ Failed to send verification email: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String code) {
        System.out.println("📧 Sending password reset email to: " + toEmail);

        Resend resend = new Resend(apiKey);

        String htmlContent = buildResetEmail(toEmail, code);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("support@harvest3.com")
                .to(toEmail)
                .subject("Reset your Harvest3 password")
                .html(htmlContent)
                .build();

        try {
            CreateEmailResponse response = resend.emails().send(params);
            System.out.println("✅ Password reset email sent successfully. ID: " + response.getId());
        } catch (ResendException e) {
            System.err.println("❌ Failed to send password reset email: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send password reset email", e);
        }
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

    private String buildVerificationHtml(String code) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        background-color: #f4f4f4;
                        margin: 0;
                        padding: 0;
                    }
                    .container {
                        max-width: 600px;
                        margin: 40px auto;
                        background: white;
                        border-radius: 8px;
                        overflow: hidden;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    .header {
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        color: white;
                        padding: 40px 20px;
                        text-align: center;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 28px;
                    }
                    .content {
                        padding: 40px 30px;
                    }
                    .code-box {
                        background: #f8f9fa;
                        border: 2px dashed #667eea;
                        border-radius: 8px;
                        padding: 20px;
                        text-align: center;
                        margin: 30px 0;
                    }
                    .code {
                        font-size: 36px;
                        font-weight: bold;
                        letter-spacing: 8px;
                        color: #667eea;
                        font-family: 'Courier New', monospace;
                    }
                    .footer {
                        background: #f8f9fa;
                        padding: 20px;
                        text-align: center;
                        color: #666;
                        font-size: 12px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🌱 Welcome to Harvest3!</h1>
                    </div>
                    <div class="content">
                        <p>Thank you for signing up! Please verify your email address to get started.</p>
                        <p>Your verification code is:</p>
                        <div class="code-box">
                            <div class="code">%s</div>
                        </div>
                        <p><strong>This code will expire in 10 minutes.</strong></p>
                        <p>If you didn't create an account with Harvest3, you can safely ignore this email.</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 Harvest3. All rights reserved.</p>
                        <p>This is an automated email. Please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """, code);
    }

    private String buildPasswordResetHtml(String resetUrl) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        background-color: #f4f4f4;
                        margin: 0;
                        padding: 0;
                    }
                    .container {
                        max-width: 600px;
                        margin: 40px auto;
                        background: white;
                        border-radius: 8px;
                        overflow: hidden;
                        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                    }
                    .header {
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        color: white;
                        padding: 40px 20px;
                        text-align: center;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 28px;
                    }
                    .content {
                        padding: 40px 30px;
                    }
                    .button {
                        display: inline-block;
                        padding: 15px 30px;
                        background: #667eea;
                        color: white;
                        text-decoration: none;
                        border-radius: 5px;
                        font-weight: bold;
                        margin: 20px 0;
                    }
                    .button-container {
                        text-align: center;
                    }
                    .footer {
                        background: #f8f9fa;
                        padding: 20px;
                        text-align: center;
                        color: #666;
                        font-size: 12px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔑 Reset Your Password</h1>
                    </div>
                    <div class="content">
                        <p>You requested to reset your password for your Harvest3 account.</p>
                        <p>Click the button below to reset your password:</p>
                        <div class="button-container">
                            <a href="%s" class="button">Reset Password</a>
                        </div>
                        <p><strong>This link will expire in 1 hour.</strong></p>
                        <p>If you didn't request this password reset, please ignore this email and your password will remain unchanged.</p>
                    </div>
                    <div class="footer">
                        <p>© 2026 Harvest3. All rights reserved.</p>
                        <p>This is an automated email. Please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """, resetUrl);
    }
}