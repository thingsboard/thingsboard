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
package org.thingsboard.server.service.transaction;

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.RuleChainTransactionService;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.gen.cluster.ClusterAPIProtos;
import org.thingsboard.server.service.cluster.routing.ClusterRoutingService;
import org.thingsboard.server.service.cluster.rpc.ClusterRpcService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

@Service
@Slf4j
public class BaseRuleChainTransactionService implements RuleChainTransactionService {

    @Autowired
    private ClusterRoutingService routingService;

    @Autowired
    private ClusterRpcService clusterRpcService;

    @Value("${actors.rule.transaction.queue_size}")
    private int finalQueueSize;
    @Value("${actors.rule.transaction.duration}")
    private long duration;

    private final Lock transactionLock = new ReentrantLock();
    private final ConcurrentMap<EntityId, BlockingQueue<TbTransactionTask>> transactionMap = new ConcurrentHashMap<>();
    private final Queue<TbTransactionTask> timeoutQueue = new ConcurrentLinkedQueue<>();

    private ExecutorService timeoutExecutor;
    private ExecutorService executor;

    @PostConstruct
    public void init() {
        timeoutExecutor = Executors.newSingleThreadExecutor();
        executor = Executors.newSingleThreadExecutor();
        executeOnTimeout();
    }

    @PreDestroy
    public void destroy() {
        if (timeoutExecutor != null) {
            timeoutExecutor.shutdownNow();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    public void beginTransaction(TbContext ctx, TbMsg msg, Consumer<TbMsg> onStart, Consumer<TbMsg> onEnd, Consumer<Throwable> onFailure) {
        transactionLock.lock();
        try {
            BlockingQueue<TbTransactionTask> queue = transactionMap.computeIfAbsent(msg.getTransactionData().getOriginatorId(), id ->
                    new LinkedBlockingQueue<>(finalQueueSize));

            TbTransactionTask task = new TbTransactionTask(msg, onStart, onEnd, onFailure);
            int queueSize = queue.size();
            if (queueSize >= finalQueueSize) {
                task.getOnFailure().accept(new RuntimeException("Queue has no space!"));
            } else {
                addMsgToQueues(queue, task);
                if (queueSize == 0) {
                    startTransactionTask(task);
                } else {
                    log.trace("Msg [{}] [{}] is waiting to start transaction!", msg.getId(), msg.getType());
                }
            }
        } finally {
            transactionLock.unlock();
        }
    }

    private void addMsgToQueues(BlockingQueue<TbTransactionTask> queue, TbTransactionTask task) {
        queue.offer(task);
        timeoutQueue.offer(task);
        log.trace("Added msg to queue, size: [{}]", queue.size());
    }

    @Override
    public boolean endTransaction(TbContext ctx, TbMsg msg, Consumer<Throwable> onFailure) {
        BlockingQueue<TbTransactionTask> queue = transactionMap.get(msg.getTransactionData().getOriginatorId());

        TbTransactionTask currentTask = queue.peek();
        if (currentTask != null) {
            if (currentTask.getMsg().getTransactionData().getTransactionId().equals(msg.getTransactionData().getTransactionId())) {
                currentTask.setIsCompleted(true);
                queue.remove();
                log.trace("Removed msg from queue, size [{}]", queue.size());
                currentTask.getOnEnd().accept(currentTask.getMsg());

                TbTransactionTask nextTask = queue.peek();
                if (nextTask != null) {
                    startTransactionTask(nextTask);
                }
            } else {
                log.trace("Task has expired!");
                onFailure.accept(new RuntimeException("Task has expired!"));
                return true;
            }
        } else {
            log.trace("Queue is empty, previous task has expired!");
            onFailure.accept(new RuntimeException("Queue is empty, previous task has expired!"));
            return true;
        }
        return false;
    }

    private void executeOnTimeout() {
        timeoutExecutor.submit(() -> {
            while (true) {
                TbTransactionTask task = timeoutQueue.peek();
                if (task != null) {
                    if (task.getIsCompleted()) {
                        timeoutQueue.poll();
                    } else {
                        if (System.currentTimeMillis() > task.getExpirationTime()) {
                            log.trace("Task has expired! Deleting it...[{}] [{}]", task.getMsg().getId(), task.getMsg().getType());
                            timeoutQueue.poll();
                            task.getOnFailure().accept(new RuntimeException("Task has expired!"));

                            BlockingQueue<TbTransactionTask> queue = transactionMap.get(task.getMsg().getTransactionData().getOriginatorId());
                            queue.poll();

                            TbTransactionTask nextTask = queue.peek();
                            if (nextTask != null) {
                                startTransactionTask(nextTask);
                            }
                        } else {
                            try {
                                log.trace("Task has not expired! Continue executing...[{}] [{}]", task.getMsg().getId(), task.getMsg().getType());
                                TimeUnit.MILLISECONDS.sleep(duration);
                            } catch (InterruptedException e) {
                                throw new IllegalStateException("Thread interrupted", e);
                            }
                        }
                    }
                } else {
                    try {
                        log.trace("Queue is empty, waiting for tasks!");
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException e) {
                        throw new IllegalStateException("Thread interrupted", e);
                    }
                }
            }
        });
    }

    private void startTransactionTask(TbTransactionTask task) {
        task.setIsCompleted(false);
        task.setExpirationTime(System.currentTimeMillis() + duration);
        task.getOnStart().accept(task.getMsg());
    }

    @Override
    public void onRemoteTransactionMsg(ServerAddress serverAddress, byte[] data) {
        ClusterAPIProtos.TransactionServiceMsgProto proto;
        try {
            proto = ClusterAPIProtos.TransactionServiceMsgProto.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        TenantId tenantId = new TenantId(new UUID(proto.getTenantIdMSB(), proto.getTenantIdLSB()));

        String entityTypeStr = proto.getEntityType();
        EntityId entityId;
        if (entityTypeStr.equals(EntityType.ASSET.name())) {
            entityId = new AssetId(new UUID(proto.getOriginatorIdMSB(), proto.getOriginatorIdLSB()));
        } else {
            entityId = new DeviceId(new UUID(proto.getOriginatorIdMSB(), proto.getOriginatorIdLSB()));
        }
        onTransactionEnd(tenantId, entityId);
    }

    @Override
    public void onTransactionEnd(TenantId tenantId, EntityId entityId) {
        executor.submit(() -> onTransactionEndSync(tenantId, entityId));
    }

    private void onTransactionEndSync(TenantId tenantId, EntityId entityId) {
        Optional<ServerAddress> address = routingService.resolveById(entityId);
        address.ifPresent(serverAddress -> sendTransactionEvent(tenantId, entityId, serverAddress));
    }

    private void sendTransactionEvent(TenantId tenantId, EntityId entityId, ServerAddress address) {
        log.trace("[{}][{}] Originator is monitored on other server: {}", tenantId, entityId, address);
        ClusterAPIProtos.TransactionServiceMsgProto.Builder builder = ClusterAPIProtos.TransactionServiceMsgProto.newBuilder();
        builder.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        builder.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());
        builder.setEntityType(entityId.getEntityType().name());
        builder.setOriginatorIdMSB(entityId.getId().getMostSignificantBits());
        builder.setOriginatorIdLSB(entityId.getId().getLeastSignificantBits());
        clusterRpcService.tell(address, ClusterAPIProtos.MessageType.CLUSTER_TRANSACTION_SERVICE_MESSAGE, builder.build().toByteArray());
    }
}
