package com.academic.platform.service;

import com.academic.platform.model.Result;
import com.academic.platform.model.User;
import com.academic.platform.model.Course;
import com.academic.platform.model.Section;
import com.academic.platform.repository.ResultRepository;
import com.academic.platform.repository.UserRepository;
import com.academic.platform.repository.CourseRepository;
import com.academic.platform.repository.SectionRepository;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ResultService {

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SectionRepository sectionRepository;

    public byte[] generateTemplate(String dept, Integer sem) {
        StringBuilder csv = new StringBuilder();

        // 1. Base Headers
        csv.append("StudentEmail,RegisterNumber,StudentName,Department,Semester");

        // 2. Fetch Dynamic Subjects for this Batch
        List<String> subjectCodes = new ArrayList<>();
        if (sem != null) {
            // Fetch Sections for this Semester
            // Assuming simplified exact match first since we control the UI values
            List<Section> sections = sectionRepository.findBySemester(String.valueOf(sem));

            // Filter by Dept if provided
            if (dept != null) {
                sections = sections.stream()
                        .filter(s -> s.getFaculty().getStudentDetails().getDepartment() != null
                                && s.getFaculty().getStudentDetails().getDepartment().equalsIgnoreCase(dept))
                        .collect(Collectors.toList());
            }

            subjectCodes = sections.stream()
                    .map(s -> s.getCourse().getCode())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        }

        // 3. Append Subject Headers
        if (subjectCodes.isEmpty()) {
            csv.append(",SubjectCode1,SubjectCode2,SubjectCode3,SubjectCode4,SubjectCode5");
        } else {
            for (String code : subjectCodes) {
                csv.append(",").append(code);
            }
        }
        csv.append("\n");

        // 4. Fetch Students
        List<User> students;
        if (dept != null && sem != null) {
            students = userRepository.findByStudentDetails_DepartmentAndStudentDetails_Semester(dept, sem);
        } else if (dept != null) {
            students = userRepository.findByStudentDetails_Department(dept);
        } else {
            students = userRepository.findByRole(com.academic.platform.model.Role.STUDENT);
        }

        // 5. Append Rows
        for (User s : students) {
            csv.append(String.format("%s,%s,%s,%s,%d",
                    s.getEmail(),
                    s.getStudentDetails().getRollNumber() != null ? s.getStudentDetails().getRollNumber() : "",
                    s.getFullName(),
                    s.getStudentDetails().getDepartment() != null ? s.getStudentDetails().getDepartment() : "",
                    s.getStudentDetails().getSemester() != null ? s.getStudentDetails().getSemester() : 0));

            // Add empty commas for subjects to match header count
            int count = subjectCodes.isEmpty() ? 5 : subjectCodes.size();
            for (int i = 0; i < count; i++)
                csv.append(",");

            csv.append("\n");
        }
        return csv.toString().getBytes();
    }

    @Transactional
    public List<String> processBulkResultUpload(InputStream inputStream) {
        List<String> logs = new ArrayList<>();

        try (CSVReader reader = new CSVReaderBuilder(new InputStreamReader(inputStream)).build()) {
            List<String[]> allRows = reader.readAll();
            if (allRows.isEmpty()) {
                logs.add("❌ Empty CSV file");
                return logs;
            }

            // Parse Headers from Row 0
            String[] headers = allRows.get(0);
            List<Integer> subjectColIndices = new ArrayList<>();
            List<String> subjectCodes = new ArrayList<>();

            // Identify Subject Columns (Everything after index 4: Email, RegNo, Name, Dept,
            // Sem)
            for (int i = 5; i < headers.length; i++) {
                String header = headers[i].trim();
                // Clean header if it has suffix like _Marks
                String code = header.replaceAll("(?i)_marks", "").trim();

                if (!code.isEmpty() && !code.startsWith("SubjectCode")) { // Ignore placeholders if left untouched
                    subjectColIndices.add(i);
                    subjectCodes.add(code);
                }
            }

            logs.add("ℹ️ Detected Subjects: " + String.join(", ", subjectCodes));

            // Process Data Rows (Start from Row 1)
            for (int i = 1; i < allRows.size(); i++) {
                String[] line = allRows.get(i);
                if (line.length < 1)
                    continue;

                String email = line[0].trim();
                if (email.isEmpty())
                    continue;

                Optional<User> studentOpt = userRepository.findByEmail(email.toLowerCase());
                if (studentOpt.isEmpty()) {
                    logs.add("❌ Row " + (i + 1) + ": Student not found (" + email + ")");
                    continue;
                }
                User student = studentOpt.get();

                double totalGradePoints = 0;
                int totalCredits = 0;
                int totalMarks = 0;
                boolean hasResults = false;

                // Process each subject for this student
                for (int j = 0; j < subjectColIndices.size(); j++) {
                    int colIdx = subjectColIndices.get(j);
                    String subCode = subjectCodes.get(j);

                    if (colIdx >= line.length)
                        continue;

                    String markStr = line[colIdx].trim();
                    if (markStr.isEmpty())
                        continue;

                    try {
                        int marks = Integer.parseInt(markStr);
                        hasResults = true;
                        totalMarks += marks;

                        // Fetch Course Credits (Default to 3 if dynamic/unknown)
                        int credits = 3;
                        Optional<Course> courseOpt = courseRepository.findByCode(subCode);
                        if (courseOpt.isPresent()) {
                            credits = courseOpt.get().getCredits() != null ? courseOpt.get().getCredits() : 3;
                        }

                        // Calculate Grade
                        String grade = calculateGrade(marks);
                        int points = getGradePoints(grade);

                        // SGPA Logic
                        if (!grade.equals("RA") && !grade.equals("AB")) {
                            totalGradePoints += (points * credits);
                            totalCredits += credits;
                        } else {
                            // Re-appear: add credits to denominator, points are 0
                            totalCredits += credits;
                        }

                        // Save Result
                        Result result = Result.builder()
                                .student(student)
                                .subjectCode(subCode)
                                .subjectName(courseOpt.map(Course::getName).orElse(subCode))
                                .grade(grade)
                                .credits(credits)
                                .semester(student.getStudentDetails().getSemester())
                                .examType("SEMESTER")
                                .publishedDate(LocalDate.now())
                                .build();
                        resultRepository.save(result);

                    } catch (NumberFormatException e) {
                        logs.add("⚠️ Invalid mark for " + email + " in " + subCode);
                    }
                }

                // Update SGPA for Student
                if (hasResults && totalCredits > 0) {
                    double sgpa = totalGradePoints / totalCredits;
                    sgpa = Math.round(sgpa * 100.0) / 100.0;
                    student.getStudentDetails().setGpa(sgpa);
                    userRepository.save(student);
                    logs.add("✅ " + email + ": Updated Results & SGPA: " + sgpa);
                }
            }

        } catch (Exception e) {
            logs.add("❗ File Error: " + e.getMessage());
            e.printStackTrace();
        }
        return logs;
    }

    private String calculateGrade(int marks) {
        if (marks >= 91)
            return "O";
        if (marks >= 81)
            return "A+";
        if (marks >= 71)
            return "A";
        if (marks >= 61)
            return "B+";
        if (marks >= 50)
            return "B";
        return "RA";
    }

    private int getGradePoints(String grade) {
        switch (grade) {
            case "O":
                return 10;
            case "A+":
                return 9;
            case "A":
                return 8;
            case "B+":
                return 7;
            case "B":
                return 6;
            default:
                return 0;
        }
    }

    public List<Result> getResultsByStudent(String uid) {
        User student = userRepository.findByFirebaseUid(uid)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return resultRepository.findByStudent(student);
    }

    public List<Result> getRecentPublishedResults() {
        return resultRepository.findTop50ByOrderByPublishedDateDesc();
    }

    public List<Map<String, Object>> getSGPAHistory(String uid) {
        // Find user by UID (handle specific logic if needed for role)
        // Using findByFirebaseUid assuming it exists in repo or custom query
        // Re-using exiting user lookup logic pattern
        Optional<User> userOpt = userRepository.findByFirebaseUid(uid);
        if (userOpt.isEmpty()) {
            // Fallback to ID if UID lookup fails or is actually ID
            try {
                userOpt = userRepository.findById(Long.parseLong(uid));
            } catch (NumberFormatException e) {
                return new ArrayList<>();
            }
        }

        if (userOpt.isEmpty())
            return new ArrayList<>();
        User student = userOpt.get();

        List<Result> allResults = resultRepository.findByStudent(student);

        // Group by Semester
        Map<Integer, List<Result>> resultsBySemester = allResults.stream()
                .filter(r -> r.getSemester() != null)
                .collect(Collectors.groupingBy(Result::getSemester, TreeMap::new, Collectors.toList()));

        List<Map<String, Object>> history = new ArrayList<>();

        for (Map.Entry<Integer, List<Result>> entry : resultsBySemester.entrySet()) {
            Integer semester = entry.getKey();
            List<Result> semesterResults = entry.getValue();

            double totalGradePoints = 0;
            int totalCredits = 0;

            for (Result r : semesterResults) {
                int points = getGradePoints(r.getGrade());
                int credits = r.getCredits() != null ? r.getCredits() : 0;

                if (!"RA".equals(r.getGrade()) && !"AB".equals(r.getGrade())) {
                    totalGradePoints += (points * credits);
                    totalCredits += credits;
                } else {
                    // For arrears, we count credits but 0 points?
                    // Standard logic: SGPA is usually for passed subjects or includes failures as 0
                    // points.
                    // Using same logic as bulk upload:
                    totalCredits += credits;
                }
            }

            double sgpa = 0.0;
            if (totalCredits > 0) {
                sgpa = totalGradePoints / totalCredits;
                sgpa = Math.round(sgpa * 100.0) / 100.0;
            }

            Map<String, Object> semData = new HashMap<>();
            semData.put("semester", toRoman(semester));
            semData.put("semNumber", semester); // for sorting if needed
            semData.put("sgpa", sgpa);
            history.add(semData);
        }

        return history;
    }

    private String toRoman(int num) {
        String[] roman = { "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII" };
        return (num > 0 && num < roman.length) ? roman[num] : String.valueOf(num);
    }
}
