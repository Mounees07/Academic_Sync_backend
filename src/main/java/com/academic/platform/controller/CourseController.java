package com.academic.platform.controller;

import com.academic.platform.model.Course;
import com.academic.platform.model.Section;
import com.academic.platform.model.Enrollment;
import com.academic.platform.model.Lesson;
import com.academic.platform.model.Quiz;
import com.academic.platform.model.Question;
import com.academic.platform.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @Autowired
    private CourseService courseService;

    @PostMapping
    public ResponseEntity<Course> createCourse(@RequestBody Course course) {
        return ResponseEntity.ok(courseService.createCourse(course));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Course> updateCourse(@PathVariable Long id, @RequestBody Course course) {
        return ResponseEntity.ok(courseService.updateCourse(id, course));
    }

    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @GetMapping("/department/{department}")
    public ResponseEntity<List<Course>> getCoursesByDepartment(@PathVariable String department) {
        return ResponseEntity.ok(courseService.getCoursesByDepartment(department));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean cascade) {
        try {
            courseService.deleteCourse(id, cascade);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/sections")
    public ResponseEntity<Section> createSection(@RequestBody Section section, @RequestParam String facultyUid) {
        return ResponseEntity.ok(courseService.createSection(section, facultyUid));
    }

    @GetMapping("/sections")
    public ResponseEntity<List<Section>> getAllSections() {
        return ResponseEntity.ok(courseService.getAllSections());
    }

    @GetMapping("/sections/{id}")
    public ResponseEntity<Section> getSectionById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getSectionById(id));
    }

    @PatchMapping("/sections/{id}/toggle-tests")
    public ResponseEntity<Section> toggleSectionTests(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.toggleSectionTests(id));
    }

    @GetMapping("/sections/faculty/{facultyUid}")
    public ResponseEntity<List<Section>> getFacultySections(@PathVariable String facultyUid) {
        return ResponseEntity.ok(courseService.getSectionsByFaculty(facultyUid));
    }

    @PostMapping("/enroll")
    public ResponseEntity<?> enrollStudent(@RequestParam Long sectionId, @RequestParam String studentUid) {
        try {
            return ResponseEntity.ok(courseService.enrollStudent(sectionId, studentUid));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("message", e.getMessage()));
        }
    }

    @GetMapping("/enrollments/student/{studentUid}")
    public ResponseEntity<List<Enrollment>> getStudentEnrollments(@PathVariable String studentUid) {
        return ResponseEntity.ok(courseService.getStudentEnrollments(studentUid));
    }

    @GetMapping("/sections/{sectionId}/enrollments")
    public List<Enrollment> getSectionEnrollments(@PathVariable Long sectionId) {
        return courseService.getSectionEnrollments(sectionId);
    }

    // Lessons
    @GetMapping("/{courseId}/lessons")
    public List<Lesson> getCourseLessons(@PathVariable Long courseId) {
        return courseService.getCourseLessons(courseId);
    }

    @PostMapping("/{courseId}/lessons")
    public Lesson addLesson(@PathVariable Long courseId, @RequestBody Lesson lesson) {
        return courseService.addLesson(courseId, lesson);
    }

    @DeleteMapping("/lessons/{lessonId}")
    public ResponseEntity<?> deleteLesson(@PathVariable Long lessonId) {
        courseService.deleteLesson(lessonId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/lessons/{lessonId}")
    public ResponseEntity<Lesson> updateLesson(@PathVariable Long lessonId, @RequestBody Lesson lesson) {
        return ResponseEntity.ok(courseService.updateLesson(lessonId, lesson));
    }

    // Quizzes
    @GetMapping("/{courseId}/quizzes")
    public List<Quiz> getCourseQuizzes(@PathVariable Long courseId) {
        return courseService.getCourseQuizzes(courseId);
    }

    @PostMapping("/{courseId}/quizzes")
    public ResponseEntity<Quiz> createQuiz(@PathVariable Long courseId, @RequestBody Quiz quiz) {
        return ResponseEntity.ok(courseService.createQuiz(courseId, quiz));
    }

    @PostMapping("/quizzes/{quizId}/questions")
    public ResponseEntity<Question> addQuestion(@PathVariable Long quizId, @RequestBody Question question) {
        return ResponseEntity.ok(courseService.addQuestion(quizId, question));
    }

    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long questionId) {
        courseService.deleteQuestion(questionId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/quizzes/{quizId}")
    public ResponseEntity<?> deleteQuiz(@PathVariable Long quizId) {
        courseService.deleteQuiz(quizId);
        return ResponseEntity.ok().build();
    }
}
