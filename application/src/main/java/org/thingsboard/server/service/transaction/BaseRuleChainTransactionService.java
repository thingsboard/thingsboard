/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.gen.cluster.ClusterAPIProtos;
import org.thingsboard.server.service.cluster.routing.ClusterRoutingService;
import org.thingsboard.server.service.cluster.rpc.ClusterRpcService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
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

    @Autowired
    private DbCallbackExecutorService callbackExecutor;

    @Value("${actors.rule.transaction.queue_size}")
    private int finalQueueSize;
    @Value("${actors.rule.transaction.duration}")
    private long duration;

    private final Lock transactionLock = new ReentrantLock();
    private final ConcurrentMap<EntityId, BlockingQueue<TbTransactionTask>> transactionMap = new ConcurrentHashMap<>();
    private final Queue<TbTransactionTask> timeoutQueue = new ConcurrentLinkedQueue<>();

    private ExecutorService timeoutExecutor;

    @PostConstruct
    public void init() {
        timeoutExecutor = Executors.newSingleThreadExecutor();
        executeOnTimeout();
    }

    @PreDestroy
    public void destroy() {
        if (timeoutExecutor != null) {
            timeoutExecutor.shutdownNow();
        }
    }

    @Override
    public void beginTransaction(TbMsg msg, Consumer<TbMsg> onStart, Consumer<TbMsg> onEnd, Consumer<Throwable> onFailure) {
        transactionLock.lock();
        try {
            BlockingQueue<TbTransactionTask> queue = transactionMap.computeIfAbsent(msg.getTransactionData().getOriginatorId(), id ->
                    new LinkedBlockingQueue<>(finalQueueSize));

            TbTransactionTask transactionTask = new TbTransactionTask(msg, onStart, onEnd, onFailure, System.currentTimeMillis() + duration);
            int queueSize = queue.size();
            if (queueSize >= finalQueueSize) {
                executeOnFailure(transactionTask.getOnFailure(), "Queue has no space!");
            } else {
                addMsgToQueues(queue, transactionTask);
                if (queueSize == 0) {
                    executeOnSuccess(transactionTask.getOnStart(), transactionTask.getMsg());
                } else {
                    log.trace("Msg [{}][{}] is waiting to start transaction!", msg.getId(), msg.getType());
                }
            }
        } finally {
            transactionLock.unlock();
        }
    }

    @Override
    public void endTransaction(TbMsg msg, Consumer<TbMsg> onSuccess, Consumer<Throwable> onFailure) {
        EntityId originatorId = msg.getTransactionData().getOriginatorId();
        UUID transactionId = msg.getTransactionData().getTransactionId();

        Optional<ServerAddress> address = routingService.resolveById(originatorId);
        if (address.isPresent()) {
            sendTransactionEventToRemoteServer(originatorId, transactionId, address.get());
            executeOnSuccess(onSuccess, msg);
        } else {
            endLocalTransaction(transactionId, originatorId, onSuccess, onFailure);
        }
    }

    @Override
    public void onRemoteTransactionMsg(ServerAddress serverAddress, byte[] data) {
        ClusterAPIProtos.TransactionEndServiceMsgProto proto;
        try {
            proto = ClusterAPIProtos.TransactionEndServiceMsgProto.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        EntityId originatorId = EntityIdFactory.getByTypeAndUuid(proto.getEntityType(), new UUID(proto.getOriginatorIdMSB(), proto.getOriginatorIdLSB()));
        UUID transactionId = new UUID(proto.getTransactionIdMSB(), proto.getTransactionIdLSB());
        endLocalTransaction(transactionId, originatorId, msg -> {
        }, error -> {
        });
    }

    private void addMsgToQueues(BlockingQueue<TbTransactionTask> queue, TbTransactionTask transactionTask) {
        queue.offer(transactionTask);
        timeoutQueue.offer(transactionTask);
        log.trace("Added msg to queue, size: [{}]", queue.size());
    }

    private void endLocalTransaction(UUID transactionId, EntityId originatorId, Consumer<TbMsg> onSuccess, Consumer<Throwable> onFailure) {
        transactionLock.lock();
        try {
            BlockingQueue<TbTransactionTask> queue = transactionMap.computeIfAbsent(originatorId, id ->
                    new LinkedBlockingQueue<>(finalQueueSize));

            TbTransactionTask currentTransactionTask = queue.peek();
            if (currentTransactionTask != null) {
                if (currentTransactionTask.getMsg().getTransactionData().getTransactionId().equals(transactionId)) {
                    currentTransactionTask.setCompleted(true);
                    queue.poll();
                    log.trace("Removed msg from queue, size [{}]", queue.size());

                    executeOnSuccess(currentTransactionTask.getOnEnd(), currentTransactionTask.getMsg());
                    executeOnSuccess(onSuccess, currentTransactionTask.getMsg());

                    TbTransactionTask nextTransactionTask = queue.peek();
                    if (nextTransactionTask != null) {
                        executeOnSuccess(nextTransactionTask.getOnStart(), nextTransactionTask.getMsg());
                    }
                } else {
                    log.trace("Task has expired!");
                    executeOnFailure(onFailure, "Task has expired!");
                }
            } else {
                log.trace("Queue is empty, previous task has expired!");
                executeOnFailure(onFailure, "Queue is empty, previous task has expired!");
            }
        } finally {
            transactionLock.unlock();
        }
    }

    private void executeOnTimeout() {
        timeoutExecutor.submit(() -> {
            while (true) {
                TbTransactionTask transactionTask = timeoutQueue.peek();
                if (transactionTask != null) {
                    long sleepDuration = 0L;
                    transactionLock.lock();
                    try {
                        if (transactionTask.isCompleted()) {
                            timeoutQueue.poll();
                        } else {
                            long expIn = transactionTask.getExpirationTime() - System.currentTimeMillis();
                            if (expIn < 0) {
                                log.trace("Task has expired! Deleting it...[{}][{}]", transactionTask.getMsg().getId(), transactionTask.getMsg().getType());
                                timeoutQueue.poll();
                                executeOnFailure(transactionTask.getOnFailure(), "Task has expired!");

                                BlockingQueue<TbTransactionTask> queue = transactionMap.get(transactionTask.getMsg().getTransactionData().getOriginatorId());
                                if (queue != null) {
                                    queue.poll();
                                    TbTransactionTask nextTransactionTask = queue.peek();
                                    if (nextTransactionTask != null) {
                                        executeOnSuccess(nextTransactionTask.getOnStart(), nextTransactionTask.getMsg());
                                    }
                                }
                            } else {
                                sleepDuration = Math.min(expIn, duration);
                            }
                        }
                    } finally {
                        transactionLock.unlock();
                    }
                    if (sleepDuration > 0L) {
                        try {
                            log.trace("Task has not expired! Continue executing...[{}][{}]", transactionTask.getMsg().getId(), transactionTask.getMsg().getType());
                            TimeUnit.MILLISECONDS.sleep(sleepDuration);
                        } catch (InterruptedException e) {
                            throw new IllegalStateException("Thread interrupted", e);
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

    private void executeOnFailure(Consumer<Throwable> onFailure, String exception) {
        executeCallback(() -> {
            onFailure.accept(new RuntimeException(exception));
            return null;
        });
    }

    private void executeOnSuccess(Consumer<TbMsg> onSuccess, TbMsg tbMsg) {
        executeCallback(() -> {
            onSuccess.accept(tbMsg);
            return null;
        });
    }

    private void executeCallback(Callable<Void> task) {
        callbackExecutor.executeAsync(task);
    }

    private void sendTransactionEventToRemoteServer(EntityId entityId, UUID transactionId, ServerAddress address) {
        log.trace("[{}][{}] Originator is monitored on other server: {}", entityId, transactionId, address);
        ClusterAPIProtos.TransactionEndServiceMsgProto.Builder builder = ClusterAPIProtos.TransactionEndServiceMsgProto.newBuilder();
        builder.setEntityType(entityId.getEntityType().name());
        builder.setOriginatorIdMSB(entityId.getId().getMostSignificantBits());
        builder.setOriginatorIdLSB(entityId.getId().getLeastSignificantBits());
        builder.setTransactionIdMSB(transactionId.getMostSignificantBits());
        builder.setTransactionIdLSB(transactionId.getLeastSignificantBits());
        clusterRpcService.tell(address, ClusterAPIProtos.MessageType.CLUSTER_TRANSACTION_SERVICE_MESSAGE, builder.build().toByteArray());
    }
}
