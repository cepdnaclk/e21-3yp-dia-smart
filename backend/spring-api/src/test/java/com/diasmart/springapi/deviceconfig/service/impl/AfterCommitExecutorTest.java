package com.diasmart.springapi.deviceconfig.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AfterCommitExecutorTest {

    private final AfterCommitExecutor executor = new AfterCommitExecutor();

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void runAfterCommitShouldRunOnlyAfterCommit() {
        AtomicBoolean ran = new AtomicBoolean(false);
        TransactionSynchronizationManager.initSynchronization();

        executor.runAfterCommit(() -> ran.set(true));

        assertFalse(ran.get());
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        assertTrue(ran.get());
    }

    @Test
    void runAfterCommitShouldNotRunOnRollback() {
        AtomicBoolean ran = new AtomicBoolean(false);
        TransactionSynchronizationManager.initSynchronization();

        executor.runAfterCommit(() -> ran.set(true));

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        assertFalse(ran.get());
    }
}
