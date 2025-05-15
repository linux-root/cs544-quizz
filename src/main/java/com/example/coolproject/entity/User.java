package com.example.coolproject.entity;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

@Entity
@Table(name = "users") // "user" is often a reserved keyword in SQL
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String name;

    // Storing roles as a comma-separated string
    private String roles; // e.g., "ROLE_STUDENT,ROLE_PROFESSOR"

    public User() {
    }

    public User(String email, String name, Set<String> roles) {
        this.email = email;
        this.name = name;
        setRoles(roles);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<String> getRoles() {
        if (this.roles == null || this.roles.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(Arrays.asList(this.roles.split(",")));
    }

    public void setRoles(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            this.roles = null;
        } else {
            this.roles = String.join(",", roles);
        }
    }

    public void addRole(String role) {
        Set<String> currentRoles = getRoles();
        currentRoles.add(role);
        setRoles(currentRoles);
    }
} 