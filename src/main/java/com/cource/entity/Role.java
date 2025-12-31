package com.cource.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_code", nullable = false, unique = true)
    private String roleCode; // "ADMIN", "STUDENT"

    @Column(name = "role_name", nullable = false)
    private String roleName; // "Administrator", "Student"

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;


}