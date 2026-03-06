package com.academic.platform.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "student_details", indexes = {
        // Scalability: Department is filtered in nearly every list query
        @Index(name = "idx_sd_department", columnList = "department"),
        @Index(name = "idx_sd_semester", columnList = "semester"),
        @Index(name = "idx_sd_mentor", columnList = "mentor_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    @EqualsAndHashCode.Exclude
    private User user;

    @Column(length = 20)
    private String rollNumber;
    @Column(length = 50)
    private String department;
    private Integer semester;
    @Column(length = 10)
    private String section;
    private Double gpa;
    private Double sgpa;
    private Double cgpa;
    private Integer arrearCount;
    private Double feesDue;
    private Double attendance;

    @Column(length = 100)
    private String fatherName;
    @Column(length = 100)
    private String motherName;
    @Column(length = 20)
    private String parentContact;

    @Column(columnDefinition = "TEXT")
    private String address;
    @Column(columnDefinition = "TEXT")
    private String permanentAddress;

    @Column(length = 20)
    private String admissionYear;
    @Column(length = 20)
    private String batch;

    // Personal Details Extended
    @Column(length = 50)
    private String enrollmentNo;
    @Column(length = 50)
    private String registerNo;
    @Column(length = 50)
    private String dteUmisRegNo;
    @Column(length = 50)
    private String applicationNo;
    @Column(length = 50)
    private String admissionNo;
    @Column(length = 50)
    private String community;
    @Column(length = 100)
    private String guardianName;
    @Column(length = 50)
    private String religion;
    @Column(length = 50)
    private String nationality;
    @Column(length = 50)
    private String motherTongue;
    @Column(length = 10)
    private String bloodGroup;
    @Column(length = 20)
    private String aadharNo;

    // Parent Occupation
    @Column(length = 100)
    private String parentOccupation;
    @Column(length = 100)
    private String parentPlaceOfWork;
    @Column(length = 100)
    private String parentDesignation;
    private Double parentIncome;
    @Column(length = 100)
    private String parentEmailId;

    // Academic Details Extended
    @Column(length = 20)
    private String branchCode;
    @Column(length = 50)
    private String degreeLevel;
    @Column(length = 20)
    private String courseCode;
    @Column(length = 100)
    private String courseName;
    @Column(length = 100)
    private String branchName;
    @Column(length = 50)
    private String branchType;
    @Column(length = 50)
    private String regulation;
    @Column(length = 100)
    private String university;
    @Column(length = 50)
    private String studentCategory;
    @Column(length = 50)
    private String seatCategory;
    @Column(length = 50)
    private String quota;
    @Column(length = 50)
    private String studentStatus;
    @Column(length = 20)
    private String yearOfCompletion;
    @Column(length = 20)
    private String currentYear;

    // Admission Payment Details
    @Column(length = 50)
    private String dteRegisterNo;
    @Column(length = 50)
    private String dteAdmissionNo;
    @Column(length = 50)
    private String dteGeneralRank;
    @Column(length = 50)
    private String dteCommunityRank;
    @Column(length = 20)
    private String entranceMarksMin;
    @Column(length = 20)
    private String entranceMarksMax;
    @Column(length = 50)
    private String entranceRegisterNo;

    // Insurance Details
    @Column(length = 100)
    private String nomineeName;
    @Column(length = 10)
    private String nomineeAge;
    @Column(length = 50)
    private String nomineeRelationship;

    // Official Info
    @Column(length = 100)
    private String officialEmailId;

    // Hostel Details
    @Column(length = 50)
    private String hostellerDayScholar;
    @Column(length = 100)
    private String hostelName;
    @Column(length = 50)
    private String hostelRoomType;
    @Column(length = 100)
    private String wardenName;
    @Column(length = 20)
    private String hostelDiscontinuedDate;
    @Column(length = 100)
    private String classAdvisorName;
    @Column(length = 20)
    private String hostelRoomCapacity;
    @Column(length = 20)
    private String hostelFloorNo;
    @Column(length = 20)
    private String hostelRoomNo;
    @Column(length = 100)
    private String wardenAlter;

    @Column(columnDefinition = "TEXT")
    private String hostelNote;

    // School Details
    @Column(length = 100)
    private String schoolQualification;
    @Column(length = 100)
    private String schoolStudyState;
    @Column(length = 20)
    private String schoolYearOfPass;
    @Column(length = 20)
    private String schoolNoOfAttempts;
    @Column(length = 50)
    private String schoolClassification;

    // School Marks
    @Column(length = 20)
    private String schoolMarkMinPhysics;
    @Column(length = 20)
    private String schoolMarkMaxPhysics;
    @Column(length = 20)
    private String schoolMarkPctPhysics;

    @Column(length = 20)
    private String schoolMarkMinChemistry;
    @Column(length = 20)
    private String schoolMarkMaxChemistry;
    @Column(length = 20)
    private String schoolMarkPctChemistry;

    @Column(length = 20)
    private String schoolMarkMinMathematics;
    @Column(length = 20)
    private String schoolMarkMaxMathematics;
    @Column(length = 20)
    private String schoolMarkPctMathematics;

    @Column(length = 20)
    private String schoolMarkMinPCM;
    @Column(length = 20)
    private String schoolMarkMaxPCM;
    @Column(length = 20)
    private String schoolMarkPctPCM;

    @Column(length = 20)
    private String schoolMarkMinComputerScience;
    @Column(length = 20)
    private String schoolMarkMaxComputerScience;
    @Column(length = 20)
    private String schoolMarkPctComputerScience;

    @Column(length = 20)
    private String schoolMarkMinBiology;
    @Column(length = 20)
    private String schoolMarkMaxBiology;
    @Column(length = 20)
    private String schoolMarkPctBiology;

    @Column(length = 20)
    private String schoolCutOff200;

    // School Certificates
    @Column(length = 50)
    private String schoolRegNo1;
    @Column(length = 50)
    private String schoolRegNo2;
    @Column(length = 50)
    private String schoolRegNo3;
    @Column(length = 50)
    private String schoolRegNo4;

    @Column(length = 50)
    private String schoolCertNo1;
    @Column(length = 50)
    private String schoolCertNo2;
    @Column(length = 50)
    private String schoolCertNo3;
    @Column(length = 50)
    private String schoolCertNo4;

    @Column(length = 20)
    private String schoolTotalMarks1;
    @Column(length = 20)
    private String schoolTotalMarks2;
    @Column(length = 20)
    private String schoolTotalMarks3;
    @Column(length = 20)
    private String schoolTotalMarks4;

    // School TC
    @Column(length = 100)
    private String schoolName;
    @Column(length = 100)
    private String schoolTCName;
    @Column(length = 50)
    private String schoolTCNo;
    @Column(length = 20)
    private String schoolTCDate;
    @Column(length = 50)
    private String schoolTCClass;
    @Column(length = 100)
    private String boardOfSchool;
    @Column(length = 20)
    private String schoolCutOff300;

    @Column(columnDefinition = "TEXT")
    private String marksNote1;
    @Column(columnDefinition = "TEXT")
    private String marksNote2;

    // Institute (BIT) TC Details
    @Column(length = 20)
    private String tcLastClassDate;
    @Column(length = 50)
    private String tcPromotion;

    @Column(columnDefinition = "TEXT")
    private String tcReasonLeaving;
    @Column(columnDefinition = "TEXT")
    private String tcConduct;

    @Column(length = 50)
    private String bitTCNo;
    @Column(length = 20)
    private String bitTCDate;
    @Column(length = 50)
    private String duplicateTCIssued;

    @Column(columnDefinition = "TEXT")
    private String duplicateTCDescription;

    // Final Institute Marks
    @Column(length = 20)
    private String finalTotalMarksMin;
    @Column(length = 20)
    private String finalTotalMarksMax;
    @Column(length = 20)
    private String finalTotalMarksPct;
    @Column(length = 50)
    private String finalClassification;
    @Column(length = 20)
    private String finalYearOfPass;
    @Column(length = 20)
    private String universityRank;
    @Column(length = 20)
    private String universityRank1;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "mentor_id")
    @EqualsAndHashCode.Exclude
    private User mentor;
}
