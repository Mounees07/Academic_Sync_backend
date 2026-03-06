package com.academic.platform.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "visitors")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Visitor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String contactNumber;
    private String purpose;
    private String personToMeet; // Name of student or staff
    private String vehicleNumber;

    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;

    @Builder.Default
    private String status = "CHECKED_IN"; // CHECKED_IN, CHECKED_OUT

    @PrePersist
    protected void onCreate() {
        if (checkInTime == null) {
            checkInTime = LocalDateTime.now();
        }
    }
}
