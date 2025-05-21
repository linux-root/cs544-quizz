package com.example.coolproject.repository;

import com.example.coolproject.entity.StudentAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.coolproject.entity.QuizzSession;
import com.example.coolproject.entity.Student;
import com.example.coolproject.entity.StudentActionType;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentActionRepository extends JpaRepository<StudentAction, Long> {
    // You can add custom query methods here if needed in the future
    List<StudentAction> findByQuizzSessionAndActionTypeOrderByStartTimestampAsc(QuizzSession quizzSession, StudentActionType actionType);

    Optional<StudentAction> findFirstByQuizzSessionAndStudentAndActionTypeOrderByStartTimestampAsc(
        QuizzSession quizzSession, Student student, StudentActionType actionType);

    Optional<StudentAction> findFirstByQuizzSessionAndStudentOrderByEndTimestampDesc(QuizzSession quizzSession, Student student);
} 