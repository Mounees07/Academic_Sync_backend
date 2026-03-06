package com.academic.platform.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "users", indexes = {
        // Scalability: Index on firebaseUid — used on every single authenticated
        // request
        @Index(name = "idx_users_firebase_uid", columnList = "firebaseUid"),
        // Scalability: Index on role — used by every role-filtered query (students,
        // teachers, etc.)
        @Index(name = "idx_users_role", columnList = "role"),
        // Scalability: Index on email — used for login and bulk-register lookups
        @Index(name = "idx_users_email", columnList = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String firebaseUid;

    @Column(unique = true, nullable = false)
    private String email;

    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Role role;

    private String profilePictureUrl;
    private String mobileNumber;
    private java.time.LocalDate dob;
    private String gender;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonUnwrapped
    @JsonIgnoreProperties("user")
    @EqualsAndHashCode.Exclude
    private StudentDetails studentDetails = new StudentDetails();

    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        ensureStudentDetailsLink();
    }

    @PreUpdate
    protected void onUpdate() {
        ensureStudentDetailsLink();
    }

    private void ensureStudentDetailsLink() {
        if (studentDetails != null && studentDetails.getUser() == null) {
            studentDetails.setUser(this);
        }
    }

    // Helper to ensure student details are linked correctly
    public void setStudentDetails(StudentDetails details) {
        this.studentDetails = details;
        if (details != null) {
            details.setUser(this);
        }
    }
}
