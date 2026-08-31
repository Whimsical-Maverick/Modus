package com.tracker.board;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity
public class Board {

    @Id
    @GeneratedValue
    private UUID id;
}

