package com.academic.platform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * EmailService — all send methods are @Async.
 *
 * Scalability: Email sending is a slow I/O operation (100-3000ms SMTP round trip).
 * Making every send method async means the HTTP response returns immediately
 * while the email is dispatched in the background 'emailExecutor' thread pool.
 * This prevents a slow/unavailable mail server from degrading the entire platform.
 */
@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SystemSettingService systemSettingService;

    @Async("emailExecutor")
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        if ("false".equalsIgnoreCase(systemSettingService.getSetting("emailNotifications"))) {
            System.out.println("Email notifications are disabled. Skipping email to: " + to);
            return;
        }

        try {
            jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(
                    message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true = isHtml

            mailSender.send(message);
            System.out.println("Email sent successfully to: " + to);
        } catch (Exception e) {
            System.err.println("Failed to send HTML email to " + to + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    @Async("emailExecutor")
    public void sendMeetingNotification(String to, String mentorName, String title, String time, String location) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("New Mentorship Meeting Scheduled: " + title);
        message.setText("Dear Student,\n\n" +
                "A new mentorship meeting has been scheduled by " + mentorName + ".\n\n" +
                "Title: " + title + "\n" +
                "Time: " + time + "\n" +
                "Location: " + location + "\n\n" +
                "Please be on time.\n\n" +
                "Best Regards,\n" +
                "Academic Platform Team");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
        }
    }

    @Async("emailExecutor")
    public void sendBulkMeetingNotification(String[] bcc, String mentorName, String title, String time,
            String location) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setBcc(bcc); // Use BCC for privacy
        message.setSubject("Group Mentorship Meeting: " + title);
        message.setText("Dear Students,\n\n" +
                "You are invited to a group mentorship meeting by " + mentorName + ".\n\n" +
                "Title: " + title + "\n" +
                "Time: " + time + "\n" +
                "Location: " + location + "\n\n" +
                "Please make sure to attend.\n\n" +
                "Best Regards,\n" +
                "Academic Platform Team");

        try {
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send bulk email: " + e.getMessage());
        }
    }

    // --- Leave Workflow Emails ---

    @Async("emailExecutor")
    public void sendParentApprovalRequest(String parentEmail, String studentName, String leaveReason, String from,
            String to, String approvalLink, String otp) {
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
                + "<body style='font-family: sans-serif; background-color: #121212; margin: 0; padding: 0;'>"
                + "  <div style='max-width: 400px; margin: 20px auto; background-color: #1a1a1a; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.3); color: #ffffff;'>"
                + "    <div style='background-color: #1e3a8a; padding: 30px 20px; text-align: center;'>"
                + "      <h1 style='margin: 0; font-size: 20px; font-weight: 700; color: #ffffff; text-transform: uppercase; letter-spacing: 1px;'>ACADEMIC PORTAL</h1>"
                + "      <p style='margin: 5px 0 0; font-size: 12px; color: #bfdbfe; font-weight: 500;'>Leave Authorization</p>"
                + "    </div>"
                + "    <div style='padding: 30px 20px; text-align: center;'>"
                + "      <h2 style='font-size: 16px; margin: 0 0 10px; font-weight: 600; color: #ffffff;'>Verify Leave Request</h2>"
                + "      <p style='font-size: 14px; color: #9ca3af; margin: 0 0 20px;'>Your child <strong>"
                + studentName + "</strong> has requested leave.</p>"
                + "      <div style='background-color: #262626; border-radius: 8px; padding: 15px; text-align: left; margin: 15px 0; font-size: 13px; color: #d1d5db;'>"
                + "        <p style='margin: 5px 0;'><strong>Date:</strong> " + from + " to " + to + "</p>"
                + "        <p style='margin: 5px 0;'><strong>Reason:</strong> " + leaveReason + "</p>"
                + "      </div>"
                + "      <p style='font-size: 12px; margin-top: 20px; color: #9ca3af;'>To authorize this request, please share the code below with the mentor:</p>"
                + "      <div style='background-color: #0f1c13; border: 1px solid #14532d; border-radius: 8px; padding: 15px; margin: 20px 0; display: inline-block; width: 80%;'>"
                + "        <p style='color: #22c55e; font-size: 32px; font-weight: 700; letter-spacing: 5px; margin: 0; font-family: monospace;'>"
                + otp + "</p>"
                + "      </div>"
                + "      <p style='color: #ffffff; margin-bottom: 5px; font-weight: 600; font-size: 12px;'>Valid for 7 days.</p>"
                + "      <p style='font-size: 12px; color: #6b7280; margin-top: 20px;'>Do not share this OTP with anyone other than the mentor.</p>"
                + "    </div>"
                + "    <div style='background-color: #171717; padding: 15px; text-align: center; font-size: 10px; color: #525252; border-top: 1px solid #262626;'>"
                + "      &copy; 2026 Academic Platform System. All rights reserved."
                + "    </div>"
                + "  </div>"
                + "</body></html>";

        sendHtmlEmail(parentEmail, "Leave Authorization Code for " + studentName, html);
    }

    @Async("emailExecutor")
    public void sendStudentLeaveStatus(String studentEmail, String status, String comments) {
        String color = "APPROVED".equals(status) ? "#10b981" : "#ef4444";
        String html = "<html><body>"
                + "<h2>Leave Request Update</h2>"
                + "<p>Dear Student,</p>"
                + "<p>Your leave request has been <strong style='color:" + color + "'>" + status + "</strong>.</p>"
                + (comments != null ? "<p><strong>Comments:</strong> " + comments + "</p>" : "")
                + "<p>Regards,<br>Academic Team</p>"
                + "</body></html>";

        sendHtmlEmail(studentEmail, "Leave Request " + status, html);
    }

    @Async("emailExecutor")
    public void sendActionOtp(String to, String otp, String actionDescription) {
        String html = "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "<meta charset='UTF-8'>"
                + "<style>"
                + "  body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #121212; margin: 0; padding: 0; }"
                + "  .container { max-width: 400px; margin: 20px auto; background-color: #1a1a1a; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.3); color: #ffffff; }"
                + "  .header { background-color: #1e3a8a; padding: 30px 20px; text-align: center; }"
                + "  .header h1 { margin: 0; font-size: 20px; font-weight: 700; color: #ffffff; text-transform: uppercase; letter-spacing: 1px; }"
                + "  .header p { margin: 5px 0 0; font-size: 12px; color: #bfdbfe; font-weight: 500; }"
                + "  .content { padding: 30px 20px; text-align: center; }"
                + "  .content h2 { font-size: 16px; margin: 0 0 10px; font-weight: 600; color: #ffffff; }"
                + "  .content p { font-size: 14px; color: #9ca3af; margin: 0 0 20px; }"
                + "  .otp-box { background-color: #0f1c13; border: 1px solid #14532d; border-radius: 8px; padding: 15px; margin: 20px 0; display: inline-block; width: 80%; }"
                + "  .otp-code { color: #22c55e; font-size: 32px; font-weight: 700; letter-spacing: 5px; margin: 0; font-family: monospace; }"
                + "  .warning { font-size: 12px; color: #6b7280; margin-top: 20px; }"
                + "  .footer { background-color: #171717; padding: 15px; text-align: center; font-size: 10px; color: #525252; border-top: 1px solid #262626; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "  <div class='container'>"
                + "    <div class='header'>"
                + "      <h1>ACADEMIC PORTAL</h1>"
                + "      <p>Secure Action Verification</p>"
                + "    </div>"
                + "    <div class='content'>"
                + "      <h2>OTP Verification</h2>"
                + "      <p>You are attempting to: <strong>" + actionDescription
                + "</strong>. Use the One Time Password below to verify this action.</p>"
                + "      <div class='otp-box'>"
                + "        <p class='otp-code'>" + otp + "</p>"
                + "      </div>"
                + "      <p style='color: #ffffff; margin-bottom: 5px; font-weight: 600;'>Valid for 5 minutes.</p>"
                + "      <p class='warning'>Do not share this OTP with anyone. If you didn't request this, please ignore this email.</p>"
                + "    </div>"
                + "    <div class='footer'>"
                + "      &copy; 2026 Academic Platform System. All rights reserved."
                + "    </div>"
                + "  </div>"
                + "</body>"
                + "</html>";

        sendHtmlEmail(to, "Verification OTP: " + otp, html);
    }

    @Async("emailExecutor")
    public void sendParentOtpCode(String parentEmail, String studentName, String otp) {
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
                + "<body style='font-family: sans-serif; background-color: #121212; margin: 0; padding: 0;'>"
                + "  <div style='max-width: 400px; margin: 20px auto; background-color: #1a1a1a; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.3); color: #ffffff;'>"
                + "    <div style='background-color: #1e3a8a; padding: 30px 20px; text-align: center;'>"
                + "      <h1 style='margin: 0; font-size: 20px; font-weight: 700; color: #ffffff; text-transform: uppercase; letter-spacing: 1px;'>ACADEMIC PORTAL</h1>"
                + "      <p style='margin: 5px 0 0; font-size: 12px; color: #bfdbfe; font-weight: 500;'>Security Verification</p>"
                + "    </div>"
                + "    <div style='padding: 30px 20px; text-align: center;'>"
                + "      <h2 style='font-size: 16px; margin: 0 0 10px; font-weight: 600; color: #ffffff;'>Leave Approval OTP</h2>"
                + "      <p style='font-size: 14px; color: #9ca3af; margin: 0 0 20px;'>You have approved the leave for <strong>"
                + studentName + "</strong>.</p>"
                + "      <p style='font-size: 12px; color: #9ca3af;'>Provide this code to the mentor to finalize the process:</p>"
                + "      <div style='background-color: #0f1c13; border: 1px solid #14532d; border-radius: 8px; padding: 15px; margin: 20px 0; display: inline-block; width: 80%;'>"
                + "        <p style='color: #22c55e; font-size: 32px; font-weight: 700; letter-spacing: 5px; margin: 0; font-family: monospace;'>"
                + otp + "</p>"
                + "      </div>"
                + "      <p style='color: #ffffff; margin-bottom: 5px; font-weight: 600; font-size: 12px;'>Valid for 7 days.</p>"
                + "    </div>"
                + "    <div style='background-color: #171717; padding: 15px; text-align: center; font-size: 10px; color: #525252; border-top: 1px solid #262626;'>"
                + "      &copy; 2026 Academic Platform System. All rights reserved."
                + "    </div>"
                + "  </div>"
                + "</body></html>";

        sendHtmlEmail(parentEmail, "Action Required: OTP for Leave Approval", html);
    }
}
