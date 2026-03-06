package com.academic.platform.service;

import com.academic.platform.model.FacultyWorkload;
import com.academic.platform.model.User;
import com.academic.platform.repository.FacultyWorkloadRepository;
import com.academic.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class FacultyWorkloadService {

    @Autowired
    private FacultyWorkloadRepository workloadRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get workload record for a faculty member by their Firebase UID.
     * Returns empty if no saved record exists (caller can fall back to computed
     * defaults).
     */
    @Transactional(readOnly = true)
    public Optional<FacultyWorkload> getByFirebaseUid(String firebaseUid) {
        return workloadRepository.findByFaculty_FirebaseUid(firebaseUid);
    }

    /**
     * Get all saved workload records for a department.
     * Keyed by faculty DB id so the frontend can merge them into its state map.
     */
    @Transactional(readOnly = true)
    public List<FacultyWorkload> getByDepartment(String department) {
        return workloadRepository.findByFaculty_StudentDetails_DepartmentIgnoreCase(department);
    }

    /**
     * Upsert: create or update a faculty workload record.
     * 
     * @param firebaseUid Firebase UID of the faculty member
     * @param teaching    Teaching hours per week
     * @param research    Research hours per week
     * @param admin       Administrative hours per week
     */
    @Transactional
    public FacultyWorkload upsert(String firebaseUid, int teaching, int research, int admin) {
        User faculty = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("Faculty not found: " + firebaseUid));

        FacultyWorkload record = workloadRepository
                .findByFaculty_FirebaseUid(firebaseUid)
                .orElseGet(() -> {
                    FacultyWorkload fw = new FacultyWorkload();
                    fw.setFaculty(faculty);
                    return fw;
                });

        record.setTeachingHours(teaching);
        record.setResearchHours(research);
        record.setAdminHours(admin);

        return workloadRepository.save(record);
    }

    /**
     * Bulk upsert: save allocations for all faculty members in one request.
     * 
     * @param entries List of allocation entries
     */
    @Transactional
    public void bulkUpsert(List<AllocationEntry> entries) {
        for (AllocationEntry e : entries) {
            upsert(e.getFirebaseUid(), e.getTeaching(), e.getResearch(), e.getAdmin());
        }
    }

    // ── Inner DTO ──────────────────────────────────────────────────────────────
    public static class AllocationEntry {
        private String firebaseUid;
        private int teaching;
        private int research;
        private int admin;

        public String getFirebaseUid() {
            return firebaseUid;
        }

        public void setFirebaseUid(String firebaseUid) {
            this.firebaseUid = firebaseUid;
        }

        public int getTeaching() {
            return teaching;
        }

        public void setTeaching(int teaching) {
            this.teaching = teaching;
        }

        public int getResearch() {
            return research;
        }

        public void setResearch(int research) {
            this.research = research;
        }

        public int getAdmin() {
            return admin;
        }

        public void setAdmin(int admin) {
            this.admin = admin;
        }
    }
}
