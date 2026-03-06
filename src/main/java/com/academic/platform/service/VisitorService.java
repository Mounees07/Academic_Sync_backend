package com.academic.platform.service;

import com.academic.platform.model.Visitor;
import com.academic.platform.repository.VisitorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VisitorService {
    @Autowired
    private VisitorRepository visitorRepository;

    public Visitor checkIn(Visitor visitor) {
        visitor.setStatus("CHECKED_IN");
        if (visitor.getCheckInTime() == null) {
            visitor.setCheckInTime(LocalDateTime.now());
        }
        return visitorRepository.save(visitor);
    }

    public Visitor checkOut(Long visitorId) {
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new RuntimeException("Visitor not found"));

        if ("CHECKED_OUT".equals(visitor.getStatus())) {
            throw new RuntimeException("Visitor already checked out.");
        }

        visitor.setStatus("CHECKED_OUT");
        visitor.setCheckOutTime(LocalDateTime.now());
        return visitorRepository.save(visitor);
    }

    public Visitor updateVisitor(Long id, Visitor updatedVisitor) {
        Visitor visitor = visitorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Visitor not found"));

        visitor.setName(updatedVisitor.getName());
        visitor.setContactNumber(updatedVisitor.getContactNumber());
        visitor.setPurpose(updatedVisitor.getPurpose());
        visitor.setPersonToMeet(updatedVisitor.getPersonToMeet());
        visitor.setVehicleNumber(updatedVisitor.getVehicleNumber());

        // Optionally allow editing check-in/out times if needed, but keeping it simple
        // for now

        return visitorRepository.save(visitor);
    }

    public void deleteVisitor(Long id) {
        if (!visitorRepository.existsById(id)) {
            throw new RuntimeException("Visitor not found");
        }
        visitorRepository.deleteById(id);
    }

    public List<Visitor> getAllVisitors() {
        return visitorRepository.findAllByOrderByCheckInTimeDesc();
    }

    public List<Visitor> getActiveVisitors() {
        return visitorRepository.findByStatusOrderByCheckInTimeDesc("CHECKED_IN");
    }
}
