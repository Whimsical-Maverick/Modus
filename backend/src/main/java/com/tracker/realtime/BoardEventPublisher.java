package com.tracker.realtime;

import com.tracker.realtime.events.CardMovedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class BoardEventPublisher
{
    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCardMoved(CardMovedEvent event)
    {
        messagingTemplate.convertAndSend("/topic/board/"+event.boardId(),event);
    }
}

