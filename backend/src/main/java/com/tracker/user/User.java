package com.tracker.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.util.UUID;
import java.time.Instant;

@Entity
@Getter @Setter
@Table(name="users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable=false)
    private String username;

    @Column(nullable=false)
    private String passwordHash;

    @Column(nullable=false,unique=true)
    private String email;

    @CreationTimestamp
    private Instant createdAt;

}
