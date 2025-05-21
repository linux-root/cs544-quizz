package com.example.coolproject.repository;

import com.example.coolproject.entity.StudentAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentActionRepository extends JpaRepository<StudentAction, Long> {
    // You can add custom query methods here if needed in the future
} 