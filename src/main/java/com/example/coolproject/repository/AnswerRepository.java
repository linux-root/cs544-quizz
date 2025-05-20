package com.example.coolproject.repository;

import com.example.coolproject.entity.Answer;
import com.example.coolproject.entity.Question;
import com.example.coolproject.entity.QuizzSession;
import com.example.coolproject.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {
    Optional<Answer> findByQuizzSessionAndStudentAndQuestion(QuizzSession quizzSession, Student student, Question question);

    List<Answer> findByQuizzSessionAndStudent(QuizzSession quizzSession, Student student);

    // You can add custom query methods here if needed in the future
} 