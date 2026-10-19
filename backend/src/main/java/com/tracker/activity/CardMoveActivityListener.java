package com.tracker.activity;

import com.tracker.realtime.events.CardMovedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CardMoveActivityListener {

    private final ActivityLogService activityLogService;

    @Async("activityExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCardMoved(CardMovedEvent event) {
        activityLogService.recordMove(event);
    }
}