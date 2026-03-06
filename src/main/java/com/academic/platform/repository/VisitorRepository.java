package com.academic.platform.repository;

import com.academic.platform.model.Visitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VisitorRepository extends JpaRepository<Visitor, Long> {
    List<Visitor> findByStatusOrderByCheckInTimeDesc(String status);

    List<Visitor> findAllByOrderByCheckInTimeDesc();
}
