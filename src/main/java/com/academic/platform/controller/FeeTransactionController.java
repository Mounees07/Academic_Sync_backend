package com.academic.platform.controller;

import com.academic.platform.model.FeeTransaction;
import com.academic.platform.repository.FeeTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/fee-transactions")
public class FeeTransactionController {

    @Autowired
    private FeeTransactionRepository feeTransactionRepository;

    @GetMapping("/student/{uid}")
    public ResponseEntity<List<FeeTransaction>> getTransactionsByStudent(@PathVariable String uid) {
        return ResponseEntity.ok(feeTransactionRepository.findByStudentFirebaseUidOrderByCreatedAtDesc(uid));
    }
}
