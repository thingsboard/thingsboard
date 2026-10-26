// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ruleenginemonitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

import static java.util.concurrent.TimeUnit.MINUTES;

@Slf4j
@Service
@TbRuleEngineComponent
public class QueueCacheService {
    private final RuleEngineMonitoringDao dao;
    private final TbTenantProfileCache tenantProfileCache;
    private final long queueCacheTtlMs;
    // Only reloads and invalidations take the lock; lookups (one per exported span) read the snapshot lock-free.
    private final Lock queueUpdateLock = new ReentrantLock();
    private volatile QueuesSnapshot snapshot;

    public QueueCacheService(RuleEngineMonitoringDao dao,
                             TbTenantProfileCache tenantProfileCache,
                             @Value("${queue-cache.ttl-min:30}") int queueCacheTtlMin) {
        this.dao = dao;
        this.tenantProfileCache = tenantProfileCache;
        this.queueCacheTtlMs = MINUTES.toMillis(queueCacheTtlMin);
    }

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
        if (current != null && !isExpired(current)) {
            return current;
        }
        queueUpdateLock.lock();
        try {
            current = snapshot;
            if (current == null || isExpired(current)) {
                current = QueuesSnapshot.of(dao.findAllQueuesShortInfo(), System.currentTimeMillis());
                snapshot = current;
                log.debug("Reloaded queues {}", current.queues());
            }
            return current;
        } finally {
            queueUpdateLock.unlock();
        }
    }

    private boolean isExpired(QueuesSnapshot snapshot) {
        return System.currentTimeMillis() - snapshot.loadedAtMs() > queueCacheTtlMs;
    }

    private record QueueNameKey(UUID tenantId, String name) {
    }

    private record QueuesSnapshot(List<QueueShortInfo> queues, Map<QueueNameKey, UUID> queueIds, long loadedAtMs) {

        static QueuesSnapshot of(Collection<QueueShortInfo> loaded, long loadedAtMs) {
            Map<QueueNameKey, UUID> queueIds = new HashMap<>();
            for (QueueShortInfo queue : loaded) {
                // first one wins, as the former linear scan did
                queueIds.putIfAbsent(new QueueNameKey(queue.tenantId(), queue.name()), queue.id());
            }
            return new QueuesSnapshot(List.copyOf(loaded), Map.copyOf(queueIds), loadedAtMs);
        }
    }
}
