package com.tracker.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity
public class RefreshToken {

    @Id
    @GeneratedValue
    private UUID id;
}

