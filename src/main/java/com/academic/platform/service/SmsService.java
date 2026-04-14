package com.academic.platform.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.logging.Logger;

@Service
public class SmsService {

    private static final Logger logger = Logger.getLogger(SmsService.class.getName());

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final SystemSettingService systemSettingService;

    @Value("${app.sms.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${app.sms.twilio.auth-token:}")
    private String twilioAuthToken;

    @Value("${app.sms.twilio.from-number:}")
    private String twilioFromNumber;

    public SmsService(SystemSettingService systemSettingService) {
        this.systemSettingService = systemSettingService;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }

        return "";
    }

    private String getResolvedAccountSid() {
        return firstNonBlank(twilioAccountSid, systemSettingService.getSetting("app.sms.twilio.account-sid"));
    }

    private String getResolvedAuthToken() {
        return firstNonBlank(twilioAuthToken, systemSettingService.getSetting("app.sms.twilio.auth-token"));
    }

    private String getResolvedFromNumber() {
        return firstNonBlank(twilioFromNumber, systemSettingService.getSetting("app.sms.twilio.from-number"));
    }

    private boolean isSmsEnabled() {
        return !"false".equalsIgnoreCase(systemSettingService.getSetting("smsNotifications"));
    }

    private boolean isTwilioConfigured() {
        return !getResolvedAccountSid().isBlank()
                && !getResolvedAuthToken().isBlank()
                && !getResolvedFromNumber().isBlank();
    }

    private String normalizePhoneNumber(String rawPhone) {
        if (rawPhone == null) {
            return "";
        }

        String cleaned = rawPhone.replaceAll("[^\\d+]", "");
        if (cleaned.startsWith("+")) {
            return cleaned;
        }

        if (cleaned.length() == 10) {
            return "+91" + cleaned;
        }

        if (cleaned.length() == 12 && cleaned.startsWith("91")) {
            return "+" + cleaned;
        }

        return cleaned;
    }

    private void sendSms(String parentPhone, String message, String successLogMessage) {
        if (!isSmsEnabled()) {
            logger.info("SMS notifications are disabled. Skipping SMS to: " + parentPhone);
            return;
        }

        if (!isTwilioConfigured()) {
            logger.warning("SMS skipped because Twilio is not configured.");
            return;
        }

        String normalizedPhone = normalizePhoneNumber(parentPhone);
        if (normalizedPhone.isBlank()) {
            logger.warning("SMS skipped because parent phone number is missing or invalid.");
            return;
        }

        String accountSid = getResolvedAccountSid();
        String authToken = getResolvedAuthToken();
        String fromNumber = getResolvedFromNumber();

        try {
            String auth = Base64.getEncoder()
                    .encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));

            String form = "To=" + URLEncoder.encode(normalizedPhone, StandardCharsets.UTF_8)
                    + "&From=" + URLEncoder.encode(fromNumber, StandardCharsets.UTF_8)
                    + "&Body=" + URLEncoder.encode(message, StandardCharsets.UTF_8);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json"))
                    .header("Authorization", "Basic " + auth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                logger.warning("Failed to send SMS. Twilio status: " + response.statusCode() + ", body: " + response.body());
            } else {
                logger.info(successLogMessage + normalizedPhone);
            }
        } catch (Exception ex) {
            logger.warning("Failed to send SMS: " + ex.getMessage());
        }
    }

    @Async("emailExecutor")
    public void sendParentLeaveApplicationSms(String parentPhone, String studentName, String reason,
                                              String fromDate, String toDate, String approvalLink, String otp) {

        String safeStudentName = studentName == null || studentName.isBlank() ? "your ward" : studentName;
        String safeReason = reason == null || reason.isBlank() ? "Leave request submitted" : reason;
        String safeOtp = otp == null || otp.isBlank() ? "N/A" : otp;

        String message = "Academic Portal leave OTP: " + safeOtp
                + ". Student: " + safeStudentName
                + ". Dates: " + fromDate + " to " + toDate
                + ". Reason: " + safeReason
                + ". Review: " + approvalLink;

        sendSms(parentPhone, message, "Parent leave application SMS sent successfully to: ");
    }

    @Async("emailExecutor")
    public void sendParentOtpCodeSms(String parentPhone, String studentName, String otp) {
        String safeStudentName = studentName == null || studentName.isBlank() ? "your ward" : studentName;
        String safeOtp = otp == null || otp.isBlank() ? "N/A" : otp;

        String message = "Academic Portal approval OTP for " + safeStudentName
                + ": " + safeOtp
                + ". Share this code only with the assigned mentor. Valid for 7 days.";

        sendSms(parentPhone, message, "Parent OTP SMS sent successfully to: ");
    }
}
