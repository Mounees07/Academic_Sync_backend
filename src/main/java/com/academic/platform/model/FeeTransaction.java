package com.academic.platform.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fee_transactions", indexes = {
        @Index(name = "idx_feetx_student", columnList = "student_id"),
        @Index(name = "idx_feetx_status", columnList = "status"),
        @Index(name = "idx_feetx_txid", columnList = "transaction_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_record_id")
    private FeeRecord feeRecord;

    /** Razorpay payment ID or generated transaction ID */
    @Column(unique = true, nullable = false, length = 100)
    private String transactionId;

    @Column(length = 100)
    private String razorpayOrderId;

    @Column(length = 100)
    private String razorpaySignature;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(length = 10)
    private String currency = "INR";

    private LocalDateTime paymentDate;

    /** SUCCESS | FAILED | PENDING | REFUNDED */
    @Builder.Default
    @Column(length = 20)
    private String status = "PENDING";

    @Column(length = 50)
    private String paymentMethod;

    @Column(columnDefinition = "TEXT")
    private String receiptUrl;

    @Column(length = 50)
    private String academicYear;

    private Integer semester;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (currency == null) currency = "INR";
        if (status == null) status = "PENDING";
    }
}
