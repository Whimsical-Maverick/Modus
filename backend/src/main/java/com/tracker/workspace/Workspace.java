package com.tracker.workspace;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name="workspace")
public class Workspace {

    @Id
    @GeneratedValue
    private UUID id;
}

