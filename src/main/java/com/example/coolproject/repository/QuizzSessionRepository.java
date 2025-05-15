package com.example.coolproject.repository;

import com.example.coolproject.entity.Quizz;
import com.example.coolproject.entity.QuizzSession;
import com.example.coolproject.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizzSessionRepository extends JpaRepository<QuizzSession, Long> {
    List<QuizzSession> findByQuizz(Quizz quizz);
    List<QuizzSession> findByStatus(QuizzSession.SessionStatus status);
    List<QuizzSession> findByParticipantsContaining(Student student);
} 