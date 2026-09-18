package com.cozy.common.tx;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AfterCommitTest {

    @AfterEach
    void cleanup() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void runsImmediatelyWhenNoTransactionIsActive() {
        AtomicBoolean ran = new AtomicBoolean(false);

        AfterCommit.run(() -> ran.set(true));

        assertTrue(ran.get());
    }

    @Test
    void defersUntilTheTransactionCommits() {
        AtomicBoolean ran = new AtomicBoolean(false);
        TransactionSynchronizationManager.initSynchronization();

        AfterCommit.run(() -> ran.set(true));

        assertFalse(ran.get(), "事务提交前不应执行");

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

        assertTrue(ran.get(), "事务提交后应执行");
    }

    @Test
    void neverRunsWhenTheTransactionRollsBack() {
        AtomicBoolean ran = new AtomicBoolean(false);
        TransactionSynchronizationManager.initSynchronization();

        AfterCommit.run(() -> ran.set(true));
        // 回滚：不会触发 afterCommit
        TransactionSynchronizationManager.clearSynchronization();

        assertFalse(ran.get());
    }
}
