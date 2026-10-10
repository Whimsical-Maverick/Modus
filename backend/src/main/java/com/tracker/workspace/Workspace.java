package com.tracker.workspace;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name="workspace")
public class Workspace {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "name", nullable = false, length = 120)
    private String name;
}

