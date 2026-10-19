package com.tracker.activity;

import com.tracker.realtime.events.CardMovedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;
import java.util.concurrent.Executor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class CardMoveActivityListenerTests {

    @Test
    void recordsOnlyAfterCommit() {
        try (var context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            ActivityLogService service = AopTestUtils.getUltimateTargetObject(
                    context.getBean(ActivityLogService.class));
            CardMovedEvent event = event();

            beginTransactionSynchronization();
            try {
                context.publishEvent(event);
                verifyNoInteractions(service);
                complete(TransactionSynchronization.STATUS_COMMITTED);
                verify(service).recordMove(event);
            } finally {
                clearTransactionSynchronization();
            }
        }
    }

    @Test
    void doesNotRecordRolledBackMove() {
        try (var context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            ActivityLogService service = AopTestUtils.getUltimateTargetObject(
                    context.getBean(ActivityLogService.class));

            beginTransactionSynchronization();
            try {
                context.publishEvent(event());
                complete(TransactionSynchronization.STATUS_ROLLED_BACK);
                verifyNoInteractions(service);
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
    @EnableAsync
    static class TestConfig {
        @Bean
        PlatformTransactionManager transactionManager() {
            PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
            when(manager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
            return manager;
        }

        @Bean("activityExecutor")
        Executor activityExecutor() {
            // Deterministic execution in this test; production uses a thread pool.
            return new SyncTaskExecutor();
        }

        @Bean
        ActivityLogService activityLogService() {
            return mock(ActivityLogService.class);
        }

        @Bean
        CardMoveActivityListener cardMoveActivityListener(ActivityLogService service) {
            return new CardMoveActivityListener(service);
        }
    }
}
