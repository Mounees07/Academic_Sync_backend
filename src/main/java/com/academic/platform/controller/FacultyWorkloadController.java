package com.academic.platform.controller;

import com.academic.platform.model.FacultyWorkload;
import com.academic.platform.service.FacultyWorkloadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/faculty-workload")
public class FacultyWorkloadController {

    @Autowired
    private FacultyWorkloadService workloadService;

    /**
     * GET /api/faculty-workload/department/{dept}
     * Returns all saved workload records for every faculty member in a department.
     * The frontend uses this to initialise the allocation state on page load.
     */
    @GetMapping("/department/{department}")
    public ResponseEntity<List<FacultyWorkload>> getByDepartment(
            @PathVariable String department) {
        return ResponseEntity.ok(workloadService.getByDepartment(department));
    }

    /**
     * GET /api/faculty-workload/faculty/{firebaseUid}
     * Returns the saved workload record for a single faculty member (if any).
     */
    @GetMapping("/faculty/{firebaseUid}")
    public ResponseEntity<FacultyWorkload> getByFaculty(
            @PathVariable String firebaseUid) {
        Optional<FacultyWorkload> result = workloadService.getByFirebaseUid(firebaseUid);
        return result.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /**
     * PUT /api/faculty-workload/faculty/{firebaseUid}
     * Upsert the workload record for a single faculty member.
     * Body: { "teaching": 18, "research": 10, "admin": 14 }
     */
    @PutMapping("/faculty/{firebaseUid}")
    public ResponseEntity<FacultyWorkload> upsert(
            @PathVariable String firebaseUid,
            @RequestBody Map<String, Integer> body) {
        int teaching = body.getOrDefault("teaching", 0);
        int research = body.getOrDefault("research", 0);
        int admin = body.getOrDefault("admin", 0);
        FacultyWorkload saved = workloadService.upsert(firebaseUid, teaching, research, admin);
        return ResponseEntity.ok(saved);
    }

    /**
     * POST /api/faculty-workload/bulk
     * Save allocations for multiple faculty members at once — called by the
     * "Save Changes" button on the Adjust Allocations page.
     * Body: [ { "firebaseUid": "...", "teaching": 18, "research": 10, "admin": 14
     * }, ... ]
     */
    @PostMapping("/bulk")
    public ResponseEntity<?> bulkUpsert(
            @RequestBody List<FacultyWorkloadService.AllocationEntry> entries) {
        try {
            workloadService.bulkUpsert(entries);
            return ResponseEntity.ok(Map.of("message", "Allocations saved successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
