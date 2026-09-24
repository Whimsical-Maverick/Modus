package com.tracker.board;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name="board")
public class Board {

    @Id
    @GeneratedValue
    private UUID id;
}

