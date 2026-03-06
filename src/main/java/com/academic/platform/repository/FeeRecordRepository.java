package com.academic.platform.repository;

import com.academic.platform.model.FeeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeRecordRepository extends JpaRepository<FeeRecord, Long> {

    List<FeeRecord> findAllByOrderByIdDesc();

    List<FeeRecord> findByStudent_FirebaseUid(String firebaseUid);

    List<FeeRecord> findByPaymentStatus(String paymentStatus);

    List<FeeRecord> findByAcademicYear(String academicYear);

    /**
     * Monthly totals for chart — returns [month(1-12), totalCollected]
     * for fee records that are Paid, grouped by payment month.
     */
    @Query("SELECT MONTH(f.paymentDate), SUM(f.totalAmount) " +
            "FROM FeeRecord f WHERE f.paymentStatus = 'Paid' AND f.paymentDate IS NOT NULL " +
            "GROUP BY MONTH(f.paymentDate) ORDER BY MONTH(f.paymentDate)")
    List<Object[]> getMonthlyCollectionTotals();
}
