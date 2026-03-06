package com.academic.platform.controller;

import com.academic.platform.model.User;
import com.academic.platform.model.Role;
import com.academic.platform.service.UserService;
import com.academic.platform.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger logger = Logger.getLogger(UserController.class.getName());

    @Autowired
    private UserService userService;

    @Autowired
    private SecurityUtils securityUtils;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @GetMapping("/{uid}")
    public ResponseEntity<User> getUserByUid(@PathVariable String uid) {
        logger.info("Fetching user profile for UID: " + uid);
        Optional<User> user = userService.getUserByFirebaseUid(uid);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/profile")
    public ResponseEntity<User> getProfile() {
        String uid = securityUtils.getCurrentUserUid();
        logger.info("Fetching profile for authenticated user: " + uid);
        if (uid == null) {
            return ResponseEntity.status(401).build();
        }
        Optional<User> user = userService.getUserByFirebaseUid(uid);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{uid}")
    public ResponseEntity<?> updateUser(@PathVariable String uid,
            @RequestBody java.util.Map<String, Object> updatesMap) {
        try {
            // Remove empty strings to prevent deserialization errors for non-String fields
            // (like LocalDate, Double)
            updatesMap.values().removeIf(val -> val instanceof String && ((String) val).trim().isEmpty());

            // Clean numeric strings: remove commas from values like "3,00,000" before
            // mapping
            // Identify numeric fields in User class via reflection to safely clean strings
            java.util.Set<String> numericFields = new java.util.HashSet<>();
            for (java.lang.reflect.Field field : User.class.getDeclaredFields()) {
                if (Number.class.isAssignableFrom(field.getType()) ||
                        field.getType() == double.class || field.getType() == int.class ||
                        field.getType() == long.class) {
                    numericFields.add(field.getName());
                }
            }

            for (java.util.Map.Entry<String, Object> entry : updatesMap.entrySet()) {
                // If it's a numeric field but value is a String, remove commas (e.g.
                // "3,00,000")
                if (entry.getValue() instanceof String && numericFields.contains(entry.getKey())) {
                    String val = (String) entry.getValue();
                    entry.setValue(val.replace(",", ""));
                }
            }

            // Remove relationship/complex fields that shouldn't be updated here or cause
            // serialization issues
            updatesMap.remove("mentor");
            updatesMap.remove("mentees");
            updatesMap.remove("authorities");
            updatesMap.remove("role"); // Role handled separately or carefully
            updatesMap.remove("id");
            updatesMap.remove("firebaseUid");

            // Convert map to User entity using a lenient mapper copy
            com.fasterxml.jackson.databind.ObjectMapper mapperCopy = objectMapper.copy();
            mapperCopy.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    false);

            User updates = mapperCopy.convertValue(updatesMap, User.class);

            if (updates.getStudentDetails() == null) {
                updates.setStudentDetails(new com.academic.platform.model.StudentDetails());
            }
            com.academic.platform.model.StudentDetails details = updates.getStudentDetails();

            if (updatesMap.containsKey("rollNumber")) {
                details.setRollNumber(updatesMap.get("rollNumber") != null ? String.valueOf(updatesMap.get("rollNumber")) : null);
            }
            if (updatesMap.containsKey("department")) {
                details.setDepartment(updatesMap.get("department") != null ? String.valueOf(updatesMap.get("department")) : null);
            }
            if (updatesMap.containsKey("semester")) {
                Object sem = updatesMap.get("semester");
                if (sem != null) {
                    try {
                        details.setSemester(Integer.parseInt(String.valueOf(sem)));
                    } catch (NumberFormatException ignored) {}
                } else {
                    details.setSemester(null);
                }
            }
            if (updatesMap.containsKey("studentStatus")) {
                details.setStudentStatus(updatesMap.get("studentStatus") != null ? String.valueOf(updatesMap.get("studentStatus")) : null);
            }

            return ResponseEntity.ok(userService.updateUser(uid, updates));
        } catch (IllegalArgumentException e) {
            String msg = "Error updating user " + uid + " - Invalid Argument: " + e.getMessage();
            logger.severe(msg);
            return ResponseEntity.badRequest().body(null); // Or msg if response type allows, but return type is User.
                                                           // Usually best to return badRequest with body string, but
                                                           // generic type mismatch.
        } catch (Exception e) {
            e.printStackTrace();
            logger.severe("Error updating user " + uid + ": " + e.getMessage());
            // Using raw ResponseEntity to return String error message despite method
            // signature saying User
            return new ResponseEntity("Error: " + e.getMessage(), org.springframework.http.HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{uid}")
    public ResponseEntity<Void> deleteUser(@PathVariable String uid) {
        try {
            userService.deleteUser(uid);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<User>> getUsersByRole(@PathVariable Role role) {
        logger.info("Fetching all users with role: " + role);
        return ResponseEntity.ok(userService.getUsersByRole(role));
    }

    @GetMapping("/faculty")
    public ResponseEntity<List<User>> getAllFaculty() {
        return ResponseEntity.ok(userService.getPotentialFaculty());
    }

    @GetMapping("/mentees/{mentorUid}")
    public ResponseEntity<List<User>> getMentees(@PathVariable String mentorUid) {
        return ResponseEntity.ok(userService.getMenteesByMentor(mentorUid));
    }

    @GetMapping("/faculty/department")
    public ResponseEntity<List<User>> getFacultyByDepartment(@RequestParam String department) {
        return ResponseEntity.ok(userService.getFacultyByDepartment(department));
    }

    @GetMapping("/students/department")
    public ResponseEntity<List<User>> getStudentsByDepartment(@RequestParam String department) {
        return ResponseEntity.ok(userService.getStudentsByDepartment(department));
    }

    @PostMapping("/assign-mentor")
    public ResponseEntity<?> assignMentor(@RequestBody MentorshipRequest request) {
        try {
            User updated = userService.assignMentor(request.getStudentUid(), request.getMentorUid());
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/bulk-assign-mentor")
    public ResponseEntity<List<String>> bulkAssignMentors(@RequestParam("file") MultipartFile file) {
        try {
            List<String> logs = userService.bulkAssignMentors(file.getInputStream());
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/bulk-register")
    public ResponseEntity<List<String>> bulkRegister(@RequestParam("file") MultipartFile file,
            @RequestParam("role") Role role) {
        try {
            List<String> logs = userService.bulkRegisterUsers(file.getInputStream(), role);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/bulk-upload-mentees/{mentorUid}")
    public ResponseEntity<List<String>> bulkUploadMentees(@PathVariable String mentorUid,
            @RequestParam("file") MultipartFile file) {
        try {
            List<String> logs = userService.bulkUploadMenteesForMentor(file.getInputStream(), mentorUid);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of("Error: " + e.getMessage()));
        }
    }

    @PostMapping("/dev/seed-faculty")
    public ResponseEntity<User> seedFaculty(@RequestParam String department) {
        return ResponseEntity.ok(userService.seedDummyFaculty(department));
    }

    @Autowired
    private com.academic.platform.service.SystemSettingService systemSettingService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody UserRegistrationRequest request) {
        logger.info("Attempting to register user: " + request.getEmail());

        if (!systemSettingService.isRegistrationAllowed()) {
            return ResponseEntity.status(403).body("Registration is currently disabled by administrator.");
        }

        try {
            String currentUid = securityUtils.getCurrentUserUid();

            if (currentUid == null || "anonymousUser".equals(currentUid)) {
                logger.warning("Registration rejected: No valid Firebase token found.");
                return ResponseEntity.status(401).body("Unauthorized: Missing or invalid Firebase ID token");
            }

            if (!currentUid.equals(request.getFirebaseUid())) {
                return ResponseEntity.status(403).body("Unauthorized: UID mismatch");
            }

            String parsedRole = request.getRole() != null ? request.getRole().toUpperCase().trim() : "STUDENT";

            User user = userService.registerUser(
                    request.getFirebaseUid(),
                    request.getEmail(),
                    request.getFullName(),
                    request.getProfilePictureUrl(),
                    Role.valueOf(parsedRole));
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            logger.severe("Registration error: " + e.getMessage());
            return ResponseEntity.status(500).body("Registration failed: " + e.getMessage());
        }
    }

    @PostMapping("/create-user")
    public ResponseEntity<?> createUser(@RequestBody java.util.Map<String, Object> requestBody) {
        try {
            // Extract password separately as it's not part of User entity
            String password = (String) requestBody.get("password");
            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("Password is required");
            }

            // Remove password from map to avoid mapping errors
            requestBody.remove("password");

            // Convert map to User entity
            // Using convertValue handles type conversions including Enums if names match
            User user = objectMapper.convertValue(requestBody, User.class);

            // Explicitly map StudentDetails fields if they are in the top-level map
            // This ensures robust handling even if @JsonUnwrapped deserialization via
            // convertValue is tricky
            if (user.getStudentDetails() == null) {
                user.setStudentDetails(new com.academic.platform.model.StudentDetails());
            }
            com.academic.platform.model.StudentDetails details = user.getStudentDetails();

            if (requestBody.containsKey("department")) {
                details.setDepartment((String) requestBody.get("department"));
            }
            if (requestBody.containsKey("rollNumber")) {
                details.setRollNumber((String) requestBody.get("rollNumber"));
            }
            if (requestBody.containsKey("section")) {
                details.setSection((String) requestBody.get("section"));
            }
            if (requestBody.containsKey("semester")) {
                Object sem = requestBody.get("semester");
                if (sem instanceof Integer) {
                    details.setSemester((Integer) sem);
                } else if (sem instanceof String) {
                    try {
                        details.setSemester(Integer.parseInt(((String) sem).trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            // Explicitly handle Role string if needed, although Jackson usually handles
            // String->Enum if matches
            if (requestBody.containsKey("role") && user.getRole() == null) {
                try {
                    user.setRole(Role.valueOf(((String) requestBody.get("role")).trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body("Invalid Role: " + requestBody.get("role"));
                }
            }

            return ResponseEntity.ok(userService.createUser(user, password));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error creating user: " + e.getMessage());
        }
    }

    public static class CreateUserRequest {
        private String email;
        private String fullName;
        private String role;
        private String department;
        private String rollNumber;
        private String password;
        private String mobileNumber;
        private String address;
        private Integer semester;
        private String section;

        // Getters and Setters
        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getDepartment() {
            return department;
        }

        public void setDepartment(String department) {
            this.department = department;
        }

        public String getRollNumber() {
            return rollNumber;
        }

        public void setRollNumber(String rollNumber) {
            this.rollNumber = rollNumber;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getMobileNumber() {
            return mobileNumber;
        }

        public void setMobileNumber(String mobileNumber) {
            this.mobileNumber = mobileNumber;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public Integer getSemester() {
            return semester;
        }

        public void setSemester(Integer semester) {
            this.semester = semester;
        }

        public String getSection() {
            return section;
        }

        public void setSection(String section) {
            this.section = section;
        }
    }

    public static class MessageResponse {
        private String message;

        public MessageResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public static class UserRegistrationRequest {
        private String firebaseUid;
        private String email;
        private String fullName;
        private String profilePictureUrl;
        private String role;

        public String getFirebaseUid() {
            return firebaseUid;
        }

        public void setFirebaseUid(String firebaseUid) {
            this.firebaseUid = firebaseUid;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getProfilePictureUrl() {
            return profilePictureUrl;
        }

        public void setProfilePictureUrl(String profilePictureUrl) {
            this.profilePictureUrl = profilePictureUrl;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    public static class MentorshipRequest {
        private String studentUid;
        private String mentorUid;

        public String getStudentUid() {
            return studentUid;
        }

        public void setStudentUid(String studentUid) {
            this.studentUid = studentUid;
        }

        public String getMentorUid() {
            return mentorUid;
        }

        public void setMentorUid(String mentorUid) {
            this.mentorUid = mentorUid;
        }
    }
}
