package com.academic.platform.controller;

import com.academic.platform.model.InternalMark;
import com.academic.platform.model.User;
import com.academic.platform.model.Section;
import com.academic.platform.repository.InternalMarkRepository;
import com.academic.platform.repository.UserRepository;
import com.academic.platform.repository.SectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/internal-marks")
public class InternalMarkController {

    @Autowired
    private InternalMarkRepository markRepo;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @GetMapping("/student/{uid}")
    public ResponseEntity<List<Map<String, Object>>> getStudentMarks(@PathVariable String uid) {
        List<InternalMark> marks = markRepo.findByStudentFirebaseUidOrderBySubjectName(uid);
        List<Map<String, Object>> response = marks.stream().map(m -> {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", m.getId());
            r.put("subjectName", m.getSubjectName());
            r.put("subjectCode", m.getSubjectCode());
            r.put("assignmentMarks", m.getAssignmentMarks());
            r.put("maxAssignment", m.getMaxAssignment());
            r.put("ut1Marks", m.getUt1Marks());
            r.put("ut2Marks", m.getUt2Marks());
            r.put("maxUt", m.getMaxUt());
            r.put("modelMarks", m.getModelMarks());
            r.put("maxModel", m.getMaxModel());
            r.put("totalScore", m.getTotalInternalScore());
            r.put("maxTotal", m.getMaxTotalScore());
            r.put("percentageScore", m.getPercentageScore());
            r.put("semester", m.getSemester());
            r.put("academicYear", m.getAcademicYear());
            return r;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/upsert")
    public ResponseEntity<InternalMark> upsert(@RequestBody Map<String, Object> body) {
        String studentUid = (String) body.get("studentUid");
        Long sectionId = Long.parseLong(body.get("sectionId").toString());

        User student = userRepository.findByFirebaseUid(studentUid)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Section not found"));

        InternalMark mark = markRepo.findByStudentFirebaseUidAndSectionId(studentUid, sectionId)
                .orElse(InternalMark.builder().student(student).section(section).build());

        if (body.containsKey("subjectName")) mark.setSubjectName((String) body.get("subjectName"));
        if (body.containsKey("subjectCode")) mark.setSubjectCode((String) body.get("subjectCode"));
        if (body.containsKey("assignmentMarks"))
            mark.setAssignmentMarks(Double.parseDouble(body.get("assignmentMarks").toString()));
        if (body.containsKey("ut1Marks"))
            mark.setUt1Marks(Double.parseDouble(body.get("ut1Marks").toString()));
        if (body.containsKey("ut2Marks"))
            mark.setUt2Marks(Double.parseDouble(body.get("ut2Marks").toString()));
        if (body.containsKey("modelMarks"))
            mark.setModelMarks(Double.parseDouble(body.get("modelMarks").toString()));
        if (body.containsKey("semester"))
            mark.setSemester(Integer.parseInt(body.get("semester").toString()));
        if (body.containsKey("academicYear"))
            mark.setAcademicYear((String) body.get("academicYear"));

        return ResponseEntity.ok(markRepo.save(mark));
    }

    @GetMapping("/student/{uid}/comparison")
    public ResponseEntity<List<Map<String, Object>>> getComparison(@PathVariable String uid) {
        List<InternalMark> studentMarks = markRepo.findByStudentFirebaseUidOrderBySubjectName(uid);
        List<Map<String, Object>> comparison = new ArrayList<>();

        for (InternalMark sm : studentMarks) {
            List<InternalMark> sectionMarks = markRepo.findBySectionId(sm.getSection().getId());
            double classAvg = sectionMarks.stream()
                    .mapToDouble(InternalMark::getPercentageScore).average().orElse(0);

            Map<String, Object> r = new LinkedHashMap<>();
            r.put("subjectName", sm.getSubjectName());
            r.put("studentScore", sm.getPercentageScore());
            r.put("classAverage", Math.round(classAvg * 100.0) / 100.0);
            r.put("improvement", sm.getPercentageScore() > classAvg ? "ABOVE" :
                    sm.getPercentageScore() < classAvg ? "BELOW" : "AT_PAR");
            comparison.add(r);
        }
        return ResponseEntity.ok(comparison);
    }
}

