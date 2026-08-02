package com.diasmart.springapi.deviceconfig.service.impl;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class AfterCommitExecutor {

    public void runAfterCommit(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                task.run();
            }
        });
    }
}
