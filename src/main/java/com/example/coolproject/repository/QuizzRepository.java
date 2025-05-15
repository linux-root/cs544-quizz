package com.example.coolproject.repository;

import com.example.coolproject.entity.Professor;
import com.example.coolproject.entity.Quizz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizzRepository extends JpaRepository<Quizz, Long> {
    List<Quizz> findByCreator(Professor professor);
} 