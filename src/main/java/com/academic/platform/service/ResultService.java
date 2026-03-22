package com.academic.platform.service;

import com.academic.platform.model.Course;
import com.academic.platform.model.InternalMark;
import com.academic.platform.model.Result;
import com.academic.platform.model.Section;
import com.academic.platform.model.User;
import com.academic.platform.repository.CourseRepository;
import com.academic.platform.repository.EnrollmentRepository;
import com.academic.platform.repository.InternalMarkRepository;
import com.academic.platform.repository.ResultRepository;
import com.academic.platform.repository.AcademicScheduleRepository;
import com.academic.platform.repository.SectionRepository;
import com.academic.platform.repository.UserRepository;
import com.academic.platform.model.AcademicSchedule;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class ResultService {

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private InternalMarkRepository internalMarkRepository;

    @Autowired
    private AcademicScheduleRepository academicScheduleRepository;

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private Integer parseSemesterValue(String value) {
        String normalized = normalizeText(value).toUpperCase()
                .replace("SEMESTER", "")
                .replace("SEM", "")
                .trim();
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean matchesScheduledExamSubject(String scheduleSubject, String courseCode, String courseName) {
        String normalizedSchedule = normalizeText(scheduleSubject);
        if (normalizedSchedule.isBlank()) {
            return false;
        }
        return normalizedSchedule.equalsIgnoreCase(normalizeText(courseName))
                || normalizedSchedule.equalsIgnoreCase(normalizeText(courseCode));
    }

    private boolean hasSemesterExamScheduled(String courseCode, String courseName) {
        return academicScheduleRepository.findByType(AcademicSchedule.ScheduleType.SEMESTER_EXAM).stream()
                .anyMatch(schedule -> matchesScheduledExamSubject(schedule.getSubjectName(), courseCode, courseName));
    }

    private List<User> getStudentsForDepartmentAndSemester(String dept, Integer sem) {
        List<User> students;
        if (dept != null && sem != null) {
            students = userRepository.findByStudentDetails_DepartmentAndStudentDetails_Semester(dept, sem).stream()
                    .filter(user -> user.getRole() == com.academic.platform.model.Role.STUDENT)
                    .collect(Collectors.toList());
        } else if (dept != null) {
            students = userRepository.findByStudentDetails_DepartmentIgnoreCaseAndRoleIn(
                    dept,
                    List.of(com.academic.platform.model.Role.STUDENT)
            );
        } else {
            students = userRepository.findByRole(com.academic.platform.model.Role.STUDENT);
        }

        if (students.isEmpty() && dept != null) {
            students = userRepository.findByStudentDetails_DepartmentIgnoreCaseAndRoleIn(
                    dept,
                    List.of(com.academic.platform.model.Role.STUDENT)
            );
        }

        if (students.isEmpty()) {
            String targetDept = normalizeText(dept);
            students = userRepository.findAll().stream()
                    .filter(user -> user.getStudentDetails() != null)
                    .filter(user -> targetDept.isBlank()
                            || normalizeText(user.getStudentDetails().getDepartment()).equalsIgnoreCase(targetDept)
                            || normalizeText(user.getStudentDetails().getBranchName()).equalsIgnoreCase(targetDept)
                            || normalizeText(user.getStudentDetails().getBranchCode()).equalsIgnoreCase(targetDept))
                    .filter(user -> sem == null
                            || sem.equals(user.getStudentDetails().getSemester()))
                    .collect(Collectors.toList());
        }

        if (students.isEmpty() && dept != null) {
            String targetDept = normalizeText(dept);
            students = userRepository.findAll().stream()
                    .filter(user -> user.getStudentDetails() != null)
                    .filter(user -> normalizeText(user.getStudentDetails().getDepartment()).equalsIgnoreCase(targetDept)
                            || normalizeText(user.getStudentDetails().getBranchName()).equalsIgnoreCase(targetDept)
                            || normalizeText(user.getStudentDetails().getBranchCode()).equalsIgnoreCase(targetDept))
                    .filter(user -> sem == null
                            || sem.equals(user.getStudentDetails().getSemester()))
                    .collect(Collectors.toList());
        }

        if (students.isEmpty()) {
            students = userRepository.findByRole(com.academic.platform.model.Role.STUDENT).stream()
                    .filter(user -> user.getStudentDetails() != null)
                    .filter(user -> sem == null
                            || sem.equals(user.getStudentDetails().getSemester()))
                    .collect(Collectors.toList());
        }

        if (students.isEmpty()) {
            students = userRepository.findAll().stream()
                    .filter(user -> user.getStudentDetails() != null
                            || user.getRole() == com.academic.platform.model.Role.STUDENT)
                    .filter(user -> user.getStudentDetails() != null)
                    .filter(user -> sem == null
                            || sem.equals(user.getStudentDetails().getSemester()))
                    .collect(Collectors.toList());
        }

        return students.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(User::getId, user -> user, (left, right) -> left, LinkedHashMap::new),
                        map -> new ArrayList<>(map.values())
                ));
    }

    private List<Section> getStudentSemesterSections(User student, String dept, Integer sem) {
        return enrollmentRepository.findByStudent(student).stream()
                .map(com.academic.platform.model.Enrollment::getSection)
                .filter(section -> section != null && section.getCourse() != null)
                .filter(section -> hasSemesterExamScheduled(
                        section.getCourse().getCode(),
                        section.getCourse().getName()))
                // Student filtering already scopes department; use enrollments + semester here.
                .filter(section -> {
                    if (sem == null) {
                        return true;
                    }
                    Integer sectionSemester = parseSemesterValue(section.getSemester());
                    if (sectionSemester != null) {
                        return sem.equals(sectionSemester);
                    }
                    return student.getStudentDetails() == null
                            || student.getStudentDetails().getSemester() == null
                            || sem.equals(student.getStudentDetails().getSemester());
                })
                .collect(Collectors.toList());
    }

    private Double resolveInternalMarks(User student, Section section, Integer sem) {
        Optional<InternalMark> exactMark = internalMarkRepository
                .findByStudentFirebaseUidAndSectionId(student.getFirebaseUid(), section.getId());
        if (exactMark.isPresent()) {
            return Math.round(exactMark.get().getPercentageScore() * 100.0) / 100.0;
        }

        return internalMarkRepository.findByStudentFirebaseUidAndSemester(student.getFirebaseUid(), sem != null ? sem : 0)
                .stream()
                .filter(mark -> mark.getSubjectCode() != null
                        && section.getCourse().getCode() != null
                        && mark.getSubjectCode().equalsIgnoreCase(section.getCourse().getCode()))
                .map(InternalMark::getPercentageScore)
                .findFirst()
                .map(score -> Math.round(score * 100.0) / 100.0)
                .orElse(0.0);
    }

    private double resolveInternalMarksFromExisting(User student, String subjectCode, Integer sem) {
        if (subjectCode == null || subjectCode.isBlank()) {
            return 0.0;
        }

        return internalMarkRepository.findByStudentFirebaseUidAndSemester(student.getFirebaseUid(), sem != null ? sem : 0)
                .stream()
                .filter(mark -> subjectCode.equalsIgnoreCase(mark.getSubjectCode()))
                .map(InternalMark::getPercentageScore)
                .findFirst()
                .map(score -> Math.round(score * 100.0) / 100.0)
                .orElse(0.0);
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double convertSemesterMarksToSixty(Double semesterMarks) {
        if (semesterMarks == null) {
            return 0.0;
        }
        return round2((semesterMarks / 100.0) * 60.0);
    }

    private double calculateFinalMarks(double internalMarks, Double semesterMarks) {
        return round2(internalMarks + convertSemesterMarksToSixty(semesterMarks));
    }

    private String calculateResultStatus(Double semesterMarks, String grade) {
        if (semesterMarks == null) {
            return "AB";
        }
        return "RA".equals(grade) ? "RA" : "PASS";
    }

    private Result normalizeResult(Result result) {
        if (result == null) {
            return null;
        }

        double internalMarks = round2(result.getInternalMarks() != null ? result.getInternalMarks() : 0.0);
        Double semesterMarks = result.getSemesterMarks() == null ? null : round2(result.getSemesterMarks());
        double totalMarks = calculateFinalMarks(internalMarks, semesterMarks);
        String grade = calculateGrade(totalMarks, semesterMarks);
        int gradePoints = getGradePoints(grade);

        result.setInternalMarks(internalMarks);
        result.setSemesterMarks(semesterMarks);
        result.setTotalMarks(totalMarks);
        result.setGrade(grade);
        result.setGradePoints(gradePoints);
        return result;
    }

    private Double calculateSgpa(List<Result> results) {
        if (results == null || results.isEmpty()) {
            return null;
        }

        boolean hasArrear = false;
        double totalGradePoints = 0;
        int totalCredits = 0;

        for (Result rawResult : results) {
            Result result = normalizeResult(rawResult);
            int credits = result.getCredits() != null ? result.getCredits() : 0;
            if (credits <= 0) {
                continue;
            }

            if ("RA".equals(result.getGrade())) {
                hasArrear = true;
                break;
            }

            if ("AB".equals(result.getGrade())) {
                continue;
            }

            totalGradePoints += (double) (result.getGradePoints() != null ? result.getGradePoints() : 0) * credits;
            totalCredits += credits;
        }

        if (hasArrear || totalCredits == 0) {
            return null;
        }

        return round2(totalGradePoints / totalCredits);
    }

    private int calculateArrearCount(List<Result> results) {
        if (results == null) {
            return 0;
        }

        return (int) results.stream()
                .map(this::normalizeResult)
                .filter(result -> "RA".equals(result.getGrade()))
                .count();
    }

    private List<Map<String, Object>> buildFallbackSubjectsFromInternalMarks(User student, Integer sem) {
        return internalMarkRepository.findByStudentFirebaseUidAndSemester(student.getFirebaseUid(), sem != null ? sem : 0)
                .stream()
                .filter(mark -> hasSemesterExamScheduled(mark.getSubjectCode(), mark.getSubjectName()))
                .map(mark -> {
                    double internalMarks = round2(mark.getPercentageScore());
                    Double semesterMarks = null;
                    double totalMarks = calculateFinalMarks(internalMarks, semesterMarks);
                    String grade = calculateGrade(totalMarks, semesterMarks);
                    String resultStatus = calculateResultStatus(semesterMarks, grade);

                    Map<String, Object> subject = new LinkedHashMap<>();
                    subject.put("sectionId", mark.getSection() != null ? mark.getSection().getId() : null);
                    subject.put("subjectCode", mark.getSubjectCode() != null ? mark.getSubjectCode() : mark.getSubjectName());
                    subject.put("subjectName", mark.getSubjectName());
                    subject.put("credits", mark.getSection() != null
                            && mark.getSection().getCourse() != null
                            && mark.getSection().getCourse().getCredits() != null
                            ? mark.getSection().getCourse().getCredits()
                            : 3);
                    subject.put("internalMarks", internalMarks);
                    subject.put("semesterMarks", semesterMarks);
                    subject.put("convertedSemesterMarks", convertSemesterMarksToSixty(semesterMarks));
                    subject.put("totalMarks", totalMarks);
                    subject.put("finalPercentage", totalMarks);
                    subject.put("grade", grade);
                    subject.put("resultStatus", resultStatus);
                    return subject;
                })
                .collect(Collectors.toList());
    }

    private Result saveOrUpdateResult(User student, String subjectCode, Integer semester, double internalMarks, Double semesterMarks) {
        Optional<Course> courseOpt = courseRepository.findByCode(subjectCode);
        int credits = courseOpt.map(Course::getCredits).orElse(3);
        if (credits <= 0) {
            credits = 3;
        }

        double totalMarks = calculateFinalMarks(internalMarks, semesterMarks);
        String grade = calculateGrade(totalMarks, semesterMarks);
        int gradePoints = getGradePoints(grade);

        Result result = resultRepository.findByStudentAndSemesterAndSubjectCode(student, semester, subjectCode)
                .orElse(Result.builder()
                        .student(student)
                        .subjectCode(subjectCode)
                        .semester(semester)
                        .examType("SEMESTER")
                        .build());

        result.setSubjectName(courseOpt.map(Course::getName).orElse(subjectCode));
        result.setCredits(credits);
        result.setInternalMarks(round2(internalMarks));
        result.setSemesterMarks(semesterMarks == null ? null : round2(semesterMarks));
        result.setTotalMarks(totalMarks);
        result.setGrade(grade);
        result.setGradePoints(gradePoints);
        result.setPublishedDate(LocalDate.now());
        result.setExamType("SEMESTER");

        return resultRepository.save(result);
    }

    public byte[] generateTemplate(String dept, Integer sem) {
        StringBuilder csv = new StringBuilder();
        csv.append("StudentEmail,RegisterNumber,StudentName,Department,Semester");

        List<User> students = getStudentsForDepartmentAndSemester(dept, sem);
        Map<Long, List<Section>> studentSections = new HashMap<>();
        LinkedHashSet<String> subjectCodes = new LinkedHashSet<>();

        for (User student : students) {
            List<Section> sections = getStudentSemesterSections(student, dept, sem);
            studentSections.put(student.getId(), sections);
            sections.stream()
                    .map(section -> section.getCourse().getCode())
                    .filter(code -> code != null && !code.isBlank())
                    .sorted()
                    .forEach(subjectCodes::add);
        }

        if (subjectCodes.isEmpty() && dept != null) {
            List<Course> deptCourses = courseRepository.findByDepartment(dept);
            if (deptCourses.isEmpty()) {
                deptCourses = courseRepository.findAll();
            }
            deptCourses.stream()
                    .map(Course::getCode)
                    .filter(code -> code != null && !code.isBlank())
                    .sorted()
                    .forEach(subjectCodes::add);
        }

        if (subjectCodes.isEmpty()) {
            csv.append(",SubjectCode1_Internal,SubjectCode1_Semester");
        } else {
            for (String code : subjectCodes) {
                csv.append(",").append(code).append("_Internal");
                csv.append(",").append(code).append("_Semester");
            }
        }
        csv.append("\n");

        for (User student : students) {
            String name = student.getFullName() != null ? student.getFullName() : "No Name";
            name = name.replace("\"", "\"\"");

            csv.append(String.format("%s,%s,\"%s\",%s,%d",
                    student.getEmail() != null ? student.getEmail() : "",
                    student.getStudentDetails() != null && student.getStudentDetails().getRollNumber() != null
                            ? student.getStudentDetails().getRollNumber() : "",
                    name,
                    student.getStudentDetails() != null && student.getStudentDetails().getDepartment() != null
                            ? student.getStudentDetails().getDepartment() : "",
                    student.getStudentDetails() != null && student.getStudentDetails().getSemester() != null
                            ? student.getStudentDetails().getSemester() : 0));

            List<Section> sections = studentSections.getOrDefault(student.getId(), new ArrayList<>());
            Map<String, Section> sectionByCode = sections.stream()
                    .filter(section -> section.getCourse() != null && section.getCourse().getCode() != null)
                    .collect(Collectors.toMap(
                            section -> section.getCourse().getCode(),
                            section -> section,
                            (left, right) -> left,
                            LinkedHashMap::new));

            if (subjectCodes.isEmpty()) {
                csv.append(",0.00,");
            } else {
                for (String code : subjectCodes) {
                    Section section = sectionByCode.get(code);
                    if (section == null) {
                        csv.append(",NA,NA");
                        continue;
                    }

                    Double internalMarks = resolveInternalMarks(student, section, sem);
                    csv.append(",").append(String.format("%.2f", internalMarks));
                    csv.append(",");
                }
            }
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
                logs.add("âŒ Empty CSV file");
                return logs;
            }

            String[] headers = allRows.get(0);
            List<Map<String, Object>> subjectColumns = new ArrayList<>();
            for (int i = 5; i < headers.length; i++) {
                String header = headers[i].trim();
                if (header.isEmpty() || header.startsWith("SubjectCode")) {
                    continue;
                }

                String normalized = header.replaceAll("(?i)_marks", "").trim();
                if (normalized.endsWith("_Internal")) {
                    String code = normalized.substring(0, normalized.length() - "_Internal".length()).trim();
                    subjectColumns.add(Map.of("code", code, "index", i, "type", "INTERNAL"));
                } else if (normalized.endsWith("_Semester")) {
                    String code = normalized.substring(0, normalized.length() - "_Semester".length()).trim();
                    subjectColumns.add(Map.of("code", code, "index", i, "type", "SEMESTER"));
                } else {
                    subjectColumns.add(Map.of("code", normalized, "index", i, "type", "LEGACY"));
                }
            }

            for (int i = 1; i < allRows.size(); i++) {
                String[] line = allRows.get(i);
                if (line.length < 1) {
                    continue;
                }

                String email = line[0].trim();
                if (email.isEmpty()) {
                    continue;
                }

                Optional<User> studentOpt = userRepository.findByEmail(email.toLowerCase());
                if (studentOpt.isEmpty()) {
                    logs.add("âŒ Row " + (i + 1) + ": Student not found (" + email + ")");
                    continue;
                }
                User student = studentOpt.get();

                Integer rowSemester = line.length > 4 && !line[4].trim().isEmpty()
                        ? Integer.parseInt(line[4].trim())
                        : (student.getStudentDetails() != null ? student.getStudentDetails().getSemester() : null);
                if (rowSemester == null) {
                    logs.add("âŒ Row " + (i + 1) + ": Semester not found for " + email);
                    continue;
                }

                boolean hasResults = false;

                Map<String, Double> internalByCode = new HashMap<>();
                Map<String, Double> semesterByCode = new HashMap<>();
                Map<String, Boolean> absentByCode = new HashMap<>();

                for (Map<String, Object> column : subjectColumns) {
                    String code = String.valueOf(column.get("code"));
                    int colIdx = (Integer) column.get("index");
                    String type = String.valueOf(column.get("type"));
                    if (colIdx >= line.length) {
                        continue;
                    }

                    String rawValue = line[colIdx].trim();
                    if (rawValue.isEmpty() || "NA".equalsIgnoreCase(rawValue)) {
                        continue;
                    }

                    if ("SEMESTER".equals(type) && ("AB".equalsIgnoreCase(rawValue) || "ABSENT".equalsIgnoreCase(rawValue))) {
                        absentByCode.put(code, true);
                        continue;
                    }

                    try {
                        double value = Double.parseDouble(rawValue);
                        if ("INTERNAL".equals(type)) {
                            internalByCode.put(code, value);
                        } else {
                            semesterByCode.put(code, value);
                        }
                    } catch (NumberFormatException e) {
                        logs.add("âš ï¸ Invalid mark for " + email + " in " + code + " (" + type + ")");
                    }
                }

                LinkedHashSet<String> attemptedSubjectCodes = new LinkedHashSet<>();
                attemptedSubjectCodes.addAll(semesterByCode.keySet());
                attemptedSubjectCodes.addAll(absentByCode.keySet());

                for (String subjectCode : attemptedSubjectCodes) {
                    Double semesterMarks = absentByCode.containsKey(subjectCode) ? null : semesterByCode.get(subjectCode);
                    Optional<Course> scheduledCourse = courseRepository.findByCode(subjectCode);
                    String scheduledCourseName = scheduledCourse.map(Course::getName).orElse(subjectCode);
                    if (!hasSemesterExamScheduled(subjectCode, scheduledCourseName)) {
                        logs.add("⚠️ Skipped unscheduled course " + subjectCode + " for " + email + ".");
                        continue;
                    }
                    double internalMarks = internalByCode.containsKey(subjectCode)
                            ? internalByCode.get(subjectCode)
                            : resolveInternalMarksFromExisting(student, subjectCode, rowSemester);

                    saveOrUpdateResult(student, subjectCode, rowSemester, internalMarks, semesterMarks);
                    hasResults = true;
                }

                if (hasResults && student.getStudentDetails() != null) {
                    List<Result> semesterResults = resultRepository.findByStudent(student).stream()
                            .filter(result -> rowSemester.equals(result.getSemester()))
                            .collect(Collectors.toList());
                    Double sgpa = calculateSgpa(semesterResults);
                    student.getStudentDetails().setSgpa(sgpa);
                    student.getStudentDetails().setGpa(sgpa);
                    student.getStudentDetails().setArrearCount(calculateArrearCount(semesterResults));
                    userRepository.save(student);
                    logs.add("âœ… " + email + ": Updated Results & SGPA: " + (sgpa != null ? sgpa : "N/A"));

                    try {
                        notificationService.createNotification(
                                student.getFirebaseUid(),
                                "RESULT_PUBLISHED",
                                "Semester Results Published",
                                "The Controller of Examinations has officially released your latest semester results.",
                                "/student/results"
                        );
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            logs.add("â— File Error: " + e.getMessage());
            e.printStackTrace();
        }

        return logs;
    }

    public List<Map<String, Object>> getEntrySheet(String dept, Integer sem) {
        List<User> students = getStudentsForDepartmentAndSemester(dept, sem);
        List<Map<String, Object>> response = new ArrayList<>();

        for (User student : students) {
            List<Section> sections = getStudentSemesterSections(student, dept, sem);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("studentId", student.getId());
            row.put("studentUid", student.getFirebaseUid());
            row.put("studentEmail", student.getEmail());
            row.put("registerNumber", student.getStudentDetails() != null ? student.getStudentDetails().getRollNumber() : "");
            row.put("studentName", student.getFullName());
            row.put("department", student.getStudentDetails() != null
                    ? (student.getStudentDetails().getDepartment() != null
                    ? student.getStudentDetails().getDepartment()
                    : student.getStudentDetails().getBranchName())
                    : dept);
            row.put("semester", student.getStudentDetails() != null && student.getStudentDetails().getSemester() != null
                    ? student.getStudentDetails().getSemester()
                    : sem);

            List<Map<String, Object>> subjects = new ArrayList<>();
            double weightedPoints = 0;
            int totalCredits = 0;
            boolean hasArrear = false;

            for (Section section : sections) {
                String subjectCode = section.getCourse().getCode();
                double internalMarks = resolveInternalMarks(student, section, sem);
                Result existing = resultRepository.findByStudentAndSemesterAndSubjectCode(student, sem, subjectCode).orElse(null);
                Double semesterMarks = existing != null ? existing.getSemesterMarks() : null;
                double totalMarks = calculateFinalMarks(internalMarks, semesterMarks);
                String grade = calculateGrade(totalMarks, semesterMarks);
                String resultStatus = calculateResultStatus(semesterMarks, grade);
                int gradePoints = getGradePoints(grade);
                int credits = section.getCourse().getCredits() != null ? section.getCourse().getCredits() : 3;

                if ("RA".equals(grade)) {
                    hasArrear = true;
                } else if (!"AB".equals(grade)) {
                    totalCredits += credits;
                    weightedPoints += (double) gradePoints * credits;
                }

                Map<String, Object> subject = new LinkedHashMap<>();
                subject.put("sectionId", section.getId());
                subject.put("subjectCode", subjectCode);
                subject.put("subjectName", section.getCourse().getName());
                subject.put("credits", credits);
                subject.put("internalMarks", round2(internalMarks));
                subject.put("semesterMarks", semesterMarks == null ? null : round2(semesterMarks));
                subject.put("convertedSemesterMarks", convertSemesterMarksToSixty(semesterMarks));
                subject.put("totalMarks", totalMarks);
                subject.put("finalPercentage", totalMarks);
                subject.put("grade", grade);
                subject.put("resultStatus", resultStatus);
                subjects.add(subject);
            }

            if (subjects.isEmpty()) {
                subjects = buildFallbackSubjectsFromInternalMarks(student, sem);
                for (Map<String, Object> subject : subjects) {
                    int credits = ((Number) subject.get("credits")).intValue();
                    String grade = String.valueOf(subject.get("grade"));
                    if ("RA".equals(grade)) {
                        hasArrear = true;
                    } else if (!"AB".equals(grade)) {
                        totalCredits += credits;
                        weightedPoints += getGradePoints(grade) * credits;
                    }
                }
            }

            row.put("subjects", subjects);
            row.put("sgpa", hasArrear || totalCredits == 0 ? null : round2(weightedPoints / totalCredits));
            response.add(row);
        }

        return response;
    }

    @Transactional
    public List<String> publishManualResults(String dept, Integer sem, List<Map<String, Object>> studentsPayload) {
        List<String> logs = new ArrayList<>();

        for (Map<String, Object> studentRow : studentsPayload) {
            String studentUid = String.valueOf(studentRow.get("studentUid"));
            if (studentUid == null || studentUid.isBlank()) {
                continue;
            }

            User student = userRepository.findByFirebaseUid(studentUid)
                    .orElseThrow(() -> new RuntimeException("Student not found: " + studentUid));

            Object subjectsObject = studentRow.get("subjects");
            if (!(subjectsObject instanceof List<?> subjectsList)) {
                continue;
            }

            boolean hasResults = false;

            for (Object subjectObject : subjectsList) {
                if (!(subjectObject instanceof Map<?, ?> rawMap)) {
                    continue;
                }

                String subjectCode = String.valueOf(rawMap.get("subjectCode"));
                if (subjectCode == null || subjectCode.isBlank()) {
                    continue;
                }

                double internalMarks = rawMap.get("internalMarks") == null ? 0.0
                        : Double.parseDouble(String.valueOf(rawMap.get("internalMarks")));
                Double semesterMarks = rawMap.get("semesterMarks") == null
                        || String.valueOf(rawMap.get("semesterMarks")).isBlank()
                        ? null
                        : Double.parseDouble(String.valueOf(rawMap.get("semesterMarks")));

                saveOrUpdateResult(student, subjectCode, sem, internalMarks, semesterMarks);
                hasResults = true;
            }

            if (hasResults && student.getStudentDetails() != null) {
                List<Result> semesterResults = resultRepository.findByStudent(student).stream()
                        .filter(result -> sem.equals(result.getSemester()))
                        .collect(Collectors.toList());
                Double sgpa = calculateSgpa(semesterResults);
                student.getStudentDetails().setSgpa(sgpa);
                student.getStudentDetails().setGpa(sgpa);
                student.getStudentDetails().setArrearCount(calculateArrearCount(semesterResults));
                userRepository.save(student);
                logs.add("âœ… " + student.getEmail() + ": Published results with SGPA " + (sgpa != null ? sgpa : "N/A"));

                try {
                    notificationService.createNotification(
                            student.getFirebaseUid(),
                            "RESULT_PUBLISHED",
                            "Semester Results Published",
                            "The Controller of Examinations has officially released your latest semester results.",
                            "/student/results"
                    );
                } catch (Exception ignored) {
                }
            }
        }

        if (logs.isEmpty()) {
            logs.add("âš ï¸ No student results were published for " + dept + " semester " + sem + ".");
        }
        return logs;
    }

    private String calculateGrade(double marks, Double semesterMarks) {
        if (semesterMarks == null) {
            return "AB";
        }
        if (marks >= 91) {
            return "O";
        }
        if (marks >= 81) {
            return "A+";
        }
        if (marks >= 71) {
            return "A";
        }
        if (marks >= 61) {
            return "B+";
        }
        if (marks >= 50) {
            return "B";
        }
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
        return resultRepository.findByStudent(student).stream()
                .map(this::normalizeResult)
                .collect(Collectors.toList());
    }

    public List<Result> getRecentPublishedResults() {
        return resultRepository.findTop50ByOrderByPublishedDateDesc();
    }

    public List<Map<String, Object>> getSGPAHistory(String uid) {
        Optional<User> userOpt = userRepository.findByFirebaseUid(uid);
        if (userOpt.isEmpty()) {
            try {
                userOpt = userRepository.findById(Long.parseLong(uid));
            } catch (NumberFormatException e) {
                return new ArrayList<>();
            }
        }

        if (userOpt.isEmpty()) {
            return new ArrayList<>();
        }
        User student = userOpt.get();

        List<Result> allResults = resultRepository.findByStudent(student);
        Map<Integer, List<Result>> resultsBySemester = allResults.stream()
                .filter(r -> r.getSemester() != null)
                .collect(Collectors.groupingBy(Result::getSemester, TreeMap::new, Collectors.toList()));

        List<Map<String, Object>> history = new ArrayList<>();
        for (Map.Entry<Integer, List<Result>> entry : resultsBySemester.entrySet()) {
            Integer semester = entry.getKey();
            List<Result> semesterResults = entry.getValue();
            Double sgpa = calculateSgpa(semesterResults);

            Map<String, Object> semData = new HashMap<>();
            semData.put("semester", toRoman(semester));
            semData.put("semNumber", semester);
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
