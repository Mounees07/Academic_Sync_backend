package com.academic.platform.repository;

import com.academic.platform.model.FeeTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeeTransactionRepository extends JpaRepository<FeeTransaction, Long> {

    // ✅ Use firebaseUid — the actual field name on User entity
    List<FeeTransaction> findByStudentFirebaseUidOrderByCreatedAtDesc(String firebaseUid);

    Optional<FeeTransaction> findByTransactionId(String transactionId);

    Optional<FeeTransaction> findByRazorpayOrderId(String razorpayOrderId);
}
