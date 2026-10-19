package com.tracker.activity;

import com.tracker.realtime.events.CardMovedEvent;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ActivityLogService
{
    private final ActivityLogRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordMove(CardMovedEvent event) {
        ActivityLog log = new ActivityLog();
        log.setBoardId(event.boardId());
        log.setCardId(event.cardId());
        log.setActorUserId(event.actorId());
        log.setAction("CARD_MOVED");
        log.setDescription(
                event.actorName() + " moved " + event.cardTitle()
                        + " from " + event.oldListName()
                        + " to " + event.newListName()
        );
        log.setCreatedAt(Instant.now());

        repository.save(log);
    }
}

