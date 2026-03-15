package com.academic.platform.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "department_resources", indexes = {
    @Index(name = "idx_dr_department", columnList = "department"),
    @Index(name = "idx_dr_type", columnList = "resourceType")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Department this resource belongs to (e.g., "Computer Engineering")
    @Column(nullable = false)
    private String department;

    // "LAB" or "CLASSROOM"
    @Column(nullable = false, length = 20)
    private String resourceType;

    @Column(nullable = false)
    private String name;     // e.g., "CS Lab 1", "Classroom 301"

    private String block;    // e.g., "Block A"

    private Integer capacity; // Seating/system capacity

    // Number of systems/computers inside (for labs)
    private Integer systemCount;

    // Number of systems currently under maintenance
    private Integer systemsUnderMaintenance;

    // Current occupancy % (manually entered or computed)
    private Double occupancyPercent;

    // Resource status: ACTIVE, MAINTENANCE, OFFLINE
    @Column(length = 20)
    private String status;

    // Notes / reason for maintenance
    @Column(columnDefinition = "TEXT")
    private String notes;

    // Expected return date if under maintenance (stored as string for simplicity)
    private String expectedReturnDate;

    @Column(insertable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = java.time.LocalDateTime.now();
        if (status == null) status = "ACTIVE";
        if (systemCount == null) systemCount = 0;
        if (systemsUnderMaintenance == null) systemsUnderMaintenance = 0;
        if (occupancyPercent == null) occupancyPercent = 0.0;
    }
}
