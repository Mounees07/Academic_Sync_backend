package com.academic.platform.config;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
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

    private static final String ADMIN_EMAIL    = "sankavi8881@gmail.com";
    private static final String ADMIN_PASSWORD  = "Admin@1234"; // default password for first-time login

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🔧 Running Database Schema Fixes...");
        try {
            // PostgreSQL uses ALTER COLUMN syntax (not MySQL MODIFY COLUMN)
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(50)");
            System.out.println("✅ Successfully altered 'users' table 'role' column to VARCHAR(50)");
        } catch (Exception e) {
            System.out.println("⚠️ Database fix skipped (or failed): " + e.getMessage());
        }

        // ── Step 1: Ensure admin exists in Firebase (with password login) ──────────
        String firebaseUid = null;
        boolean firebaseReady = !FirebaseApp.getApps().isEmpty();

        if (firebaseReady) {
            try {
                // Try to get the user from Firebase
                UserRecord fbUser = FirebaseAuth.getInstance().getUserByEmail(ADMIN_EMAIL);
                firebaseUid = fbUser.getUid();
                System.out.println("ℹ️ Firebase admin account already exists. UID=" + firebaseUid);
            } catch (com.google.firebase.auth.FirebaseAuthException e) {
                if ("USER_NOT_FOUND".equals(e.getErrorCode().toString()) || e.getMessage().contains("USER_NOT_FOUND")) {
                    // Create the Firebase user with email + password
                    try {
                        UserRecord.CreateRequest req = new UserRecord.CreateRequest()
                                .setEmail(ADMIN_EMAIL)
                                .setDisplayName("System Admin")
                                .setPassword(ADMIN_PASSWORD)
                                .setEmailVerified(true);
                        UserRecord created = FirebaseAuth.getInstance().createUser(req);
                        firebaseUid = created.getUid();
                        System.out.println("✅ Created Firebase admin account. UID=" + firebaseUid);
                    } catch (Exception createEx) {
                        System.out.println("⚠️ Could not create Firebase admin: " + createEx.getMessage());
                    }
                } else {
                    System.out.println("⚠️ Firebase lookup failed: " + e.getMessage());
                }
            }
        } else {
            System.out.println("⚠️ Firebase not ready — skipping Firebase admin sync.");
        }

        // ── Step 2: Ensure admin exists in Database with correct role & UID ────────
        try {
            var adminUserOpt = userRepository.findByEmail(ADMIN_EMAIL);
            if (adminUserOpt.isPresent()) {
                var adminUser = adminUserOpt.get();
                boolean dirty = false;

                // Fix role if wrong
                if (adminUser.getRole() != com.academic.platform.model.Role.ADMIN) {
                    adminUser.setRole(com.academic.platform.model.Role.ADMIN);
                    dirty = true;
                }

                // Fix UID if we got the real Firebase UID and it differs from placeholder
                if (firebaseUid != null && !firebaseUid.equals(adminUser.getFirebaseUid())) {
                    adminUser.setFirebaseUid(firebaseUid);
                    dirty = true;
                }

                if (dirty) {
                    userRepository.save(adminUser);
                    System.out.println("✅ Updated ADMIN user in DB (role + UID synced)");
                } else {
                    System.out.println("ℹ️ " + ADMIN_EMAIL + " is already correct in DB.");
                }
            } else {
                // Create the DB user from scratch
                com.academic.platform.model.User newAdmin = new com.academic.platform.model.User();
                newAdmin.setEmail(ADMIN_EMAIL);
                newAdmin.setFullName("System Admin");
                newAdmin.setRole(com.academic.platform.model.Role.ADMIN);
                newAdmin.setFirebaseUid(firebaseUid != null ? firebaseUid : "admin_placeholder_" + System.currentTimeMillis());

                com.academic.platform.model.StudentDetails sd = new com.academic.platform.model.StudentDetails();
                newAdmin.setStudentDetails(sd);

                userRepository.save(newAdmin);
                System.out.println("✅ Created ADMIN user in DB: " + ADMIN_EMAIL);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Admin role enforcement failed: " + e.getMessage());
        }
    }
}
