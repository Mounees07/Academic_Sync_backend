package com.academic.platform.service;

import com.academic.platform.model.AcademicSchedule;
import com.academic.platform.model.ExamSeating;
import com.academic.platform.model.ExamVenue;
import com.academic.platform.model.Role;
import com.academic.platform.model.User;
import com.academic.platform.repository.AcademicScheduleRepository;
import com.academic.platform.repository.ExamSeatingRepository;
import com.academic.platform.repository.ExamVenueRepository;
import com.academic.platform.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ExamSeatingService {

    @Autowired
    private ExamSeatingRepository examSeatingRepository;

    @Autowired
    private AcademicScheduleRepository academicScheduleRepository;

    @Autowired
    private ExamVenueRepository examVenueRepository;

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager em;

    // ─────────────────────────────────────────────────────────────────────────
    // CSV-based manual allocation (existing)
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public List<ExamSeating> processSeatingUpload(Long examId, MultipartFile file) throws Exception {
        AcademicSchedule exam = academicScheduleRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found"));

        List<ExamVenue> allVenues = examVenueRepository.findAll();
        Map<String, ExamVenue> venueMap = allVenues.stream()
                .collect(Collectors.toMap(v -> v.getName().trim().toLowerCase(), v -> v, (v1, v2) -> v1));

        List<ExamSeating> allocations = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (lineNum == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }

                if (line.trim().isEmpty())
                    continue;

                String[] parts = line.split("[,;]");
                if (parts.length < 2)
                    continue;

                String rollNoInput = parts[0].trim();
                String venueName = parts[1].trim();
                String seatNo = parts.length > 2 ? parts[2].trim() : null;

                if (rollNoInput.isEmpty() || rollNoInput.toLowerCase().startsWith("roll"))
                    continue;

                ExamVenue venue = venueMap.get(venueName.toLowerCase());
                if (venue == null) {
                    venue = new ExamVenue();
                    venue.setName(venueName);
                    venue.setBlock("Allocated Block");
                    venue.setCapacity(100);
                    venue.setExamType("All");
                    venue.setAvailable(true);
                    venue = examVenueRepository.save(venue);
                    venueMap.put(venueName.toLowerCase(), venue);
                }

                List<User> students = new ArrayList<>();
                String[] rangeParts = rollNoInput.split("[-=]");
                if (rangeParts.length == 2) {
                    String start = rangeParts[0].trim();
                    String end = rangeParts[1].trim();
                    List<User> rangeStudents = userRepository.findByStudentDetails_RollNumberBetween(start, end);
                    if (rangeStudents.isEmpty()) {
                        errors.add("Line " + lineNum + ": No students found in range " + start + " to " + end);
                    } else {
                        students.addAll(rangeStudents);
                    }
                } else {
                    User s = userRepository.findByStudentDetails_RollNumber(rollNoInput).orElse(null);
                    if (s == null) {
                        errors.add("Line " + lineNum + ": Student " + rollNoInput + " not found.");
                    } else {
                        students.add(s);
                    }
                }

                for (User student : students) {
                    ExamSeating seating = ExamSeating.builder()
                            .exam(exam).venue(venue).student(student).seatNumber(seatNo).build();
                    allocations.add(seating);
                }
            }
        }

        if (!errors.isEmpty()) {
            String errorMsg = errors.stream().limit(5).collect(Collectors.joining("; "));
            if (errors.size() > 5)
                errorMsg += "... (" + (errors.size() - 5) + " more errors)";
            throw new RuntimeException("Validation failed. Errors: " + errorMsg);
        }

        if (allocations.isEmpty()) {
            throw new RuntimeException("No valid data found in CSV. Check format.");
        }

        // Delete old allocations then flush so MySQL sees the DELETE before INSERT
        examSeatingRepository.deleteAllByExamIdDirect(examId);
        em.flush();
        em.clear();

        return examSeatingRepository.saveAll(allocations);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // AUTO-ALLOCATE — alternating dept + section interleaving
    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Algorithm:
     * 1. Fetch ALL students with studentDetails.
     * 2. Filter by exam.department if set (internal exam) — otherwise all (semester exam).
     * 3. Group by (department + section) combined key — e.g. "CSE||A", "IT||B".
     * 4. Max-heap interleave groups → adjacent seats NEVER share same dept+section.
     *    Students from the same section sit neither front/back/side of each other.
     * 5. Assign seat labels A1…A<cols>, B1…B<cols> across venues sorted by capacity (largest first).
     */
    @Transactional
    public List<ExamSeating> autoAllocate(Long examId) {

        // ── STEP 0: Delete existing allocations for this exam FIRST
        examSeatingRepository.deleteAllByExamIdDirect(examId);
        em.flush();

        // ── STEP 1: Load the exam
        AcademicSchedule exam = academicScheduleRepository.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found: " + examId));

        // ── STEP 2: Fetch only true STUDENT users
        List<User> allStudents = userRepository.findByRole(Role.STUDENT).stream()
                .filter(u -> u.getStudentDetails() != null
                        && u.getStudentDetails().getDepartment() != null
                        && !u.getStudentDetails().getDepartment().isBlank())
                .collect(Collectors.toList());

        // Filter by exam department for internal exams
        String examDept = exam.getDepartment();
        if (examDept != null && !examDept.isBlank() && 
            !examDept.trim().equalsIgnoreCase("General") && 
            !examDept.trim().equalsIgnoreCase("COE") && 
            !examDept.trim().equalsIgnoreCase("Admin") && 
            !examDept.trim().equalsIgnoreCase("All")) {
            final String dept = examDept.trim().toUpperCase();
            allStudents = allStudents.stream()
                    .filter(u -> dept.equalsIgnoreCase(u.getStudentDetails().getDepartment().trim()))
                    .collect(Collectors.toList());
        }

        if (allStudents.isEmpty()) {
            throw new RuntimeException(
                    "No eligible students found for this exam. " +
                            "Ensure students have their Department set in their profile.");
        }

        // ── STEP 3: Group by DEPARTMENT + SECTION (combined key).
        // E.g. "CSE||A", "CSE||B", "IT||A" are separate groups.
        // This ensures same-section students are never adjacent to each other.
        Map<String, List<User>> byDeptSection = new LinkedHashMap<>();
        for (User u : allStudents) {
            String dept = u.getStudentDetails().getDepartment() != null
                    ? u.getStudentDetails().getDepartment().trim().toUpperCase()
                    : "UNKNOWN";
            String section = u.getStudentDetails().getSection() != null
                    ? u.getStudentDetails().getSection().trim().toUpperCase()
                    : "X";
            String key = dept + "||" + section;
            byDeptSection.computeIfAbsent(key, k -> new ArrayList<>()).add(u);
        }

        // Sort each group by roll number for deterministic ordering
        Map<String, Deque<User>> groupQueues = new LinkedHashMap<>();
        for (Map.Entry<String, List<User>> e : byDeptSection.entrySet()) {
            e.getValue().sort(Comparator.comparing(
                    u -> u.getStudentDetails().getRollNumber() == null
                            ? "" : u.getStudentDetails().getRollNumber()));
            groupQueues.put(e.getKey(), new ArrayDeque<>(e.getValue()));
        }

        // ── STEP 4: Max-heap Task-Scheduler across dept+section groups
        List<User> orderedStudents = taskSchedulerInterleave(groupQueues);

        // ── STEP 5: Load available venues (largest first)
        List<ExamVenue> venues = examVenueRepository.findAll().stream()
                .filter(ExamVenue::isAvailable)
                .sorted(Comparator.comparingInt(ExamVenue::getCapacity).reversed())
                .collect(Collectors.toList());

        if (venues.isEmpty()) {
            throw new RuntimeException(
                    "No available venues found. Please add venues in the Venues section first.");
        }

        // ── STEP 6: Build seat assignments — seats labelled A1, A2 … A<cols>, B1 …
        List<ExamSeating> allocations = new ArrayList<>();
        Set<Long> seenStudentIds = new HashSet<>();
        int studentIdx = 0;

        for (ExamVenue venue : venues) {
            int capacity = venue.getCapacity();
            int cols = computeColumns(capacity);
            int filled = 0;

            while (filled < capacity && studentIdx < orderedStudents.size()) {
                User student = orderedStudents.get(studentIdx);
                studentIdx++;

                if (student == null) {
                    if (filled > 0) filled++;
                    continue;
                }

                if (!seenStudentIds.add(student.getId()))
                    continue;

                int row = filled / cols;
                int col = filled % cols;
                String rowLabel = row < 26
                        ? String.valueOf((char) ('A' + row))
                        : "A" + (char) ('A' + (row - 26));
                String seatLabel = rowLabel + (col + 1);

                allocations.add(ExamSeating.builder()
                        .exam(exam)
                        .venue(venue)
                        .student(student)
                        .seatNumber(seatLabel)
                        .build());
                filled++;
            }
            if (studentIdx >= orderedStudents.size())
                break;
        }

        // Overflow — students beyond total venue capacity
        if (studentIdx < orderedStudents.size()) {
            ExamVenue lastVenue = venues.get(venues.size() - 1);
            int overflow = 1;
            while (studentIdx < orderedStudents.size()) {
                User student = orderedStudents.get(studentIdx++);
                if (student == null) continue;
                if (!seenStudentIds.add(student.getId()))
                    continue;
                allocations.add(ExamSeating.builder()
                        .exam(exam).venue(lastVenue).student(student)
                        .seatNumber("OVF-" + overflow++)
                        .build());
            }
        }

        if (allocations.isEmpty()) {
            throw new RuntimeException("Could not build any seat allocations. Check student data.");
        }

        return examSeatingRepository.saveAll(allocations);
    }

    /**
     * Task-Scheduler / Max-Heap interleave across DEPT+SECTION groups.
     *
     * Guarantees no two consecutive seat positions share the same
     * (department + section) group key. Concretely:
     *  - CSE-A never sits next to another CSE-A student
     *  - CSE-B may sit next to CSE-A (different section = different group)
     *  - The algorithm maximises the spread of each identical group
     *
     * When only one group remains it falls back to placing consecutive seats
     * from the same group — this is the mathematically unavoidable minimum.
     */
    private List<User> taskSchedulerInterleave(Map<String, Deque<User>> groupQueues) {
        // Max-heap: group with most remaining students polled first
        PriorityQueue<Map.Entry<String, Deque<User>>> pq = new PriorityQueue<>(
                (a, b) -> b.getValue().size() - a.getValue().size());

        for (Map.Entry<String, Deque<User>> e : groupQueues.entrySet()) {
            if (!e.getValue().isEmpty())
                pq.offer(e);
        }

        List<User> result = new ArrayList<>();
        String lastGroup = null;
        Map.Entry<String, Deque<User>> held = null;

        while (!pq.isEmpty() || held != null) {
            Map.Entry<String, Deque<User>> curr = pq.isEmpty() ? null : pq.poll();

            // Same group as last — hold aside and try next
            if (curr != null && curr.getKey().equals(lastGroup)) {
                held = curr;
                curr = pq.isEmpty() ? null : pq.poll();
            }

            if (curr == null) {
                // Have to pick from the same group as last time. Add a gap empty seat first.
                result.add(null);
                lastGroup = null; // Next seat is a gap, so any group is fine for the seat after that.
                
                curr = held;
                held = null;
            }
            if (curr == null) break;

            // Place one student from this group
            result.add(curr.getValue().poll());
            lastGroup = curr.getKey();

            // Return the held entry back to the heap
            if (held != null) {
                pq.offer(held);
                held = null;
            }

            // Return curr to heap if it still has students
            if (!curr.getValue().isEmpty()) {
                pq.offer(curr);
            }
        }
        return result;
    }

    /** Simple round-robin interleave used for within-dept section mixing. */
    private List<User> roundRobin(List<List<User>> groups) {
        List<Deque<User>> queues = groups.stream().map(ArrayDeque::new).collect(Collectors.toList());
        List<User> result = new ArrayList<>();
        boolean anyLeft = true;
        while (anyLeft) {
            anyLeft = false;
            for (Deque<User> q : queues) {
                if (!q.isEmpty()) {
                    result.add(q.poll());
                    anyLeft = true;
                }
            }
        }
        return result;
    }

    /** Max columns per row so seat labels stay readable. */
    private int computeColumns(int capacity) {
        if (capacity <= 30)
            return 5;
        if (capacity <= 60)
            return 10;
        if (capacity <= 100)
            return 10;
        return 12;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Query helpers
    // ─────────────────────────────────────────────────────────────────────────
    public List<ExamSeating> getSeatingByExam(Long examId) {
        return examSeatingRepository.findByExamId(examId);
    }

    public List<ExamSeating> getSeatingByStudent(Long studentId) {
        return examSeatingRepository.findByStudentId(studentId);
    }

    public List<ExamSeating> getSeatingByStudentUid(String uid) {
        return examSeatingRepository.findByStudentFirebaseUid(uid);
    }

    public List<ExamSeating> getAllAllocations() {
        return examSeatingRepository.findAll();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CRUD — Update single allocation (seat number and/or venue)
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public ExamSeating updateSeating(Long id, String seatNumber, Long venueId) {
        ExamSeating seating = examSeatingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Seating record not found: " + id));

        if (seatNumber != null && !seatNumber.isBlank()) {
            seating.setSeatNumber(seatNumber.trim().toUpperCase());
        }

        if (venueId != null) {
            ExamVenue venue = examVenueRepository.findById(venueId)
                    .orElseThrow(() -> new RuntimeException("Venue not found: " + venueId));
            seating.setVenue(venue);
        }

        return examSeatingRepository.save(seating);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CRUD — Delete single allocation
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public void deleteSingleSeating(Long id) {
        if (!examSeatingRepository.existsById(id)) {
            throw new RuntimeException("Seating record not found: " + id);
        }
        examSeatingRepository.deleteById(id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CRUD — Delete ALL allocations for an exam
    // ─────────────────────────────────────────────────────────────────────────
    @Transactional
    public void deleteAllByExam(Long examId) {
        examSeatingRepository.deleteAllByExamIdDirect(examId);
    }
}

