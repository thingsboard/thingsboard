/**
 * Copyright © 2016-2018 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.queue;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.transport.quota.tenant.TenantQuotaService;
import org.thingsboard.server.dao.queue.MsgQueue;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class DefaultMsgQueueService implements MsgQueueService {

    @Value("${actors.rule.queue.max_size}")
    private long queueMaxSize;

    @Value("${actors.rule.queue.cleanup_period}")
    private long queueCleanUpPeriod;

    @Autowired
    private MsgQueue msgQueue;

    @Autowired
    private TenantQuotaService quotaService;

    private ScheduledExecutorService cleanupExecutor;

    private Map<TenantId, AtomicLong> pendingCountPerTenant = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (queueCleanUpPeriod > 0) {
            cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
            cleanupExecutor.scheduleAtFixedRate(() -> cleanup(),
                    queueCleanUpPeriod, queueCleanUpPeriod, TimeUnit.SECONDS);
        }
    }

    @PreDestroy
    public void stop() {
        if (cleanupExecutor != null) {
            cleanupExecutor.shutdownNow();
        }
    }

    @Override
    public ListenableFuture<Void> put(TenantId tenantId, TbMsg msg, UUID nodeId, long clusterPartition) {
        if(quotaService.isQuotaExceeded(tenantId.getId().toString())) {
            log.warn("Tenant TbMsg Quota exceeded for [{}:{}] . Reject", tenantId.getId());
            return Futures.immediateFailedFuture(new RuntimeException("Tenant TbMsg Quota exceeded"));
        }

        AtomicLong pendingMsgCount = pendingCountPerTenant.computeIfAbsent(tenantId, key -> new AtomicLong());
        if (pendingMsgCount.incrementAndGet() < queueMaxSize) {
            return msgQueue.put(tenantId, msg, nodeId, clusterPartition);
        } else {
            pendingMsgCount.decrementAndGet();
            return Futures.immediateFailedFuture(new RuntimeException("Message queue is full!"));
        }
    }

    @Override
    public ListenableFuture<Void> ack(TenantId tenantId, TbMsg msg, UUID nodeId, long clusterPartition) {
        ListenableFuture<Void> result = msgQueue.ack(tenantId, msg, nodeId, clusterPartition);
        AtomicLong pendingMsgCount = pendingCountPerTenant.computeIfAbsent(tenantId, key -> new AtomicLong());
        pendingMsgCount.decrementAndGet();
        return result;
    }

    @Override
    public Iterable<TbMsg> findUnprocessed(TenantId tenantId, UUID nodeId, long clusterPartition) {
        return msgQueue.findUnprocessed(tenantId, nodeId, clusterPartition);
    }

    private void cleanup() {
        pendingCountPerTenant.forEach((tenantId, pendingMsgCount) -> {
            pendingMsgCount.set(0);
            msgQueue.cleanUp(tenantId);
        });
    }

}
