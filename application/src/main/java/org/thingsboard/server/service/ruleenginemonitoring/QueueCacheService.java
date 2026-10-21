/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.service.ruleenginemonitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.QueueShortInfo;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleEngineMonitoringDao;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;

import java.util.Collection;
import java.util.List;
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
    private final Lock queueUpdateLock = new ReentrantLock();
    private List<QueueShortInfo> queues = List.of();
    private long queuesLoadedAtMs;

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
        Collection<QueueShortInfo> allQueues = getAllQueues();
        TenantProfile profile = tenantProfileCache.get(TenantId.fromUUID(tenantId));
        boolean usesSystemQueues = profile != null && !profile.isIsolatedTbRuleEngine();
        UUID lookupTenantId = usesSystemQueues ? TenantId.SYS_TENANT_ID.getId() : tenantId;
        return allQueues.stream()
                .filter(q -> lookupTenantId.equals(q.tenantId()) && queueName.equals(q.name()))
                .map(QueueShortInfo::id)
                .findFirst()
                .orElse(null);
    }

    public Collection<QueueShortInfo> getAllQueues() {
        queueUpdateLock.lock();
        try {
            if (queues.isEmpty() || queuesExpired()) {
                queues = List.copyOf(dao.findAllQueuesShortInfo());
                queuesLoadedAtMs = System.currentTimeMillis();
                log.debug("Reloaded queues {}", queues);
            }
            return queues;
        } finally {
            queueUpdateLock.unlock();
        }
    }

    private boolean queuesExpired() {
        return System.currentTimeMillis() - queuesLoadedAtMs > queueCacheTtlMs;
    }

    // Called by the rule engine consumer service on every queue create/update/delete notification.
    public void invalidateQueues() {
        queueUpdateLock.lock();
        try {
            queues = List.of();
            queuesLoadedAtMs = 0;
        } finally {
            queueUpdateLock.unlock();
        }
    }
}
