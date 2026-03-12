package com.academic.platform.repository;

import com.academic.platform.model.Quiz;
import com.academic.platform.model.QuizAttempt;
import com.academic.platform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    Optional<QuizAttempt> findByQuizAndStudent(Quiz quiz, User student);

    List<QuizAttempt> findByStudentOrderBySubmittedAtDesc(User student);

    void deleteByQuiz(Quiz quiz);
}
