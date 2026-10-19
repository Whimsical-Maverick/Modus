package com.tracker.card;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, UUID>
{
    List<Card> findByList_Board_Id(UUID boardId);
}

