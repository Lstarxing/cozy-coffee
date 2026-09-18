package com.cozy.common.tx;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 把副作用推迟到「当前事务提交之后」执行。
 *
 * <p>为什么需要：{@code CompletableFuture.runAsync(...)} 会立即在线程池上启动，若它调用远程服务
 * 或写库，而外层事务随后回滚，副作用已经生效且无法撤销（典型：注册事务回滚但会员/优惠券已发出）。
 *
 * <p>无活跃事务时立即执行，便于纯单测与非事务调用点复用。
 *
 * <p>注意：只解决「先于提交执行」，不解决「提交后进程崩溃导致副作用丢失」——需要持久化 Outbox 才能兜住。
 */
public final class AfterCommit {

    private AfterCommit() {
    }

    public static void run(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
