package com.example.coolproject.service;

import com.example.coolproject.dto.StudentActionDTO;
import com.example.coolproject.entity.*;
import com.example.coolproject.repository.QuestionRepository;
import com.example.coolproject.repository.QuizzSessionRepository;
import com.example.coolproject.repository.StudentActionRepository;
import com.example.coolproject.repository.StudentRepository;
import com.example.coolproject.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class StudentActionService {

    private final StudentActionRepository studentActionRepository;
    private final StudentRepository studentRepository;
    private final QuizzSessionRepository quizzSessionRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    @Autowired
    public StudentActionService(StudentActionRepository studentActionRepository,
                                StudentRepository studentRepository,
                                QuizzSessionRepository quizzSessionRepository,
                                QuestionRepository questionRepository,
                                UserRepository userRepository) {
        this.studentActionRepository = studentActionRepository;
        this.studentRepository = studentRepository;
        this.quizzSessionRepository = quizzSessionRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public StudentAction createStudentAction(StudentActionDTO dto, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + userEmail));
        
        if (!(user instanceof Student)) {
            throw new IllegalArgumentException("User with email " + userEmail + " is not a Student.");
        }
        Student student = (Student) user;

        QuizzSession quizzSession = quizzSessionRepository.findById(dto.getSessionId())
                .orElseThrow(() -> new RuntimeException("QuizzSession not found with id: " + dto.getSessionId()));

        Question question = null;
        if (dto.getQuestionId() != null) {
            question = questionRepository.findById(dto.getQuestionId())
                    .orElseThrow(() -> new RuntimeException("Question not found with id: " + dto.getQuestionId()));
        }

        StudentAction studentAction = new StudentAction();
        studentAction.setActionType(dto.getActionType());
        studentAction.setStartTimestamp(dto.getStartTimestamp() != null ? dto.getStartTimestamp() : LocalDateTime.now());
        studentAction.setEndTimestamp(dto.getEndTimestamp() != null ? dto.getEndTimestamp() : LocalDateTime.now());
        
        studentAction.setStudent(student);
        studentAction.setQuizzSession(quizzSession);
        studentAction.setQuestion(question);
        studentAction.setActionValue(dto.getActionValue());

        return studentActionRepository.save(studentAction);
    }
} 