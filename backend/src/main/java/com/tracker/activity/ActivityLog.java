package com.tracker.activity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "activity_log")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID boardId;
    private UUID cardId;
    private UUID actorUserId;
    private String action;
    private String description;
    private Instant createdAt;
}