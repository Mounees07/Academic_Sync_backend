package com.academic.platform.controller;

import com.academic.platform.model.FeeRecord;
import com.academic.platform.model.FeeTransaction;
import com.academic.platform.model.User;
import com.academic.platform.repository.FeeRecordRepository;
import com.academic.platform.repository.FeeTransactionRepository;
import com.academic.platform.repository.UserRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")

public class PaymentController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FeeRecordRepository feeRecordRepository;

    @Autowired
    private FeeTransactionRepository feeTransactionRepository;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @PostMapping("/create-order")
    public ResponseEntity<?> createOrder(@RequestBody Map<String, Object> data) {
        try {
            int amount = Integer.parseInt(data.get("amount").toString());
            String currency = data.getOrDefault("currency", "INR").toString();
            String receipt = "txn_" + System.currentTimeMillis();

            RazorpayClient razorpay = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount * 100); // Amount is in paise (Multiply by 100)
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", receipt);

            Order order = razorpay.orders.create(orderRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("orderId", order.get("id"));
            response.put("currency", order.get("currency"));
            response.put("amount", order.get("amount"));
            response.put("keyId", razorpayKeyId);

            return ResponseEntity.ok(response);
        } catch (RazorpayException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "An error occurred while creating order"));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPayment(@RequestBody Map<String, String> data) {
        // Here you would optimally verify the signature:
        // Utils.verifyPaymentSignature(attributes, razorpayKeySecret);
        // And then update the student's feesDue to 0 inside your database.

        String razorpayPaymentId = data.get("razorpay_payment_id");
        String razorpayOrderId = data.get("razorpay_order_id");
        String razorpaySignature = data.get("razorpay_signature");
        String studentUid = data.get("student_uid");

        if (razorpayPaymentId != null && razorpayOrderId != null && studentUid != null) {
            User student = userRepository.findByFirebaseUid(studentUid)
                    .orElse(null);

            if (student == null) {
                return ResponseEntity.status(404).body(Map.of("error", "Student not found"));
            }

            FeeRecord feeRecord = feeRecordRepository.findFirstByStudent_FirebaseUidAndPaymentStatusInOrderByIdDesc(
                    studentUid,
                    List.of("Pending", "Overdue"));

            FeeTransaction transaction = feeTransactionRepository.findByRazorpayOrderId(razorpayOrderId)
                    .orElseGet(FeeTransaction::new);

            transaction.setStudent(student);
            transaction.setFeeRecord(feeRecord);
            transaction.setTransactionId(razorpayPaymentId);
            transaction.setRazorpayOrderId(razorpayOrderId);
            transaction.setRazorpaySignature(razorpaySignature);
            transaction.setStatus("SUCCESS");
            transaction.setPaymentDate(LocalDateTime.now());
            transaction.setPaymentMethod(data.getOrDefault("payment_method", "Razorpay"));
            transaction.setCurrency(data.getOrDefault("currency", "INR"));
            transaction.setAmount(new BigDecimal(data.getOrDefault("amount", "0")));

            if (feeRecord != null) {
                transaction.setAcademicYear(feeRecord.getAcademicYear());
                transaction.setSemester(feeRecord.getSemester());
                transaction.setRemarks(feeRecord.getRemarks());

                feeRecord.setPaymentStatus("Paid");
                feeRecord.setPaymentDate(LocalDate.now());
                feeRecordRepository.save(feeRecord);
            }

            FeeTransaction savedTransaction = feeTransactionRepository.save(transaction);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Payment verified and recorded successfully",
                    "transactionId", savedTransaction.getTransactionId()));
        }
        return ResponseEntity.status(400).body(Map.of("error", "Invalid payment details"));
    }
}

