package com.example.coolproject.repository;

import com.example.coolproject.entity.Question;
import com.example.coolproject.entity.Quizz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByQuizzOrderByOrderIndex(Quizz quizz);
} 