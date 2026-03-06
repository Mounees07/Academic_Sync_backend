package com.academic.platform.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseFixer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private com.academic.platform.repository.UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🔧 Running Database Schema Fixes...");
        try {
            // Fix for 'Data truncated' error on 'role' column.
            // This happens when the column was created as a MySQL ENUM missing the 'COE'
            // value.
            // Converting to VARCHAR(50) makes it flexible for any Enum value defined in
            // Java.
            jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN role VARCHAR(50)");
            System.out.println("✅ Successfully altered 'users' table 'role' column to VARCHAR(50)");
        } catch (Exception e) {
            System.out.println("⚠️ Database fix skipped (or failed): " + e.getMessage());
        }

        // Enforce ADMIN role for specific email
        try {
            String adminEmail = "sankavi8881@gmail.com";
            var adminUserOpt = userRepository.findByEmail(adminEmail);
            if (adminUserOpt.isPresent()) {
                var adminUser = adminUserOpt.get();
                if (adminUser.getRole() != com.academic.platform.model.Role.ADMIN) {
                    adminUser.setRole(com.academic.platform.model.Role.ADMIN);
                    userRepository.save(adminUser);
                    System.out.println("✅ Enforced ADMIN role for " + adminEmail);
                } else {
                    System.out.println("ℹ️ " + adminEmail + " already has ADMIN role");
                }
            } else {
                System.out.println("⚠️ Admin user " + adminEmail
                        + " not found in database. Please run /api/seed/init-admin endpoint.");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Admin role enforcement failed: " + e.getMessage());
        }
    }
}
