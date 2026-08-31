package com.tracker.workspace;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity
public class Workspace {

    @Id
    @GeneratedValue
    private UUID id;
}

