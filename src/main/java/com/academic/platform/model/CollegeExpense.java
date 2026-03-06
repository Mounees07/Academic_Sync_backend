package com.academic.platform.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "college_expenses", indexes = {
        @Index(name = "idx_exp_date", columnList = "expenseDate"),
        @Index(name = "idx_exp_type", columnList = "expenseType")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollegeExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** e.g. "Staff Salaries", "Infrastructure", "Utilities" */
    @Column(length = 100, nullable = false)
    private String category;

    /** Short description */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** Amount in currency units */
    private Double amount;

    /** The date the expense was incurred */
    private LocalDate expenseDate;

    /**
     * Type tag: Salaries, Infrastructure, Utilities, Equipment, Events, Library,
     * Other
     */
    @Column(length = 50)
    private String expenseType;

    /** Academic year for grouping, e.g. "2025-26" */
    @Column(length = 20)
    private String academicYear;

    /** Optional: who approved / recorded this */
    @Column(length = 100)
    private String recordedBy;
}
