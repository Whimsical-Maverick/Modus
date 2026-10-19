package com.tracker.card;

import com.tracker.board.BoardList;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "card")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "list_id", nullable = false)
    private BoardList list;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Version
    @Column(name = "version",nullable=false)
    private long version;
}