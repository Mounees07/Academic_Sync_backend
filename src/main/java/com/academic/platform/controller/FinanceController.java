package com.academic.platform.controller;

import com.academic.platform.model.CollegeExpense;
import com.academic.platform.model.FeeRecord;
import com.academic.platform.service.FinanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/finance")
@CrossOrigin(origins = { "http://localhost:5173", "http://10.10.188.128:5173" }, allowCredentials = "true")
public class FinanceController {

    @Autowired
    private FinanceService financeService;

    // ──────────────────── FEE RECORDS ────────────────────

    /** GET /api/finance/fees — all fee records */
    @GetMapping("/fees")
    public ResponseEntity<List<FeeRecord>> getAllFees() {
        return ResponseEntity.ok(financeService.getAllFeeRecords());
    }

    /** GET /api/finance/fees/{id} — single record */
    @GetMapping("/fees/{id}")
    public ResponseEntity<?> getFeeById(@PathVariable Long id) {
        return financeService.getFeeRecordById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/finance/fees/student/{uid} — fees for one student */
    @GetMapping("/fees/student/{uid}")
    public ResponseEntity<List<FeeRecord>> getFeesByStudent(@PathVariable String uid) {
        return ResponseEntity.ok(financeService.getFeesByStudent(uid));
    }

    /** POST /api/finance/fees — create fee record */
    @PostMapping("/fees")
    public ResponseEntity<?> createFee(@RequestBody Map<String, Object> data) {
        try {
            FeeRecord created = financeService.createFeeRecord(data);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /** PUT /api/finance/fees/{id} — update fee record */
    @PutMapping("/fees/{id}")
    public ResponseEntity<?> updateFee(@PathVariable Long id, @RequestBody Map<String, Object> data) {
        try {
            FeeRecord updated = financeService.updateFeeRecord(id, data);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /** DELETE /api/finance/fees/{id} — delete fee record */
    @DeleteMapping("/fees/{id}")
    public ResponseEntity<Void> deleteFee(@PathVariable Long id) {
        try {
            financeService.deleteFeeRecord(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ──────────────────── CHART DATA ────────────────────

    /** GET /api/finance/monthly-chart — 12-month collection totals */
    @GetMapping("/monthly-chart")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyChart() {
        return ResponseEntity.ok(financeService.getMonthlyCollectionData());
    }

    // ──────────────────── COLLEGE EXPENSES ────────────────────

    /** GET /api/finance/expenses — all college expenses */
    @GetMapping("/expenses")
    public ResponseEntity<List<CollegeExpense>> getAllExpenses() {
        return ResponseEntity.ok(financeService.getAllExpenses());
    }

    /** GET /api/finance/expenses/{id} */
    @GetMapping("/expenses/{id}")
    public ResponseEntity<?> getExpenseById(@PathVariable Long id) {
        return financeService.getExpenseById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** POST /api/finance/expenses */
    @PostMapping("/expenses")
    public ResponseEntity<?> createExpense(@RequestBody CollegeExpense expense) {
        try {
            return ResponseEntity.ok(financeService.createExpense(expense));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /** PUT /api/finance/expenses/{id} */
    @PutMapping("/expenses/{id}")
    public ResponseEntity<?> updateExpense(@PathVariable Long id, @RequestBody CollegeExpense expense) {
        try {
            return ResponseEntity.ok(financeService.updateExpense(id, expense));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /** DELETE /api/finance/expenses/{id} */
    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        try {
            financeService.deleteExpense(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
