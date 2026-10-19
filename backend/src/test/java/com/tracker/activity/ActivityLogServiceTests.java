package com.tracker.activity;

import com.tracker.realtime.events.CardMovedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTests {
    @Mock ActivityLogRepository repository;
    @InjectMocks ActivityLogService service;

    @Test
    void recordsMoveWithActorAndBoard() {
        CardMovedEvent event = new CardMovedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), 4, UUID.randomUUID(), "Bhavya",
                "Fix login", "To Do", "In Progress");

        service.recordMove(event);

        ArgumentCaptor<ActivityLog> saved = ArgumentCaptor.forClass(ActivityLog.class);
        verify(repository).save(saved.capture());
        assertEquals(event.boardId(), saved.getValue().getBoardId());
        assertEquals(event.cardId(), saved.getValue().getCardId());
        assertEquals(event.actorId(), saved.getValue().getActorUserId());
        assertEquals("CARD_MOVED", saved.getValue().getAction());
        assertEquals("Bhavya moved Fix login from To Do to In Progress",
                saved.getValue().getDescription());
        assertNotNull(saved.getValue().getCreatedAt());
    }
}
