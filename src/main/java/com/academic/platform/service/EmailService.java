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
                + "      <p style='font-size: 12px; margin-top: 20px; color: #9ca3af;'>To authorize this request, please click the button below or share the code below with the mentor:</p>"
                + "      <a href='" + approvalLink + "' style='display: inline-block; padding: 12px 24px; background-color: #2563eb; color: #ffffff; text-decoration: none; border-radius: 6px; font-weight: 600; font-size: 14px; margin-bottom: 15px;'>Review Leave Request</a><br/>"
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

    // ─── Faculty Leave Emails ────────────────────────────────────────────────────

    @Async("emailExecutor")
    public void sendPlacementDriveInvitation(String to, String studentName, String companyName, String roleTitle,
                                             String driveDate, String location, String criteria) {
        String html = "<html><body style='font-family:Arial,sans-serif;color:#111827;'>"
                + "<h2>New Placement Drive Opportunity</h2>"
                + "<p>Dear <strong>" + studentName + "</strong>,</p>"
                + "<p>You are eligible for the <strong>" + roleTitle + "</strong> drive at <strong>" + companyName + "</strong>.</p>"
                + "<p><strong>Date:</strong> " + driveDate + "<br/>"
                + "<strong>Location:</strong> " + location + "<br/>"
                + "<strong>Eligibility:</strong> " + criteria + "</p>"
                + "<p>Please review the requirements in your student placement page and apply if interested.</p>"
                + "</body></html>";
        sendHtmlEmail(to, "Placement Drive: " + companyName + " - " + roleTitle, html);
    }

    @Async("emailExecutor")
    public void sendPlacementDriveReminder(String to, String studentName, String companyName, String roleTitle,
                                           int reminderNumber) {
        String html = "<html><body style='font-family:Arial,sans-serif;color:#111827;'>"
                + "<h2>Placement Drive Reminder</h2>"
                + "<p>Dear <strong>" + studentName + "</strong>,</p>"
                + "<p>This is reminder " + reminderNumber + " for the <strong>" + roleTitle + "</strong> opportunity at <strong>"
                + companyName + "</strong>.</p>"
                + "<p>You are marked eligible but have not applied yet. Please check the drive details on your placement page.</p>"
                + "</body></html>";
        sendHtmlEmail(to, "Reminder " + reminderNumber + ": Placement Drive Pending Application", html);
    }

    @Async("emailExecutor")
    public void sendMentorPlacementAlert(String to, String mentorName, String companyName, String roleTitle, String studentList) {
        String html = "<html><body style='font-family:Arial,sans-serif;color:#111827;'>"
                + "<h2>Mentor Alert: Students Have Not Applied</h2>"
                + "<p>Dear <strong>" + mentorName + "</strong>,</p>"
                + "<p>The following students are eligible for the <strong>" + roleTitle + "</strong> drive at <strong>"
                + companyName + "</strong> but have still not applied after two reminders:</p>"
                + "<p>" + studentList.replace("\n", "<br/>") + "</p>"
                + "<p>Please follow up with them.</p>"
                + "</body></html>";
        sendHtmlEmail(to, "Mentor Alert: Placement Drive Non-Registration", html);
    }

    @Async("emailExecutor")
    public void sendPlacementApplicationReview(String to, String studentName, String companyName, String roleTitle,
                                               String status, String remarks) {
        String html = "<html><body style='font-family:Arial,sans-serif;color:#111827;'>"
                + "<h2>Placement Application Update</h2>"
                + "<p>Dear <strong>" + studentName + "</strong>,</p>"
                + "<p>Your application for <strong>" + roleTitle + "</strong> at <strong>" + companyName
                + "</strong> has been marked as <strong>" + status + "</strong>.</p>"
                + (remarks == null || remarks.isBlank() ? "" : "<p><strong>Coordinator notes:</strong> " + remarks + "</p>")
                + "</body></html>";
        sendHtmlEmail(to, "Placement Application Status: " + status, html);
    }

    @Async("emailExecutor")
    public void sendFacultyLeaveApproved(String email, String name, String from, String to, String remarks) {
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
                + "<body style='font-family:sans-serif;background:#f3f4f6;margin:0;padding:0;'>"
                + "<div style='max-width:480px;margin:30px auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 16px rgba(0,0,0,.08);'>"
                + "<div style='background:#10b981;padding:28px 24px;text-align:center;'>"
                + "<h1 style='margin:0;color:#fff;font-size:22px;letter-spacing:1px;'>ACADEMIC PORTAL</h1>"
                + "<p style='margin:6px 0 0;color:#d1fae5;font-size:13px;'>Faculty Leave – Final Decision</p></div>"
                + "<div style='padding:30px 24px;'>"
                + "<h2 style='color:#065f46;margin:0 0 12px;'>✅ Leave Approved</h2>"
                + "<p style='color:#374151;'>Dear <strong>" + name + "</strong>,</p>"
                + "<p style='color:#374151;'>Your leave request has been <strong style='color:#10b981;'>APPROVED</strong> by the Admin.</p>"
                + "<div style='background:#f0fdf4;border:1px solid #bbf7d0;border-radius:8px;padding:16px;margin:16px 0;'>"
                + "<p style='margin:4px 0;color:#374151;'><strong>Period:</strong> " + from + " to " + to + "</p>"
                + (remarks != null && !remarks.isBlank() ? "<p style='margin:4px 0;color:#374151;'><strong>Remarks:</strong> " + remarks + "</p>" : "")
                + "</div><p style='color:#6b7280;font-size:13px;'>Please make necessary arrangements before your leave period.</p></div>"
                + "<div style='background:#f9fafb;padding:14px;text-align:center;font-size:11px;color:#9ca3af;border-top:1px solid #e5e7eb;'>"
                + "&copy; 2026 Academic Platform System</div></div></body></html>";
        sendHtmlEmail(email, "Leave Approved – " + from + " to " + to, html);
    }

    @Async("emailExecutor")
    public void sendFacultyLeaveRejected(String email, String name, String from, String to, String remarks) {
        String html = "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
                + "<body style='font-family:sans-serif;background:#f3f4f6;margin:0;padding:0;'>"
                + "<div style='max-width:480px;margin:30px auto;background:#fff;border-radius:12px;overflow:hidden;box-shadow:0 4px 16px rgba(0,0,0,.08);'>"
                + "<div style='background:#ef4444;padding:28px 24px;text-align:center;'>"
                + "<h1 style='margin:0;color:#fff;font-size:22px;'>ACADEMIC PORTAL</h1>"
                + "<p style='margin:6px 0 0;color:#fecaca;font-size:13px;'>Faculty Leave – Final Decision</p></div>"
                + "<div style='padding:30px 24px;'>"
                + "<h2 style='color:#991b1b;margin:0 0 12px;'>❌ Leave Rejected</h2>"
                + "<p>Dear <strong>" + name + "</strong>,</p>"
                + "<p>Your leave request (<strong>" + from + " to " + to + "</strong>) has been <strong style='color:#ef4444;'>REJECTED</strong>.</p>"
                + (remarks != null && !remarks.isBlank() ? "<div style='background:#fef2f2;border:1px solid #fecaca;border-radius:8px;padding:14px;margin:14px 0;'><p style='margin:0;color:#374151;'><strong>Reason:</strong> " + remarks + "</p></div>" : "")
                + "<p style='color:#6b7280;font-size:13px;'>You may re-apply or contact your HOD/Admin for further clarification.</p></div>"
                + "<div style='background:#f9fafb;padding:14px;text-align:center;font-size:11px;color:#9ca3af;border-top:1px solid #e5e7eb;'>"
                + "&copy; 2026 Academic Platform System</div></div></body></html>";
        sendHtmlEmail(email, "Leave Rejected – " + from + " to " + to, html);
    }
}
