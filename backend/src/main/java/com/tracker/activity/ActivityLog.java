package com.tracker.activity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name="activity_log")
public class ActivityLog {

    @Id
    @GeneratedValue
    private UUID id;
}

