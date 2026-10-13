package com.tracker.board;

import com.tracker.workspace.Workspace;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name="board")
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY,optional=false)
    @JoinColumn(name = "workspace_id",nullable=false)
    private Workspace workspace;

    @Column(name = "name",nullable=false,length=120)
    private String name;

}

