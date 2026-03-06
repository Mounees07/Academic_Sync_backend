package com.academic.platform.controller;

import com.academic.platform.model.Assignment;
import com.academic.platform.model.Submission;
import com.academic.platform.service.AssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assignments")
@CrossOrigin(origins = { "http://localhost:5173", "http://10.10.188.128:5173" }, allowCredentials = "true")
public class AssignmentController {

    @Autowired
    private AssignmentService assignmentService;

    @PostMapping("/section/{sectionId}")
    public ResponseEntity<Assignment> createAssignment(@PathVariable Long sectionId,
            @RequestBody Assignment assignment) {
        return ResponseEntity.ok(assignmentService.createAssignment(sectionId, assignment));
    }

    @GetMapping("/section/{sectionId}")
    public ResponseEntity<List<Assignment>> getSectionAssignments(@PathVariable Long sectionId) {
        return ResponseEntity.ok(assignmentService.getSectionAssignments(sectionId));
    }

    @PostMapping("/{assignmentId}/submit")
    public ResponseEntity<Submission> submitAssignment(
            @PathVariable Long assignmentId,
            @RequestParam String studentUid,
            @RequestBody Map<String, String> body) {
        String fileUrl = body.get("fileUrl");
        return ResponseEntity.ok(assignmentService.submitAssignment(assignmentId, studentUid, fileUrl));
    }

    @PostMapping("/submissions/{submissionId}/grade")
    public ResponseEntity<Submission> gradeSubmission(
            @PathVariable Long submissionId,
            @RequestBody Map<String, Object> body) {
        Double grade = Double.valueOf(body.get("grade").toString());
        String feedback = (String) body.get("feedback");
        return ResponseEntity.ok(assignmentService.gradeSubmission(submissionId, grade, feedback));
    }

    @GetMapping("/{assignmentId}/submissions")
    public ResponseEntity<List<Submission>> getAssignmentSubmissions(@PathVariable Long assignmentId) {
        return ResponseEntity.ok(assignmentService.getAssignmentSubmissions(assignmentId));
    }

    @GetMapping("/student/{studentUid}")
    public ResponseEntity<List<Submission>> getStudentSubmissions(@PathVariable String studentUid) {
        return ResponseEntity.ok(assignmentService.getStudentSubmissions(studentUid));
    }

    @GetMapping("/teacher/section/{sectionId}/submissions")
    public ResponseEntity<List<Submission>> getSectionSubmissions(@PathVariable Long sectionId) {
        return ResponseEntity.ok(assignmentService.getSectionSubmissions(sectionId));
    }
}
