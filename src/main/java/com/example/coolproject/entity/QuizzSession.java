package com.example.coolproject.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
public class QuizzSession {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne()
  @JoinColumn(name = "quizz_id", referencedColumnName = "id")
  private Quizz quizz;

  private LocalDateTime scheduledStartTime;
  private LocalDateTime actualStartTime;
  private LocalDateTime endTime;

  @Enumerated(EnumType.STRING)
  private SessionStatus status = SessionStatus.CREATED;

  @ManyToMany
  @JoinTable(name = "quizz_session_participants", joinColumns = @JoinColumn(name = "session_id"), inverseJoinColumns = @JoinColumn(name = "student_id"))
  private Set<Student> participants = new HashSet<>();

  // Enum for session status
  public enum SessionStatus {
    CREATED,
    SCHEDULED,
    OPEN,
    CLOSED
  }

  // Getters and Setters
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Quizz getQuizz() {
    return quizz;
  }

  public void setQuizz(Quizz quizz) {
    this.quizz = quizz;
  }

  public LocalDateTime getScheduledStartTime() {
    return scheduledStartTime;
  }

  public void setScheduledStartTime(LocalDateTime scheduledStartTime) {
    this.scheduledStartTime = scheduledStartTime;
  }

  public LocalDateTime getActualStartTime() {
    return actualStartTime;
  }

  public void setActualStartTime(LocalDateTime actualStartTime) {
    this.actualStartTime = actualStartTime;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }

  public SessionStatus getStatus() {
    return status;
  }

  public void setStatus(SessionStatus status) {
    this.status = status;
  }

  public Set<Student> getParticipants() {
    return participants;
  }

  public void setParticipants(Set<Student> participants) {
    this.participants = participants;
  }

  public void addParticipant(Student student) {
    participants.add(student);
  }
}
