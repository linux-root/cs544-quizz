package com.example.coolproject.repository;

import com.example.coolproject.entity.Quizz;
import com.example.coolproject.entity.QuizzSession;
import com.example.coolproject.entity.Student;
import com.example.coolproject.entity.Professor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface QuizzSessionRepository extends JpaRepository<QuizzSession, Long> {
    List<QuizzSession> findByQuizz(Quizz quizz);
    List<QuizzSession> findByStatus(QuizzSession.SessionStatus status);
    List<QuizzSession> findByParticipantsContaining(Student student);
    List<QuizzSession> findByStatusAndScheduledStartTimeLessThanEqual(QuizzSession.SessionStatus status, LocalDateTime scheduledStartTime);
    boolean existsByQuizz_CreatorAndStatus(Professor professor, QuizzSession.SessionStatus status);
} 