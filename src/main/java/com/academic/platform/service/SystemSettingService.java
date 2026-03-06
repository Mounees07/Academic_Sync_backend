package com.academic.platform.service;

import com.academic.platform.model.SystemSetting;
import com.academic.platform.repository.SystemSettingRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

@Service
public class SystemSettingService {

    private static final Logger logger = Logger.getLogger(SystemSettingService.class.getName());

    @Autowired
    private SystemSettingRepository settingRepository;

    @Autowired
    private AuditLogService auditLogService;

    // Cache settings in memory on app start
    private final Map<String, String> cachedSettings = new HashMap<>();

    @PostConstruct
    public void init() {
        logger.info("Initializing System Settings Cache...");
        List<SystemSetting> all = settingRepository.findAll();
        for (SystemSetting s : all) {
            cachedSettings.put(s.getKey(), s.getValue());
        }

        // Ensure defaults for system settings
        ensureDefault("siteName", "AcaSync Platform", "Name of the platform");
        ensureDefault("adminEmail", "admin@acasync.edu", "Main support email");
        ensureDefault("maintenanceMode", "false", "If true, blocks non-admin access");

        // USER_REQUEST: Force Maintenance Mode OFF in code
        // This block ensures that even if the DB has it as "true", it gets reset to
        // "false" on startup.
        if ("true".equals(cachedSettings.get("maintenanceMode"))) {
            logger.info("Forcing Maintenance Mode OFF via code override.");
            Optional<SystemSetting> sOpt = settingRepository.findById("maintenanceMode");
            if (sOpt.isPresent()) {
                SystemSetting s = sOpt.get();
                s.setValue("false");
                settingRepository.save(s);
                cachedSettings.put("maintenanceMode", "false");
            }
        }

        ensureDefault("allowRegistration", "true", "Allow public signup");
        ensureDefault("emailNotifications", "true", "Enable system emails");
        ensureDefault("defaultLanguage", "English", "Default UI language");
        ensureDefault("sessionTimeout", "30", "Session timeout in minutes");

        // --- 1. Policy & Governance ---
        ensureDefault("policy.attendance.threshold", "75", "Minimum attendance percentage");
        ensureDefault("policy.attendance.detain", "65", "Detain students below this attendance %");
        ensureDefault("policy.leave.maxDays", "10", "Maximum leave days per semester");
        ensureDefault("policy.password.minLength", "8", "Minimum password length");
        ensureDefault("policy.password.complexity", "strong", "Password strength requirement");
        ensureDefault("policy.dataRetention", "365", "Days to retain logs/records");

        // --- 2. Feature Toggles ---
        ensureDefault("feature.leave.enabled", "true", "Enable leave management module");

        // Force Leave Feature ON (Fix for 403 error)
        if ("false".equals(cachedSettings.get("feature.leave.enabled"))) {
            logger.info("Forcing Leave Feature ON via code override.");
            Optional<SystemSetting> sOpt = settingRepository.findById("feature.leave.enabled");
            if (sOpt.isPresent()) {
                SystemSetting s = sOpt.get();
                s.setValue("true");
                settingRepository.save(s);
                cachedSettings.put("feature.leave.enabled", "true");
            }
        }

        ensureDefault("feature.result.enabled", "true", "Enable results module");
        ensureDefault("feature.analytics.enabled", "true", "Enable analytics dashboard");
        ensureDefault("feature.messaging.enabled", "true", "Enable internal messaging");
        ensureDefault("feature.courseRegistration.enabled", "true", "Allow students to self-register faculty");
        ensureDefault("feature.assignments.enabled", "true", "Enable assignment submission module");
        ensureDefault("feature.finance.enabled", "true", "Enable finance / fee payment module");
        ensureDefault("feature.examSeating.enabled", "true", "Enable COE exam seating allocation");

        // --- 4. Rate Limiting ---
        ensureDefault("security.api.rateLimit", "100", "Requests per minute");
        ensureDefault("security.login.maxAttempts", "5", "Max failed login attempts before lockout");
        ensureDefault("security.captcha.enabled", "false", "Enable CAPTCHA on login");

        // --- 6. Reporting & Analytics ---
        ensureDefault("report.kpi.attendanceAlert", "70", "Alert if attendance drops below %");
        ensureDefault("report.export.enabled", "true", "Allow data export (PDF/CSV)");

        // --- 8. Environment ---
        ensureDefault("env.label", "Production", "Environment label (Dev/Test/Prod)");
        ensureDefault("env.debugMode", "false", "Enable debug logging");

        // --- 11. Accessibility ---
        ensureDefault("ui.accessibility.contrast", "normal", "Contrast mode (normal/high)");
        ensureDefault("ui.font.scalability", "true", "Allow font scaling");
    }

    private void ensureDefault(String key, String value, String desc) {
        if (!cachedSettings.containsKey(key)) {
            logger.info("Creating default system setting: " + key);
            SystemSetting setting = new SystemSetting(key, value, desc);
            settingRepository.save(setting);
            cachedSettings.put(key, value);
        }
    }

    public Map<String, String> getAllSettings() {
        // Refresh from DB just in case, or use cache
        List<SystemSetting> all = settingRepository.findAll();
        Map<String, String> map = new HashMap<>();
        for (SystemSetting s : all) {
            map.put(s.getKey(), s.getValue());
        }
        return map;
    }

    public String getSetting(String key) {
        return cachedSettings.getOrDefault(key, "");
    }

    // Check specific boolean settings efficiently
    public boolean isMaintenanceMode() {
        return Boolean.parseBoolean(getSetting("maintenanceMode"));
    }

    public boolean isRegistrationAllowed() {
        return Boolean.parseBoolean(getSetting("allowRegistration"));
    }

    public void updateSettings(Map<String, String> updates, String adminUid, String adminEmail, String ip) {
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            String key = entry.getKey();
            String newVal = entry.getValue();

            // Validate changes if needed

            Optional<SystemSetting> existing = settingRepository.findById(key);
            if (existing.isPresent()) {
                SystemSetting s = existing.get();
                if (!s.getValue().equals(newVal)) {
                    s.setValue(newVal);
                    settingRepository.save(s);
                    cachedSettings.put(key, newVal);

                    // Audit log for critical changes
                    if (key.equals("maintenanceMode") || key.equals("allowRegistration")) {
                        auditLogService.log(adminUid, adminEmail, "UPDATE_CONFIG",
                                "Changed " + key + " to " + newVal, ip);
                    }
                }
            } else {
                // New setting?
                SystemSetting s = new SystemSetting(key, newVal, "User Defined");
                settingRepository.save(s);
                cachedSettings.put(key, newVal);
            }
        }
        auditLogService.log(adminUid, adminEmail, "UPDATE_SETTINGS_BULK", "Updated system configuration", ip);
    }

    public Map<String, String> getPublicSettings() {
        Map<String, String> publicSettings = new HashMap<>();
        Map<String, String> all = getAllSettings();

        for (Map.Entry<String, String> entry : all.entrySet()) {
            String key = entry.getKey();
            // Whitelist safe settings for frontend consumption
            if (key.startsWith("feature.") ||
                    key.startsWith("policy.") ||
                    key.equals("siteName") ||
                    key.equals("allowRegistration") ||
                    key.equals("maintenanceMode") ||
                    key.equals("emailNotifications") ||
                    key.equals("report.export.enabled") ||
                    key.equals("defaultLanguage") ||
                    key.startsWith("ui.")) {
                publicSettings.put(key, entry.getValue());
            }
        }
        return publicSettings;
    }
}
