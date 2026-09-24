package com.tracker.card;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name="card")
public class Card {

    @Id
    @GeneratedValue
    private UUID id;
}

