package com.academic.platform.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "fee_records", indexes = {
        @Index(name = "idx_fee_student", columnList = "student_id"),
        @Index(name = "idx_fee_status", columnList = "paymentStatus"),
        @Index(name = "idx_fee_payment_date", columnList = "paymentDate")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The student this fee belongs to */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /** Academic year e.g. "2025-26" */
    @Column(length = 20)
    private String academicYear;

    /** Semester number */
    private Integer semester;

    /** Breakdown */
    private Double tuitionFee;
    private Double activitiesFee;
    private Double miscellaneous;

    /** Total = tuition + activities + misc (stored for quick queries) */
    private Double totalAmount;

    /** Paid / Pending / Overdue */
    @Column(length = 20)
    private String paymentStatus;

    /** Date when the payment was made (null if not paid) */
    private LocalDate paymentDate;

    /** Optional remarks */
    @Column(columnDefinition = "TEXT")
    private String remarks;

    @PrePersist
    @PreUpdate
    public void computeTotal() {
        double t = tuitionFee != null ? tuitionFee : 0.0;
        double a = activitiesFee != null ? activitiesFee : 0.0;
        double m = miscellaneous != null ? miscellaneous : 0.0;
        this.totalAmount = t + a + m;
    }
}
