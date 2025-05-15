package com.example.coolproject.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Set;

@Entity
@Table(name = "students")
public class Student extends User {

    @Column(unique = true)
    private String githubId;

    private String avatarUrl;

    public Student() {
        super();
    }

    public Student(String email, String name, String githubId, String avatarUrl, Set<String> roles) {
        super(email, name, roles);
        this.githubId = githubId;
        this.avatarUrl = avatarUrl;
        addRole("ROLE_STUDENT"); // Ensure student role is added
    }

    public String getGithubId() {
        return githubId;
    }

    public void setGithubId(String githubId) {
        this.githubId = githubId;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
} 