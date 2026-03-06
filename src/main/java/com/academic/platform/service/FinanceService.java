package com.academic.platform.service;

import com.academic.platform.model.CollegeExpense;
import com.academic.platform.model.FeeRecord;
import com.academic.platform.model.User;
import com.academic.platform.repository.CollegeExpenseRepository;
import com.academic.platform.repository.FeeRecordRepository;
import com.academic.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class FinanceService {

    @Autowired
    private FeeRecordRepository feeRecordRepository;

    @Autowired
    private CollegeExpenseRepository collegeExpenseRepository;

    @Autowired
    private UserRepository userRepository;

    // ──────────────── FEE RECORDS ────────────────

    public List<FeeRecord> getAllFeeRecords() {
        return feeRecordRepository.findAllByOrderByIdDesc();
    }

    public Optional<FeeRecord> getFeeRecordById(Long id) {
        return feeRecordRepository.findById(id);
    }

    public List<FeeRecord> getFeesByStudent(String firebaseUid) {
        return feeRecordRepository.findByStudent_FirebaseUid(firebaseUid);
    }

    public FeeRecord createFeeRecord(Map<String, Object> data) {
        FeeRecord record = new FeeRecord();
        applyFeeData(record, data);
        return feeRecordRepository.save(record);
    }

    public FeeRecord updateFeeRecord(Long id, Map<String, Object> data) {
        FeeRecord record = feeRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fee record not found: " + id));
        applyFeeData(record, data);
        return feeRecordRepository.save(record);
    }

    public void deleteFeeRecord(Long id) {
        feeRecordRepository.deleteById(id);
    }

    private void applyFeeData(FeeRecord record, Map<String, Object> data) {
        if (data.containsKey("studentUid")) {
            String uid = (String) data.get("studentUid");
            User student = userRepository.findByFirebaseUid(uid)
                    .orElseThrow(() -> new RuntimeException("Student not found: " + uid));
            record.setStudent(student);
        }
        if (data.containsKey("academicYear"))
            record.setAcademicYear((String) data.get("academicYear"));
        if (data.containsKey("semester"))
            record.setSemester(toInt(data.get("semester")));
        if (data.containsKey("tuitionFee"))
            record.setTuitionFee(toDouble(data.get("tuitionFee")));
        if (data.containsKey("activitiesFee"))
            record.setActivitiesFee(toDouble(data.get("activitiesFee")));
        if (data.containsKey("miscellaneous"))
            record.setMiscellaneous(toDouble(data.get("miscellaneous")));
        if (data.containsKey("paymentStatus"))
            record.setPaymentStatus((String) data.get("paymentStatus"));
        if (data.containsKey("remarks"))
            record.setRemarks((String) data.get("remarks"));
        if (data.containsKey("paymentDate")) {
            Object pd = data.get("paymentDate");
            if (pd != null && !pd.toString().isBlank()) {
                try {
                    record.setPaymentDate(LocalDate.parse(pd.toString()));
                } catch (Exception ignored) {
                }
            } else {
                record.setPaymentDate(null);
            }
        }
        // Auto-compute total via @PrePersist/@PreUpdate
    }

    // ──────────────── MONTHLY CHART DATA ────────────────

    /** Returns list of {month, totalCollected} for the chart */
    public List<Map<String, Object>> getMonthlyCollectionData() {
        String[] MONTHS = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };
        Map<Integer, Double> monthMap = new LinkedHashMap<>();
        for (int i = 1; i <= 12; i++)
            monthMap.put(i, 0.0);

        List<Object[]> rows = feeRecordRepository.getMonthlyCollectionTotals();
        for (Object[] row : rows) {
            int month = ((Number) row[0]).intValue();
            double total = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            monthMap.put(month, total);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Integer, Double> e : monthMap.entrySet()) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("month", MONTHS[e.getKey() - 1]);
            point.put("amount", e.getValue());
            result.add(point);
        }
        return result;
    }

    // ──────────────── COLLEGE EXPENSES ────────────────

    public List<CollegeExpense> getAllExpenses() {
        return collegeExpenseRepository.findAllByOrderByExpenseDateDesc();
    }

    public Optional<CollegeExpense> getExpenseById(Long id) {
        return collegeExpenseRepository.findById(id);
    }

    public CollegeExpense createExpense(CollegeExpense expense) {
        return collegeExpenseRepository.save(expense);
    }

    public CollegeExpense updateExpense(Long id, CollegeExpense updated) {
        CollegeExpense existing = collegeExpenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found: " + id));
        if (updated.getCategory() != null)
            existing.setCategory(updated.getCategory());
        if (updated.getDescription() != null)
            existing.setDescription(updated.getDescription());
        if (updated.getAmount() != null)
            existing.setAmount(updated.getAmount());
        if (updated.getExpenseDate() != null)
            existing.setExpenseDate(updated.getExpenseDate());
        if (updated.getExpenseType() != null)
            existing.setExpenseType(updated.getExpenseType());
        if (updated.getAcademicYear() != null)
            existing.setAcademicYear(updated.getAcademicYear());
        if (updated.getRecordedBy() != null)
            existing.setRecordedBy(updated.getRecordedBy());
        return collegeExpenseRepository.save(existing);
    }

    public void deleteExpense(Long id) {
        collegeExpenseRepository.deleteById(id);
    }

    // ──────────────── HELPERS ────────────────

    private Double toDouble(Object val) {
        if (val == null)
            return null;
        try {
            return Double.parseDouble(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer toInt(Object val) {
        if (val == null)
            return null;
        try {
            return Integer.parseInt(val.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
