// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.queue;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.gen.transport.TransportProtos.SystemUpdateMsg;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The one dispatch for {@link SystemUpdateMsg}, shared by the core and rule engine consumers so the two cannot
 * come to disagree about what a type means.
 * <p>
 * Every branch hands its work to this bean's own executor and acks immediately. The caller is the consumer
 * loop thread, and the pack it belongs to is bounded by a two-second processing timeout, while reconvergence
 * makes a blocking call to the licence server with 20s connect and 20s read timeouts. That is also why the
 * work does not go on the consumers' shared {@code mgmtExecutor}, which their own queue management needs. The
 * ack says the message was accepted; reconvergence is idempotent and its own failure is recovered by the
 * five-minute tick, so there is nothing a redelivery would add.
 */
@Component
@Slf4j
@RequiredArgsConstructor
class SystemUpdateMsgHandler {

    private final SubscriptionService subscriptionService;

    private ExecutorService reconvergeExecutor;

    @PostConstruct
    public void init() {
        reconvergeExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("license-reconverge"));
    }

    @PreDestroy
    public void stop() {
        if (reconvergeExecutor != null) {
            reconvergeExecutor.shutdownNow();
        }
    }

    public void handle(SystemUpdateMsg msg, TbCallback callback) {
        switch (msg.getType()) {
            case LICENSE -> reconvergeExecutor.execute(this::reconverge);
            // Covers both UNRECOGNIZED (a type a newer node knows and this build does not) and an unset field.
            default -> log.debug("Ignoring a system update of unknown type {}", msg.getTypeValue());
        }
        callback.onSuccess();
    }

    private void reconverge() {
        try {
            subscriptionService.reconcileLicenseState();
        } catch (Exception e) {
            log.warn("Failed to reconverge on the cluster's license state. The scheduled reconciliation will " +
                    "retry.", e);
        }
    }
}
