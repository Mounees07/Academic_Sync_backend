package com.academic.platform.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String emailShell(String preheader, String title, String subtitle, String content) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'></head>"
                + "<body style='margin:0;padding:0;background:#eef2f7;font-family:Segoe UI,Arial,sans-serif;color:#0f172a;'>"
                + "<div style='display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;'>"
                + escapeHtml(preheader)
                + "</div>"
                + "<table role='presentation' width='100%' cellspacing='0' cellpadding='0' style='background:#eef2f7;margin:0;padding:24px 0;'>"
                + "<tr><td align='center'>"
                + "<table role='presentation' width='100%' cellspacing='0' cellpadding='0' style='max-width:640px;background:#ffffff;border-radius:24px;overflow:hidden;box-shadow:0 20px 48px rgba(15,23,42,0.12);'>"
                + "<tr><td style='padding:0;'>"
                + "<div style='background:linear-gradient(135deg,#0f4c81,#1d4ed8 58%,#0ea5a4);padding:32px 36px;color:#ffffff;'>"
                + "<div style='font-size:12px;font-weight:700;letter-spacing:0.14em;text-transform:uppercase;opacity:0.82;'>Academic Portal</div>"
                + "<h1 style='margin:12px 0 6px;font-size:30px;line-height:1.15;font-weight:800;color:#ffffff;'>" + escapeHtml(title) + "</h1>"
                + "<p style='margin:0;font-size:15px;line-height:1.6;color:rgba(255,255,255,0.88);'>" + escapeHtml(subtitle) + "</p>"
                + "</div>"
                + "</td></tr>"
                + "<tr><td style='padding:32px 36px;background:#ffffff;'>" + content + "</td></tr>"
                + "<tr><td style='padding:18px 36px;background:#f8fafc;border-top:1px solid #e2e8f0;'>"
                + "<p style='margin:0;font-size:12px;line-height:1.7;color:#64748b;'>This is an automated message from Academic Portal. Please do not reply directly to this email.</p>"
                + "</td></tr>"
                + "</table>"
                + "</td></tr></table>"
                + "</body></html>";
    }

    private String infoGridRow(String label, String value) {
        return "<tr>"
                + "<td style='padding:12px 0 4px;font-size:12px;font-weight:700;letter-spacing:0.08em;text-transform:uppercase;color:#64748b;'>"
                + escapeHtml(label)
                + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td style='padding:0 0 14px;font-size:15px;line-height:1.6;color:#0f172a;border-bottom:1px solid #e2e8f0;'>"
                + escapeHtml(value)
                + "</td>"
                + "</tr>";
    }

    private String otpPanel(String otp, String helperText) {
        return "<div style='margin:24px 0;padding:22px 20px;border:1px solid #bbf7d0;border-radius:20px;background:linear-gradient(180deg,#f0fdf4,#ecfdf5);text-align:center;'>"
                + "<div style='font-size:12px;font-weight:800;letter-spacing:0.12em;text-transform:uppercase;color:#166534;margin-bottom:8px;'>Authorization Code</div>"
                + "<div style='font-family:Consolas,Menlo,monospace;font-size:34px;font-weight:800;letter-spacing:8px;color:#166534;'>"
                + escapeHtml(otp)
                + "</div>"
                + "<div style='margin-top:10px;font-size:13px;line-height:1.6;color:#166534;'>"
                + escapeHtml(helperText)
                + "</div>"
                + "</div>";
    }

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SystemSettingService systemSettingService;

    @Value("${app.mail.from:${spring.mail.username:}}")
    private String mailFrom;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void applyFrom(org.springframework.mail.javamail.MimeMessageHelper helper)
            throws jakarta.mail.MessagingException {
        if (!isBlank(mailFrom)) {
            helper.setFrom(mailFrom.trim());
        }
    }

    private void applyFrom(SimpleMailMessage message) {
        if (!isBlank(mailFrom)) {
            message.setFrom(mailFrom.trim());
        }
    }

    @Async("emailExecutor")
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        sendHtmlEmailNow(to, subject, htmlBody);
    }

    public void sendHtmlEmailNow(String to, String subject, String htmlBody) {
        if ("false".equalsIgnoreCase(systemSettingService.getSetting("emailNotifications"))) {
            System.out.println("Email notifications are disabled. Skipping email to: " + to);
            return;
        }

        if (isBlank(to)) {
            throw new IllegalArgumentException("Email recipient is missing.");
        }

        if (isBlank(mailUsername) || isBlank(mailPassword)) {
            throw new IllegalStateException("Mail is not configured. Set MAIL_USERNAME and MAIL_PASSWORD in production.");
        }

        try {
            jakarta.mail.internet.MimeMessage message = mailSender.createMimeMessage();
            org.springframework.mail.javamail.MimeMessageHelper helper = new org.springframework.mail.javamail.MimeMessageHelper(
                    message, true, "UTF-8");

            applyFrom(helper);
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
        if (isBlank(to)) {
            System.err.println("Skipping meeting email: recipient is missing.");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        applyFrom(message);
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
        if (bcc == null || bcc.length == 0) {
            System.err.println("Skipping bulk meeting email: recipients are missing.");
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        applyFrom(message);
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
        String content = "<p style='margin:0 0 18px;font-size:16px;line-height:1.7;color:#334155;'>Dear Parent/Guardian,</p>"
                + "<p style='margin:0 0 20px;font-size:15px;line-height:1.8;color:#475569;'>"
                + "A leave request has been submitted for <strong style='color:#0f172a;'>" + escapeHtml(studentName) + "</strong>. "
                + "Please review the details below and authorize the request if everything is correct."
                + "</p>"
                + "<table role='presentation' width='100%' cellspacing='0' cellpadding='0' style='padding:18px 20px;background:#f8fafc;border:1px solid #e2e8f0;border-radius:18px;'>"
                + infoGridRow("Requested Dates", from + " to " + to)
                + infoGridRow("Reason", leaveReason)
                + "</table>"
                + "<div style='margin:24px 0 18px;padding:18px 20px;border-radius:18px;background:#eff6ff;border:1px solid #bfdbfe;'>"
                + "<p style='margin:0 0 10px;font-size:14px;line-height:1.7;color:#1e3a8a;font-weight:700;'>Approve online</p>"
                + "<p style='margin:0 0 16px;font-size:14px;line-height:1.7;color:#475569;'>Use the secure review link below to confirm the leave request in one step.</p>"
                + "<a href='" + approvalLink + "' style='display:inline-block;padding:13px 22px;background:#2563eb;color:#ffffff;text-decoration:none;border-radius:12px;font-size:14px;font-weight:700;'>Review Leave Request</a>"
                + "</div>"
                + "<p style='margin:0 0 8px;font-size:14px;line-height:1.7;color:#475569;'>"
                + "If you prefer to share a code with the mentor, use the authorization code below."
                + "</p>"
                + otpPanel(otp, "Valid for 7 days. Share only with the assigned mentor.")
                + "<div style='padding:16px 18px;border-radius:16px;background:#fff7ed;border:1px solid #fed7aa;'>"
                + "<p style='margin:0;font-size:13px;line-height:1.7;color:#9a3412;'>For your security, please do not share this code with anyone other than the faculty mentor handling this leave request.</p>"
                + "</div>";

        String html = emailShell(
                "Leave authorization required for " + studentName,
                "Leave Authorization Required",
                "Please review and confirm your ward's leave request.",
                content);

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

    private String buildActionOtpHtml(String otp, String actionDescription) {
        return "<!DOCTYPE html>"
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
    }

    @Async("emailExecutor")
    public void sendActionOtp(String to, String otp, String actionDescription) {
        sendHtmlEmailNow(to, "Verification OTP: " + otp, buildActionOtpHtml(otp, actionDescription));
    }

    public void sendActionOtpNow(String to, String otp, String actionDescription) {
        sendHtmlEmailNow(to, "Verification OTP: " + otp, buildActionOtpHtml(otp, actionDescription));
    }

    @Async("emailExecutor")
    public void sendParentOtpCode(String parentEmail, String studentName, String otp) {
        String content = "<p style='margin:0 0 18px;font-size:16px;line-height:1.7;color:#334155;'>Dear Parent/Guardian,</p>"
                + "<p style='margin:0 0 18px;font-size:15px;line-height:1.8;color:#475569;'>"
                + "You have approved the leave request for <strong style='color:#0f172a;'>" + escapeHtml(studentName) + "</strong>."
                + "</p>"
                + "<p style='margin:0 0 8px;font-size:14px;line-height:1.7;color:#475569;'>Please share the authorization code below with the mentor to complete the approval workflow.</p>"
                + otpPanel(otp, "Valid for 7 days. This code finalizes the leave approval.")
                + "<div style='padding:16px 18px;border-radius:16px;background:#f8fafc;border:1px solid #e2e8f0;'>"
                + "<p style='margin:0;font-size:13px;line-height:1.7;color:#475569;'>If this request was not approved by you, please contact the institution immediately.</p>"
                + "</div>";

        String html = emailShell(
                "Authorization code for approved leave request",
                "Leave Approval Confirmed",
                "Share this secure code with the mentor to complete the process.",
                content);

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
