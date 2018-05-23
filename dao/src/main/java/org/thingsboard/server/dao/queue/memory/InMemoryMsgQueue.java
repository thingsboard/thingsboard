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
package org.thingsboard.server.dao.queue.memory;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.queue.MsgQueue;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

/**
 * Created by ashvayka on 27.04.18.
 */
@Component
@ConditionalOnProperty(prefix = "actors.rule.queue", value = "type", havingValue = "memory", matchIfMissing = true)
@Slf4j
public class InMemoryMsgQueue implements MsgQueue {

    private ListeningExecutorService queueExecutor;
    private Map<TenantId, Map<InMemoryMsgKey, Map<UUID, TbMsg>>> data = new HashMap<>();

    @PostConstruct
    public void init() {
        // Should be always single threaded due to absence of locks.
        queueExecutor = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor());
    }

    @PreDestroy
    public void stop() {
        if (queueExecutor != null) {
            queueExecutor.shutdownNow();
        }
    }

    @Override
    public ListenableFuture<Void> put(TenantId tenantId, TbMsg msg, UUID nodeId, long clusterPartition) {
        return queueExecutor.submit(() -> {
            data.computeIfAbsent(tenantId, key -> new HashMap<>()).
                    computeIfAbsent(new InMemoryMsgKey(nodeId, clusterPartition), key -> new HashMap<>()).put(msg.getId(), msg);
            return null;
        });
    }

    @Override
    public ListenableFuture<Void> ack(TenantId tenantId, TbMsg msg, UUID nodeId, long clusterPartition) {
        return queueExecutor.submit(() -> {
            Map<InMemoryMsgKey, Map<UUID, TbMsg>> tenantMap = data.get(tenantId);
            if (tenantMap != null) {
                InMemoryMsgKey key = new InMemoryMsgKey(nodeId, clusterPartition);
                Map<UUID, TbMsg> map = tenantMap.get(key);
                if (map != null) {
                    map.remove(msg.getId());
                    if (map.isEmpty()) {
                        tenantMap.remove(key);
                    }
                }
                if (tenantMap.isEmpty()) {
                    data.remove(tenantId);
                }
            }
            return null;
        });
    }

    @Override
    public Iterable<TbMsg> findUnprocessed(TenantId tenantId, UUID nodeId, long clusterPartition) {
        ListenableFuture<List<TbMsg>> list = queueExecutor.submit(() -> {
            Map<InMemoryMsgKey, Map<UUID, TbMsg>> tenantMap = data.get(tenantId);
            if (tenantMap != null) {
                InMemoryMsgKey key = new InMemoryMsgKey(nodeId, clusterPartition);
                Map<UUID, TbMsg> map = tenantMap.get(key);
                if (map != null) {
                    return new ArrayList<>(map.values());
                } else {
                    return Collections.emptyList();
                }
            } else {
                return Collections.emptyList();
            }
        });
        try {
            return list.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ListenableFuture<Void> cleanUp(TenantId tenantId) {
        return queueExecutor.submit(() -> {
            data.remove(tenantId);
            return null;
        });
    }
}
