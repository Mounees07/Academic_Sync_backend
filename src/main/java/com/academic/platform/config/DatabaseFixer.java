package com.academic.platform.config;

import com.academic.platform.model.Role;
import com.academic.platform.model.StudentDetails;
import com.academic.platform.model.User;
import com.academic.platform.repository.UserRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
public class DatabaseFixer implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "sankavi8881@gmail.com";
    private static final String ADMIN_PASSWORD = "Admin@1234";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) {
        System.out.println("Running database schema fixes...");
        widenUsersRoleColumn();
        syncUsersRoleConstraint();
        syncPlacementDriveApplicationColumns();
        ensureAdminUser();
    }

    private void widenUsersRoleColumn() {
        try {
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(50)");
            System.out.println("Updated users.role column to VARCHAR(50)");
        } catch (Exception e) {
            System.out.println("Skipping role column update: " + e.getMessage());
        }
    }

    private void syncUsersRoleConstraint() {
        String allowedRoles = Arrays.stream(Role.values())
                .map(Role::name)
                .map(role -> "'" + role + "'")
                .collect(Collectors.joining(", "));

        try {
            jdbcTemplate.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
            jdbcTemplate.execute(
                    "ALTER TABLE users ADD CONSTRAINT users_role_check CHECK (role IN (" + allowedRoles + "))");
            System.out.println("Synced users_role_check with roles: " + allowedRoles);
        } catch (Exception e) {
            System.out.println("Could not sync users_role_check: " + e.getMessage());
        }
    }

    private void syncPlacementDriveApplicationColumns() {
        try {
            if (!columnExists("placement_drive_applications", "attended")) {
                jdbcTemplate.execute("ALTER TABLE placement_drive_applications ADD COLUMN attended BOOLEAN");
                System.out.println("Added placement_drive_applications.attended column");
            }

            jdbcTemplate.execute("UPDATE placement_drive_applications SET attended = FALSE WHERE attended IS NULL");
            jdbcTemplate.execute("ALTER TABLE placement_drive_applications ALTER COLUMN attended SET DEFAULT FALSE");
            jdbcTemplate.execute("ALTER TABLE placement_drive_applications ALTER COLUMN attended SET NOT NULL");

            if (!columnExists("placement_drive_applications", "attendance_marked_at")) {
                jdbcTemplate.execute("ALTER TABLE placement_drive_applications ADD COLUMN attendance_marked_at TIMESTAMP");
                System.out.println("Added placement_drive_applications.attendance_marked_at column");
            }

            System.out.println("Synced placement drive attendance columns.");
        } catch (Exception e) {
            System.out.println("Could not sync placement drive attendance columns: " + e.getMessage());
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                  AND column_name = ?
                """,
                Integer.class,
                tableName,
                columnName
        );
        return count != null && count > 0;
    }

    private void ensureAdminUser() {
        String firebaseUid = null;
        boolean firebaseReady = !FirebaseApp.getApps().isEmpty();

        if (firebaseReady) {
            try {
                UserRecord fbUser = FirebaseAuth.getInstance().getUserByEmail(ADMIN_EMAIL);
                firebaseUid = fbUser.getUid();
                System.out.println("Firebase admin account already exists. UID=" + firebaseUid);
            } catch (FirebaseAuthException e) {
                if (isUserNotFound(e)) {
                    firebaseUid = createFirebaseAdmin();
                } else {
                    System.out.println("Firebase lookup failed: " + e.getMessage());
                }
            }
        } else {
            System.out.println("Firebase not ready, skipping admin sync.");
        }

        try {
            var adminUserOpt = userRepository.findByEmail(ADMIN_EMAIL);
            if (adminUserOpt.isPresent()) {
                User adminUser = adminUserOpt.get();
                boolean dirty = false;

                if (adminUser.getRole() != Role.ADMIN) {
                    adminUser.setRole(Role.ADMIN);
                    dirty = true;
                }

                if (firebaseUid != null && !firebaseUid.equals(adminUser.getFirebaseUid())) {
                    adminUser.setFirebaseUid(firebaseUid);
                    dirty = true;
                }

                if (dirty) {
                    userRepository.save(adminUser);
                    System.out.println("Updated admin user in database.");
                } else {
                    System.out.println(ADMIN_EMAIL + " is already correct in database.");
                }
                return;
            }

            User newAdmin = new User();
            newAdmin.setEmail(ADMIN_EMAIL);
            newAdmin.setFullName("System Admin");
            newAdmin.setRole(Role.ADMIN);
            newAdmin.setFirebaseUid(firebaseUid != null
                    ? firebaseUid
                    : "admin_placeholder_" + System.currentTimeMillis());
            newAdmin.setStudentDetails(new StudentDetails());

            userRepository.save(newAdmin);
            System.out.println("Created admin user in database: " + ADMIN_EMAIL);
        } catch (Exception e) {
            System.out.println("Admin role enforcement failed: " + e.getMessage());
        }
    }

    private String createFirebaseAdmin() {
        try {
            UserRecord.CreateRequest req = new UserRecord.CreateRequest()
                    .setEmail(ADMIN_EMAIL)
                    .setDisplayName("System Admin")
                    .setPassword(ADMIN_PASSWORD)
                    .setEmailVerified(true);
            UserRecord created = FirebaseAuth.getInstance().createUser(req);
            System.out.println("Created Firebase admin account. UID=" + created.getUid());
            return created.getUid();
        } catch (Exception createEx) {
            System.out.println("Could not create Firebase admin: " + createEx.getMessage());
            return null;
        }
    }

    private boolean isUserNotFound(FirebaseAuthException e) {
        String errorCode = e.getErrorCode() != null ? e.getErrorCode().name() : null;
        String message = e.getMessage();
        return "USER_NOT_FOUND".equals(errorCode)
                || (message != null && message.contains("USER_NOT_FOUND"));
    }
}
