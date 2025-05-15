package com.example.coolproject.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Set;

@Entity
@Table(name = "professors")
public class Professor extends User {

    public Professor() {
        super();
    }

    public Professor(String email, String name, Set<String> roles) {
        super(email, name, roles);
        addRole("ROLE_PROFESSOR"); // Ensure professor role is added
    }

    // Add professor-specific fields here if needed in the future
} 