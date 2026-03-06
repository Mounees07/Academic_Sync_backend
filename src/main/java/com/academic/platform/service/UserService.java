package com.academic.platform.service;

import com.academic.platform.model.User;
import com.academic.platform.model.StudentDetails;
import com.academic.platform.model.Role;
import com.academic.platform.repository.UserRepository;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import java.util.HashSet;
import java.util.Set;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SystemSettingService settingService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private com.academic.platform.utils.SecurityUtils securityUtils;

    /**
     * Scalability: @Cacheable prevents repeated DB hits for the SAME user across
     * multiple API requests. The FirebaseTokenFilter calls this on EVERY request;
     * caching it is one of the highest-impact optimisations in this codebase.
     */
    @Cacheable(value = "users", key = "#uid")
    @Transactional(readOnly = true)
    public Optional<User> getUserByFirebaseUid(String uid) {
        Optional<User> userOpt = userRepository.findByFirebaseUid(uid);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Enforce ADMIN role for specific email
            if ("sankavi8881@gmail.com".equalsIgnoreCase(user.getEmail()) && user.getRole() != Role.ADMIN) {
                user.setRole(Role.ADMIN);
                return Optional.of(userRepository.save(user));
            }
        }
        return userOpt;
    }

    @Transactional(readOnly = true)
    public List<User> getUsersByRole(Role role) {
        return userRepository.findByRole(role);
    }

    @Cacheable(value = "faculty", key = "'all'")
    @Transactional(readOnly = true)
    public List<User> getPotentialFaculty() {
        return userRepository.findByRoleIn(List.of(Role.TEACHER, Role.MENTOR, Role.HOD, Role.PRINCIPAL));
    }

    @Cacheable(value = "users", key = "'mentees-' + #mentorUid")
    @Transactional(readOnly = true)
    public List<User> getMenteesByMentor(String mentorUid) {
        return userRepository.findByStudentDetails_Mentor_FirebaseUid(mentorUid);
    }

    @Cacheable(value = "faculty", key = "#department")
    @Transactional(readOnly = true)
    public List<User> getFacultyByDepartment(String department) {
        return userRepository.findByStudentDetails_DepartmentIgnoreCaseAndRoleIn(department,
                List.of(Role.TEACHER, Role.MENTOR));
    }

    @Transactional(readOnly = true)
    public List<User> getStudentsByDepartment(String department) {
        return userRepository.findByStudentDetails_DepartmentIgnoreCaseAndRoleIn(department, List.of(Role.STUDENT));
    }

    private Integer safeParseInt(String value) {
        if (value == null || value.trim().isEmpty())
            return null;
        String cleaned = value.trim().toUpperCase();

        // Handle common prefixes
        cleaned = cleaned.replace("SEM", "").replace("SEMESTER", "").trim();

        try {
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            // Handle Roman Numerals common in Indian Universities
            switch (cleaned) {
                case "I":
                    return 1;
                case "II":
                    return 2;
                case "III":
                    return 3;
                case "IV":
                    return 4;
                case "V":
                    return 5;
                case "VI":
                    return 6;
                case "VII":
                    return 7;
                case "VIII":
                    return 8;
                default:
                    return null;
            }
        }
    }

    public List<String> bulkUploadMenteesForMentor(InputStream inputStream, String mentorUid) {
        List<String> logs = new ArrayList<>();
        User mentor = userRepository.findByFirebaseUid(mentorUid)
                .orElseThrow(() -> new RuntimeException("Mentor not found"));

        try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(inputStream)).withSkipLines(1).build()) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                try {
                    if (line.length < 2)
                        continue;
                    String fullName = line[0].trim();
                    String email = line[1].trim().toLowerCase();
                    String rollNo = (line.length > 2) ? line[2].trim() : null;
                    String dept = (line.length > 3) ? line[3].trim() : null;
                    Integer sem = (line.length > 4) ? safeParseInt(line[4]) : null;
                    String sec = (line.length > 5) ? line[5].trim() : null;

                    User student;
                    Optional<User> existing = userRepository.findByEmail(email);
                    if (existing.isEmpty()) {
                        student = new User();
                        student.setFullName(fullName);
                        student.setEmail(email);
                        student.setRole(Role.STUDENT);
                        student.setFirebaseUid("PRE_REG_" + email);
                        logs.add("Created & Assigned: " + email);
                    } else {
                        student = existing.get();
                        logs.add("Assigned existing: " + email);
                    }

                    StudentDetails details = student.getStudentDetails();
                    details.setMentor(mentor); // Set mentor on StudentDetails
                    if (rollNo != null)
                        details.setRollNumber(rollNo);
                    if (dept != null)
                        details.setDepartment(dept);
                    if (sem != null)
                        details.setSemester(sem);
                    if (sec != null)
                        details.setSection(sec);

                    userRepository.save(student);
                } catch (Exception rowEx) {
                    logs.add("Row Error (" + (line.length > 1 ? line[1] : "unknown") + "): " + rowEx.getMessage());
                }
            }
            logAdminAction("BULK_MENTOR_ASSIGN",
                    "Processed " + logs.size() + " entries for mentor " + mentor.getEmail());
        } catch (Exception e) {
            logs.add("Critical File Error: " + e.getMessage());
        }
        return logs;
    }

    public List<String> bulkAssignMentors(InputStream inputStream) {
        List<String> logs = new ArrayList<>();
        try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(inputStream)).withSkipLines(1).build()) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line.length < 2)
                    continue;
                String studentEmail = line[0].trim().toLowerCase();
                String mentorEmail = line[1].trim().toLowerCase();

                Optional<User> studentOpt = userRepository.findByEmail(studentEmail);
                Optional<User> mentorOpt = userRepository.findByEmail(mentorEmail);

                if (studentOpt.isPresent() && mentorOpt.isPresent()) {
                    User student = studentOpt.get();
                    student.getStudentDetails().setMentor(mentorOpt.get());
                    userRepository.save(student);
                    logs.add("Mapped " + studentEmail + " to " + mentorEmail);
                } else {
                    logs.add("Failed: " + (studentOpt.isEmpty() ? "Student " : "Mentor ") + studentEmail
                            + " not found.");
                }
            }
            logAdminAction("BULK_MENTOR_LINK", "Processed " + logs.size() + " mentor links");
        } catch (Exception e) {
            logs.add("Error: " + e.getMessage());
        }
        return logs;
    }

    public List<String> bulkRegisterUsers(InputStream inputStream, Role role) {
        List<String> logs = new ArrayList<>();
        int minPassLen = Integer.parseInt(settingService.getSetting("policy.password.minLength"));

        try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(inputStream)).withSkipLines(1).build()) {
            String[] line;
            while ((line = reader.readNext()) != null) {
                try {
                    // Expected CSV: Full Name, Email, Password, Roll No, Dept, Sem, Sec
                    if (line.length < 3)
                        continue;

                    String fullName = line[0].trim();
                    String email = line[1].trim().toLowerCase();
                    String password = line[2].trim();
                    String rollNo = (line.length > 3) ? line[3].trim() : null;
                    String dept = (line.length > 4) ? line[4].trim() : null;
                    Integer sem = (line.length > 5) ? safeParseInt(line[5]) : null;
                    String sec = (line.length > 6) ? line[6].trim() : null;

                    if (password.length() < minPassLen) {
                        logs.add("Skipped " + email + ": Password must be at least " + minPassLen + " characters.");
                        continue;
                    }

                    Optional<User> existing = userRepository.findByEmail(email);
                    if (existing.isPresent()) {
                        logs.add("Skipped: " + email + " (Already exists in DB)");
                        continue;
                    }

                    // Create in Firebase
                    String firebaseUid;
                    try {
                        com.google.firebase.auth.UserRecord.CreateRequest request = new com.google.firebase.auth.UserRecord.CreateRequest()
                                .setEmail(email)
                                .setDisplayName(fullName)
                                .setPassword(password)
                                .setEmailVerified(true);

                        com.google.firebase.auth.UserRecord userRecord = com.google.firebase.auth.FirebaseAuth
                                .getInstance().createUser(request);
                        firebaseUid = userRecord.getUid();
                    } catch (com.google.firebase.auth.FirebaseAuthException fae) {
                        if (fae.getErrorCode().equals("email-already-exists")) {
                            com.google.firebase.auth.UserRecord ur = com.google.firebase.auth.FirebaseAuth.getInstance()
                                    .getUserByEmail(email);
                            firebaseUid = ur.getUid();
                            logs.add("User exists in Firebase, syncing to DB: " + email);
                        } else {
                            throw fae;
                        }
                    }

                    User newUser = new User();
                    newUser.setFullName(fullName);
                    newUser.setEmail(email);
                    newUser.setRole(role);
                    newUser.setFirebaseUid(firebaseUid);

                    StudentDetails details = newUser.getStudentDetails();
                    details.setRollNumber(rollNo);
                    details.setDepartment(dept);
                    details.setSemester(sem);
                    details.setSection(sec);

                    userRepository.save(newUser);
                    logs.add("Created: " + email);

                } catch (Exception rowEx) {
                    logs.add("Row Error (" + (line.length > 1 ? line[1] : "unknown") + "): " + rowEx.getMessage());
                }
            }
            logAdminAction("BULK_REGISTER", "Registered batch of users with role " + role);
        } catch (Exception e) {
            logs.add("Critical File Error: " + e.getMessage());
        }
        return logs;
    }

    public User assignMentor(String studentUid, String mentorUid) {
        User student = userRepository.findByFirebaseUid(studentUid)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        User mentor = userRepository.findByFirebaseUid(mentorUid)
                .orElseThrow(() -> new RuntimeException("Mentor not found"));

        student.getStudentDetails().setMentor(mentor);
        // Explicitly ensure bidirectional link is healthy
        if (student.getStudentDetails().getUser() == null) {
            student.getStudentDetails().setUser(student);
        }
        User saved = userRepository.save(student);

        logAdminAction("ASSIGN_MENTOR", "Assigned mentor " + mentor.getEmail() + " to student " + student.getEmail());
        return saved;
    }

    public User registerUser(String firebaseUid, String email, String fullName, String profilePictureUrl, Role role) {
        // Enforce ADMIN for super admin
        if ("sankavi8881@gmail.com".equalsIgnoreCase(email)) {
            role = Role.ADMIN;
        }

        // Check Global Registration Setting
        if (!settingService.isRegistrationAllowed() && !role.equals(Role.ADMIN)) {
            // BUT allow if user already exists (login) or if created by admin?
            // registerUser is often called on first login after Firebase Auth.
            // If it's a new user and reg is disabled, we should block.

            // Check if user exists
            if (userRepository.findByEmail(email.toLowerCase()).isEmpty()
                    && userRepository.findByFirebaseUid(firebaseUid).isEmpty()) {
                throw new RuntimeException("Public registration is currently disabled by administrator.");
            }
        }

        Optional<User> existingByUid = userRepository.findByFirebaseUid(firebaseUid);
        if (existingByUid.isPresent()) {
            User user = existingByUid.get();
            // Force role if super admin
            if ("sankavi8881@gmail.com".equalsIgnoreCase(email)) {
                role = Role.ADMIN;
            }
            if (role != null && user.getRole() != role) {
                user.setRole(role);
                return userRepository.save(user);
            }
            return user;
        }

        Optional<User> existingByEmail = userRepository.findByEmail(email.toLowerCase());
        if (existingByEmail.isPresent()) {
            User user = existingByEmail.get();
            user.setFirebaseUid(firebaseUid);
            user.setProfilePictureUrl(profilePictureUrl);
            // Force role if super admin
            if ("sankavi8881@gmail.com".equalsIgnoreCase(email)) {
                role = Role.ADMIN;
            }
            user.setRole(role);
            if (fullName != null && !fullName.isEmpty())
                user.setFullName(fullName);
            return userRepository.save(user);
        }

        User user = new User();
        user.setFirebaseUid(firebaseUid);
        user.setEmail(email.toLowerCase());
        user.setFullName(fullName);
        user.setProfilePictureUrl(profilePictureUrl);
        // Force role if super admin
        if ("sankavi8881@gmail.com".equalsIgnoreCase(email)) {
            role = Role.ADMIN;
        }
        user.setRole(role);

        logAdminAction("REGISTER_USER", "New user registration: " + email);
        return userRepository.save(user);
    }

    @CacheEvict(value = "users", key = "#uid")
    @Transactional
    public User updateUser(String uid, User updates) {
        User user = userRepository.findByFirebaseUid(uid)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Handle StudentDetails separately to avoid OneToOne constraint violation
        if (updates.getStudentDetails() != null) {
            if (user.getStudentDetails() == null) {
                user.setStudentDetails(new com.academic.platform.model.StudentDetails());
            }

            // Explicitly exclude ID and user reference from copy
            String[] ignoredProperties = getNullPropertyNames(updates.getStudentDetails());
            List<String> ignoreList = new ArrayList<>(Arrays.asList(ignoredProperties));
            ignoreList.add("id");
            ignoreList.add("user");

            BeanUtils.copyProperties(updates.getStudentDetails(), user.getStudentDetails(),
                    ignoreList.toArray(new String[0]));

            // Ensure back-reference is set
            user.getStudentDetails().setUser(user);
        }

        // Copy other User properties, EXCLUDING studentDetails to prevent overwriting
        // the reference
        String[] ignoredUserProperties = getNullPropertyNames(updates);
        List<String> ignoreUserList = new ArrayList<>(Arrays.asList(ignoredUserProperties));
        ignoreUserList.add("studentDetails"); // Critical: Don't overwrite the nested object reference

        BeanUtils.copyProperties(updates, user, ignoreUserList.toArray(new String[0]));

        // Enforce ADMIN for super admin (prevent accidental demotion)
        if ("sankavi8881@gmail.com".equalsIgnoreCase(user.getEmail())) {
            user.setRole(Role.ADMIN);
        }

        User saved = userRepository.save(user);
        logAdminAction("UPDATE_USER", "Updated user profile: " + user.getEmail());
        return saved;
    }

    private String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<>();
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null)
                emptyNames.add(pd.getName());
        }
        // Base keys to always ignore for User
        if (source instanceof User) {
            emptyNames.add("id");
            emptyNames.add("firebaseUid");
            emptyNames.add("createdAt");
        }
        // Base keys to always ignore for StudentDetails
        if (source instanceof com.academic.platform.model.StudentDetails) {
            emptyNames.add("id");
            emptyNames.add("user");
        }

        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }

    @CacheEvict(value = "users", key = "#uid")
    @Transactional
    public void deleteUser(String uid) {
        User user = userRepository.findByFirebaseUid(uid)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userRepository.delete(user);
        logAdminAction("DELETE_USER", "Deleted user: " + user.getEmail());
    }

    public User createUser(User user, String password) throws Exception {
        // Enforce ADMIN for super admin
        if ("sankavi8881@gmail.com".equalsIgnoreCase(user.getEmail())) {
            user.setRole(Role.ADMIN);
        }

        // Enforce Password Policy
        int minPassLen = Integer.parseInt(settingService.getSetting("policy.password.minLength"));
        if (password.length() < minPassLen) {
            throw new RuntimeException("Password must be at least " + minPassLen + " characters long.");
        }

        Optional<User> existing = userRepository.findByEmail(user.getEmail().toLowerCase());
        if (existing.isPresent()) {
            throw new RuntimeException("User with this email already exists in Database");
        }

        try {
            com.google.firebase.auth.UserRecord.CreateRequest request = new com.google.firebase.auth.UserRecord.CreateRequest()
                    .setEmail(user.getEmail())
                    .setDisplayName(user.getFullName())
                    .setPassword(password)
                    .setEmailVerified(true);

            com.google.firebase.auth.UserRecord userRecord = com.google.firebase.auth.FirebaseAuth.getInstance()
                    .createUser(request);

            user.setFirebaseUid(userRecord.getUid());
            user.setEmail(user.getEmail().toLowerCase());

            User saved = userRepository.save(user);
            logAdminAction("CREATE_USER", "Created new user: " + user.getEmail() + " with role " + user.getRole());
            return saved;

        } catch (com.google.firebase.auth.FirebaseAuthException e) {
            throw new RuntimeException("Firebase Auth Error: " + e.getMessage());
        }
    }

    public User seedDummyFaculty(String department) {
        String email = "faculty." + department.toLowerCase().replaceAll("\\s+", "") + "@example.com";
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent())
            return existing.get();

        User dummy = new User();
        dummy.setEmail(email);
        dummy.setFullName("Dr. Demo Faculty (" + department + ")");
        dummy.setRole(Role.TEACHER);
        dummy.setFirebaseUid("dummy_faculty_" + System.currentTimeMillis());

        dummy.getStudentDetails().setDepartment(department);

        return userRepository.save(dummy);
    }

    /**
     * Scalability: Audit logging is now asynchronous so it never delays
     * the HTTP response thread. The @Async annotation routes it to the
     * dedicated 'auditExecutor' thread pool defined in AsyncConfig.
     */
    @Async("auditExecutor")
    public void logAdminAction(String action, String details) {
        try {
            String uid = securityUtils.getCurrentUserUid();
            if (uid != null) {
                String email = "system";
                Optional<User> actor = userRepository.findByFirebaseUid(uid);
                if (actor.isPresent()) {
                    email = actor.get().getEmail();
                }
                auditLogService.log(uid, email, action, details, "unknown-ip");
            }
        } catch (Exception e) {
            // Async context; security context may not propagate — acceptable.
        }
    }
}
