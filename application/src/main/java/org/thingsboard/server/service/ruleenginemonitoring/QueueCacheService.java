// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ruleenginemonitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.QueueShortInfo;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleEngineMonitoringDao;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@TbRuleEngineComponent
@RequiredArgsConstructor
public class QueueCacheService {
    private final RuleEngineMonitoringDao dao;
    private final TbTenantProfileCache tenantProfileCache;
    // Only reloads and invalidations take the lock; lookups (one per exported span) read the snapshot lock-free.
    private final Lock queueUpdateLock = new ReentrantLock();
    // Loaded on first use and kept until a queue changes: every rule engine node gets each queue update and delete
    // notification, which invalidates it.
    private volatile QueuesSnapshot snapshot;

    public UUID getQueueId(UUID tenantId, String queueName) {
        if (tenantId == null || queueName == null) {
            return null;
        }
        UUID queueTenantId = QueueOwners.resolveQueueTenantId(tenantProfileCache, tenantId);
        return getSnapshot().queueIds().get(new QueueNameKey(queueTenantId, queueName));
    }

    public Collection<QueueShortInfo> getAllQueues() {
        return getSnapshot().queues();
    }

    // Called by the rule engine consumer service on every queue create/update/delete notification.
    public void invalidateQueues() {
        queueUpdateLock.lock();
        try {
            snapshot = null;
        } finally {
            queueUpdateLock.unlock();
        }
    }

    private QueuesSnapshot getSnapshot() {
        QueuesSnapshot current = snapshot;
        if (current != null) {
            return current;
        }
        queueUpdateLock.lock();
        try {
            current = snapshot;
            if (current == null) {
                current = QueuesSnapshot.of(dao.findAllQueuesShortInfo());
                snapshot = current;
                log.debug("Reloaded queues {}", current.queues());
            }
            return current;
        } finally {
            queueUpdateLock.unlock();
        }
    }

    private record QueueNameKey(UUID tenantId, String name) {
    }

    private record QueuesSnapshot(List<QueueShortInfo> queues, Map<QueueNameKey, UUID> queueIds) {

        static QueuesSnapshot of(Collection<QueueShortInfo> loaded) {
            Map<QueueNameKey, UUID> queueIds = new HashMap<>();
            for (QueueShortInfo queue : loaded) {
                // first one wins, as the former linear scan did
                queueIds.putIfAbsent(new QueueNameKey(queue.tenantId(), queue.name()), queue.id());
            }
            return new QueuesSnapshot(List.copyOf(loaded), Map.copyOf(queueIds));
        }
    }
}
