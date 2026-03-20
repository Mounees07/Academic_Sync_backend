package com.academic.platform.service;

import com.academic.platform.model.Course;
import com.academic.platform.model.Section;
import com.academic.platform.model.Enrollment;
import com.academic.platform.model.Assignment;
import com.academic.platform.model.Submission;
import com.academic.platform.model.Announcement;
import com.academic.platform.model.User;
import com.academic.platform.model.Lesson;
import com.academic.platform.model.Quiz;
import com.academic.platform.model.Question;
import com.academic.platform.model.QuizAttempt;

import com.academic.platform.repository.CourseRepository;
import com.academic.platform.repository.SectionRepository;
import com.academic.platform.repository.EnrollmentRepository;
import com.academic.platform.repository.AssignmentRepository;
import com.academic.platform.repository.SubmissionRepository;
import com.academic.platform.repository.AnnouncementRepository;
import com.academic.platform.repository.UserRepository;
import com.academic.platform.repository.LessonRepository;
import com.academic.platform.repository.QuizRepository;
import com.academic.platform.repository.QuestionRepository;
import com.academic.platform.repository.QuizAttemptRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private AssignmentRepository assignmentRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private UserRepository userRepository;

    public Course createCourse(Course course) {
        if (courseRepository.findByCode(course.getCode()).isPresent()) {
            throw new RuntimeException("Course with code " + course.getCode() + " already exists.");
        }
        return courseRepository.save(course);
    }

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public List<Course> getCoursesByDepartment(String department) {
        return courseRepository.findByDepartmentIgnoreCase(department);
    }

    public Course updateCourse(Long id, Course details) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        if (details.getDescription() != null)
            course.setDescription(details.getDescription());
        if (details.getThumbnailUrl() != null)
            course.setThumbnailUrl(details.getThumbnailUrl());
        if (details.getDifficultyLevel() != null)
            course.setDifficultyLevel(details.getDifficultyLevel());
        return courseRepository.save(course);
    }

    public void deleteCourse(Long courseId, boolean cascade) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        List<Section> sections = sectionRepository.findByCourseId(courseId);

        if (!sections.isEmpty()) {
            if (!cascade) {
                throw new RuntimeException("Cannot delete course. It has " + sections.size() +
                        " active sections. Use force delete to remove all sections and enrollments.");
            }
            // Cascade Delete Logic: Delete Enrollments -> Delete Assignments (and
            // Submissions) -> Delete Sections
            for (Section section : sections) {
                // 1. Delete Enrollments
                List<Enrollment> enrollments = enrollmentRepository.findBySection(section);
                enrollmentRepository.deleteAll(enrollments);

                // 2. Delete Assignments (and their Submissions)
                List<Assignment> assignments = assignmentRepository.findBySection(section);
                for (Assignment assignment : assignments) {
                    List<Submission> submissions = submissionRepository.findByAssignment(assignment);
                    submissionRepository.deleteAll(submissions);
                }
                assignmentRepository.deleteAll(assignments);

                // 3. Delete Announcements
                List<Announcement> announcements = announcementRepository.findByTargetSection(section);
                announcementRepository.deleteAll(announcements);
            }
            // 4. Delete Sections
            sectionRepository.deleteAll(sections);
        }

        courseRepository.delete(course);
    }

    public Section createSection(Section section, String facultyUid) {
        User faculty = userRepository.findByFirebaseUid(facultyUid)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
        section.setFaculty(faculty);

        // Check for duplicates
        if (sectionRepository.existsByCourseAndFacultyAndSemesterAndYear(
                section.getCourse(), faculty, section.getSemester(), section.getYear())) {
            throw new RuntimeException("This faculty is already assigned to this course for the selected semester.");
        }

        return sectionRepository.save(section);
    }

    public List<Section> getAllSections() {
        return sectionRepository.findAll();
    }

    public Section getSectionById(Long id) {
        return sectionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Section not found"));
    }

    public Section toggleSectionTests(Long sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Section not found"));
        section.setTestsEnabled(!section.getTestsEnabled());
        return sectionRepository.save(section);
    }

    public Section updateSyllabusCompletion(Long sectionId, Integer completion) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Section not found"));
        int normalizedCompletion = Math.max(0, Math.min(100, completion == null ? 0 : completion));
        section.setSyllabusCompletion(normalizedCompletion);
        return sectionRepository.save(section);
    }

    public List<Section> getSectionsByFaculty(String facultyUid) {
        User faculty = userRepository.findByFirebaseUid(facultyUid)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));
        return sectionRepository.findByFaculty(faculty);
    }

    public Enrollment enrollStudent(Long sectionId, String studentUid) {
        Section newSection = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new RuntimeException("Section not found"));

        User student = userRepository.findByFirebaseUid(studentUid)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        // Check if student is already enrolled in ANY section of this course
        Optional<Enrollment> existingOpt = enrollmentRepository.findByStudentAndSection_Course(student,
                newSection.getCourse());

        if (existingOpt.isPresent()) {
            Enrollment existing = existingOpt.get();

            // If already enrolled in the SAME section
            if (existing.getSection().getId().equals(newSection.getId())) {
                throw new RuntimeException("Student already enrolled in this faculty");
            }

            // Logic for CHANGING faculty
            int currentChanges = existing.getChangeCount() == null ? 0 : existing.getChangeCount();

            // 1. Check Max Limit (2 changes allowed)
            if (currentChanges >= 2) {
                throw new RuntimeException(
                        "Maximum faculty changes (2) limit reached. You cannot change faculty anymore.");
            }

            // 2. Check 24 hour freeze
            // Use lastUpdatedDate if present, otherwise enrollmentDate
            LocalDateTime referencetime = existing.getLastUpdatedDate() != null ? existing.getLastUpdatedDate()
                    : existing.getEnrollmentDate();

            if (referencetime == null) {
                referencetime = LocalDateTime.now().minusDays(2);
            }

            if (referencetime.plusHours(24).isAfter(LocalDateTime.now())) {
                long hoursLeft = java.time.Duration.between(LocalDateTime.now(), referencetime.plusHours(24)).toHours();
                throw new RuntimeException(
                        "Faculty selection is frozen. You can change again in " + (hoursLeft + 1) + " hours.");
            }

            // Apply Change
            existing.setSection(newSection);
            existing.setChangeCount(currentChanges + 1);
            existing.setLastUpdatedDate(LocalDateTime.now());

            return enrollmentRepository.save(existing);
        }

        // New Enrollment
        Enrollment enrollment = Enrollment.builder()
                .student(student)
                .section(newSection)
                .enrollmentDate(LocalDateTime.now())
                .changeCount(0)
                .build();

        return enrollmentRepository.save(enrollment);
    }

    public List<Enrollment> getSectionEnrollments(Long sectionId) {
        return enrollmentRepository.findBySectionId(sectionId);
    }

    // Lesson Management
    @Autowired
    private LessonRepository lessonRepository;

    public List<com.academic.platform.model.Lesson> getCourseLessons(Long courseId) {
        return lessonRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
    }

    public com.academic.platform.model.Lesson addLesson(Long courseId, com.academic.platform.model.Lesson lesson) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        lesson.setCourse(course);
        return lessonRepository.save(lesson);
    }

    public void deleteLesson(Long lessonId) {
        if (!lessonRepository.existsById(lessonId)) {
            throw new RuntimeException("Lesson not found");
        }
        lessonRepository.deleteById(lessonId);
    }

    public com.academic.platform.model.Lesson updateLesson(Long lessonId, com.academic.platform.model.Lesson details) {
        com.academic.platform.model.Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));
        lesson.setTitle(details.getTitle());
        lesson.setContentUrl(details.getContentUrl());
        lesson.setContentType(details.getContentType());
        if (details.getDescription() != null)
            lesson.setDescription(details.getDescription());
        return lessonRepository.save(lesson);
    }

    // Quiz Management
    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    public List<com.academic.platform.model.Quiz> getCourseQuizzes(Long courseId) {
        return quizRepository.findByCourseId(courseId);
    }

    public Quiz createQuiz(Long courseId, Quiz quiz) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        quiz.setCourse(course);
        return quizRepository.save(quiz);
    }

    public Question addQuestion(Long quizId, Question question) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        question.setQuiz(quiz);
        return questionRepository.save(question);
    }

    public void deleteQuestion(Long questionId) {
        questionRepository.deleteById(questionId);
    }

    public void deleteQuiz(Long quizId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        quizAttemptRepository.deleteByQuiz(quiz);
        quizRepository.delete(quiz);
    }

    public List<Enrollment> getStudentEnrollments(String studentUid) {
        User student = userRepository.findByFirebaseUid(studentUid)
                .orElseThrow(() -> new RuntimeException("Student not found"));
        return enrollmentRepository.findByStudent(student);
    }

    public Map<String, Object> recordQuizAttempt(Long quizId, String studentUid, int score, int total, int percentage) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));
        User student = userRepository.findByFirebaseUid(studentUid)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        QuizAttempt attempt = quizAttemptRepository.findByQuizAndStudent(quiz, student)
                .orElseGet(QuizAttempt::new);

        attempt.setQuiz(quiz);
        attempt.setStudent(student);
        attempt.setScore(score);
        attempt.setTotal(total);
        attempt.setPercentage(percentage);
        attempt.setSubmittedAt(LocalDateTime.now());

        QuizAttempt saved = quizAttemptRepository.save(attempt);
        return mapQuizAttempt(saved);
    }

    public List<Map<String, Object>> getStudentQuizAttempts(String studentUid) {
        User student = userRepository.findByFirebaseUid(studentUid)
                .orElseThrow(() -> new RuntimeException("Student not found"));

        return quizAttemptRepository.findByStudentOrderBySubmittedAtDesc(student).stream()
                .map(this::mapQuizAttempt)
                .toList();
    }

    private Map<String, Object> mapQuizAttempt(QuizAttempt attempt) {
        Quiz quiz = attempt.getQuiz();
        Course course = quiz.getCourse();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", attempt.getId());
        response.put("quizId", quiz.getId());
        response.put("title", quiz.getTitle());
        response.put("courseId", course.getId());
        response.put("subjectCode", course.getCode());
        response.put("subjectName", course.getName());
        response.put("score", attempt.getScore());
        response.put("total", attempt.getTotal());
        response.put("percentage", attempt.getPercentage());
        response.put("submittedAt", attempt.getSubmittedAt());
        return response;
    }

}
