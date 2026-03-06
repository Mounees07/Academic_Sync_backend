package com.academic.platform.service;

import com.academic.platform.model.Assignment;
import com.academic.platform.model.Section;
import com.academic.platform.model.Submission;
import com.academic.platform.model.User;
import com.academic.platform.repository.AssignmentRepository;
import com.academic.platform.repository.SectionRepository;
import com.academic.platform.repository.SubmissionRepository;
import com.academic.platform.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AssignmentService {

        @Autowired
        private AssignmentRepository assignmentRepository;

        @Autowired
        private SubmissionRepository submissionRepository;

        @Autowired
        private SectionRepository sectionRepository;

        @Autowired
        private UserRepository userRepository;

        public Assignment createAssignment(Long sectionId, Assignment assignment) {
                Section section = sectionRepository.findById(sectionId)
                                .orElseThrow(() -> new RuntimeException("Section not found"));
                assignment.setSection(section);
                return assignmentRepository.save(assignment);
        }

        public List<Assignment> getSectionAssignments(Long sectionId) {
                Section section = sectionRepository.findById(sectionId)
                                .orElseThrow(() -> new RuntimeException("Section not found"));
                return assignmentRepository.findBySection(section);
        }

        public Submission submitAssignment(Long assignmentId, String studentUid, String fileUrl) {
                Assignment assignment = assignmentRepository.findById(assignmentId)
                                .orElseThrow(() -> new RuntimeException("Assignment not found"));

                User student = userRepository.findByFirebaseUid(studentUid)
                                .orElseThrow(() -> new RuntimeException("Student not found"));

                Submission submission = submissionRepository.findByAssignmentAndStudent(assignment, student)
                                .orElse(Submission.builder()
                                                .assignment(assignment)
                                                .student(student)
                                                .build());

                submission.setFileUrl(fileUrl);
                submission.setSubmissionDate(LocalDateTime.now());

                return submissionRepository.save(submission);
        }

        public Submission gradeSubmission(Long submissionId, Double grade, String feedback) {
                Submission submission = submissionRepository.findById(submissionId)
                                .orElseThrow(() -> new RuntimeException("Submission not found"));

                submission.setGrade(grade);
                submission.setFeedback(feedback);

                return submissionRepository.save(submission);
        }

        public List<Submission> getAssignmentSubmissions(Long assignmentId) {
                Assignment assignment = assignmentRepository.findById(assignmentId)
                                .orElseThrow(() -> new RuntimeException("Assignment not found"));
                return submissionRepository.findByAssignment(assignment);
        }

        public List<Submission> getStudentSubmissions(String studentUid) {
                User student = userRepository.findByFirebaseUid(studentUid)
                                .orElseThrow(() -> new RuntimeException("Student not found"));
                return submissionRepository.findByStudent(student);
        }

        public List<Submission> getSectionSubmissions(Long sectionId) {
                return submissionRepository.findByAssignment_Section_Id(sectionId);
        }
}
