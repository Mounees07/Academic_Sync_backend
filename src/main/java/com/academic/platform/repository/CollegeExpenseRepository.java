package com.academic.platform.repository;

import com.academic.platform.model.CollegeExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CollegeExpenseRepository extends JpaRepository<CollegeExpense, Long> {

    List<CollegeExpense> findAllByOrderByExpenseDateDesc();

    List<CollegeExpense> findByAcademicYear(String academicYear);

    List<CollegeExpense> findByExpenseType(String expenseType);
}
