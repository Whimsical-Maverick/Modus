package com.tracker.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Getter @Setter
@Table(name="User")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable=false)
    private String username;

    @Column(nullable=false)
    private String passwordHash;

    @Column(nullable=false,unique=true)
    private String email;

    @CreationTimestamp
    private Instant createdAt;

}
