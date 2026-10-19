package com.tracker.realtime;

import com.tracker.realtime.events.CardMovedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class BoardEventPublisherTests {

    @Test
    void broadcastsOnlyAfterSuccessfulCommit() {
        try (var context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            SimpMessagingTemplate template = context.getBean(SimpMessagingTemplate.class);
            CardMovedEvent event = event();

            beginTransactionSynchronization();
            try {
                context.publishEvent(event);
                verifyNoInteractions(template);
                complete(TransactionSynchronization.STATUS_COMMITTED);
                verify(template).convertAndSend(
                        "/topic/board/" + event.boardId(), event);
            } finally {
                clearTransactionSynchronization();
            }
        }
    }

    @Test
    void doesNotBroadcastAfterRollback() {
        try (var context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            SimpMessagingTemplate template = context.getBean(SimpMessagingTemplate.class);

            beginTransactionSynchronization();
            try {
                context.publishEvent(event());
                complete(TransactionSynchronization.STATUS_ROLLED_BACK);
                verifyNoInteractions(template);
            } finally {
                clearTransactionSynchronization();
            }
        }
    }

    private static void beginTransactionSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }

    private static void complete(int status) {
        for (TransactionSynchronization synchronization
                : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCompletion(status);
        }
    }

    private static void clearTransactionSynchronization() {
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    private static CardMovedEvent event() {
        return new CardMovedEvent(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), 4, UUID.randomUUID(), "Bhavya",
                "Fix login", "To Do", "In Progress");
    }

    @Configuration
    @EnableTransactionManagement
    static class TestConfig {
        @Bean
        SimpMessagingTemplate messagingTemplate() {
            return mock(SimpMessagingTemplate.class);
        }

        @Bean
        BoardEventPublisher boardEventPublisher(SimpMessagingTemplate template) {
            return new BoardEventPublisher(template);
        }
    }
}
