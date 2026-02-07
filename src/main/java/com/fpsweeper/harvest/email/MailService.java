package com.fpsweeper.harvest.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final JavaMailSender mailSender;

    public MailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.name}")
    private String fromName;

    /**
     * Send an HTML email
     * @param to Recipient email address
     * @param subject Email subject
     * @param htmlContent HTML content of the email
     */
    @Async
    public void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML content

            mailSender.send(message);

            System.out.println("Email sent successfully to: " + to);

        } catch (MessagingException e) {
            System.err.println("Failed to send email to: " + to);
            e.printStackTrace();
            throw new RuntimeException("Failed to send email", e);
        } catch (Exception e) {
            System.err.println("Unexpected error sending email to: " + to);
            e.printStackTrace();
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /*public void sendVerificationCode(String to, String code) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Verify your email");
        message.setText(
                "Your verification code is: " + code + "\n\n" +
                        "This code expires in 15 minutes."
        );

        mailSender.send(message);
    }*/

    @Async
    public void sendVerificationCode(String to, String code) {
        String subject = "Verify Your Email - Harvest 3";
        String htmlContent = buildVerificationEmail(code);

        sendEmail(to, subject, htmlContent);
    }

    private String buildVerificationEmail(String code) {
        return """
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
                        margin: 0;
                        padding: 0;
                        background-color: #f4f4f4;
                    }
                    .container {
                        max-width: 600px;
                        margin: 40px auto;
                        background-color: #ffffff;
                        border-radius: 12px;
                        overflow: hidden;
                        box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                    }
                    .header {
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        color: white;
                        padding: 40px 30px;
                        text-align: center;
                    }
                    .header h1 {
                        margin: 0;
                        font-size: 28px;
                        font-weight: 600;
                    }
                    .header p {
                        margin: 10px 0 0 0;
                        font-size: 16px;
                        opacity: 0.9;
                    }
                    .content {
                        padding: 40px 30px;
                        background-color: #ffffff;
                    }
                    .welcome-text {
                        font-size: 18px;
                        color: #333;
                        margin-bottom: 20px;
                    }
                    .code-box {
                        background: linear-gradient(135deg, #f5f7fa 0%%, #c3cfe2 100%%);
                        border: 3px dashed #667eea;
                        padding: 30px 20px;
                        text-align: center;
                        margin: 30px 0;
                        border-radius: 12px;
                        box-shadow: 0 2px 8px rgba(102, 126, 234, 0.1);
                    }
                    .code {
                        font-size: 42px;
                        font-weight: bold;
                        letter-spacing: 12px;
                        color: #667eea;
                        font-family: 'Courier New', monospace;
                        text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
                    }
                    .expiry-notice {
                        text-align: center;
                        color: #666;
                        font-size: 14px;
                        margin-top: 15px;
                        padding: 12px;
                        background-color: #fff3cd;
                        border-radius: 6px;
                        border-left: 4px solid #ffc107;
                    }
                    .info-box {
                        background-color: #e3f2fd;
                        border-left: 4px solid #2196f3;
                        padding: 16px;
                        margin: 25px 0;
                        border-radius: 6px;
                    }
                    .info-box p {
                        margin: 0;
                        color: #1976d2;
                        font-size: 14px;
                    }
                    .button {
                        display: inline-block;
                        padding: 14px 32px;
                        background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        color: white;
                        text-decoration: none;
                        border-radius: 8px;
                        font-weight: 600;
                        margin: 20px 0;
                        box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
                        transition: all 0.3s ease;
                    }
                    .footer {
                        background-color: #f8f9fa;
                        padding: 30px;
                        text-align: center;
                        border-top: 1px solid #e9ecef;
                    }
                    .footer p {
                        margin: 5px 0;
                        color: #6c757d;
                        font-size: 13px;
                    }
                    .footer a {
                        color: #667eea;
                        text-decoration: none;
                    }
                    .social-links {
                        margin-top: 20px;
                    }
                    .social-links a {
                        display: inline-block;
                        margin: 0 8px;
                        color: #6c757d;
                        font-size: 14px;
                    }
                    @media only screen and (max-width: 600px) {
                        .container {
                            margin: 20px;
                            border-radius: 8px;
                        }
                        .header {
                            padding: 30px 20px;
                        }
                        .header h1 {
                            font-size: 24px;
                        }
                        .content {
                            padding: 30px 20px;
                        }
                        .code {
                            font-size: 32px;
                            letter-spacing: 8px;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🎉 Welcome to Harvest 3!</h1>
                        <p>Let's verify your email address</p>
                    </div>
                    
                    <div class="content">
                        <p class="welcome-text">Hi there,</p>
                        
                        <p>Thank you for signing up! We're excited to have you on board. To complete your registration and start using Harvest 3, please verify your email address using the code below:</p>
                        
                        <div class="code-box">
                            <div class="code">%s</div>
                        </div>
                        
                        <div class="expiry-notice">
                            ⏱️ <strong>Important:</strong> This code will expire in 15 minutes
                        </div>
                        
                        <div class="info-box">
                            <p>💡 <strong>Tip:</strong> Copy and paste this code into the verification page to complete your registration.</p>
                        </div>
                        
                        <p style="margin-top: 30px; color: #666; font-size: 14px;">
                            If you didn't create an account with Harvest 3, you can safely ignore this email.
                        </p>
                        
                        <p style="margin-top: 25px; color: #333;">
                            Best regards,<br>
                            <strong>The Harvest 3 Team</strong>
                        </p>
                    </div>
                    
                    <div class="footer">
                        <p><strong>Harvest 3</strong> - Your Trading Automation Platform</p>
                        <p>This is an automated message, please do not reply to this email.</p>
                        <p style="margin-top: 15px; font-size: 12px;">
                            &copy; 2026 Harvest 3. All rights reserved.
                        </p>
                        <div class="social-links">
                            <a href="#">Help Center</a> •
                            <a href="#">Contact Support</a> •
                            <a href="#">Privacy Policy</a>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(code);
    }
}
