package com.tracker.activity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity
public class ActivityLog {

    @Id
    @GeneratedValue
    private UUID id;
}

