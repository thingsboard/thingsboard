/**
 * Copyright © 2016-2024 ThingsBoard, Inc.
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
package org.thingsboard.server.edqs.processor;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.edqs.EdqsEvent;
import org.thingsboard.server.common.data.edqs.EdqsEventType;
import org.thingsboard.server.common.data.edqs.EdqsObject;
import org.thingsboard.server.common.data.edqs.query.EdqsRequest;
import org.thingsboard.server.common.data.edqs.query.EdqsResponse;
import org.thingsboard.server.common.data.edqs.query.QueryResult;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.queue.QueueConfig;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.edqs.state.EdqsStateService;
import org.thingsboard.server.edqs.repo.EdqRepository;
import org.thingsboard.server.edqs.util.EdqsPartitionService;
import org.thingsboard.server.edqs.util.VersionsStore;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.EdqsEventMsg;
import org.thingsboard.server.gen.transport.TransportProtos.FromEdqsMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdqsMsg;
import org.thingsboard.server.queue.TbQueueHandler;
import org.thingsboard.server.queue.TbQueueResponseTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.MainQueueConsumerManager;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.edqs.EdqsComponent;
import org.thingsboard.server.queue.edqs.EdqsConfig;
import org.thingsboard.server.queue.edqs.EdqsConfig.EdqsPartitioningStrategy;
import org.thingsboard.server.queue.edqs.EdqsQueue;
import org.thingsboard.server.queue.edqs.EdqsQueueFactory;
import org.thingsboard.server.queue.util.AfterStartUp;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

@EdqsComponent
@Service
@RequiredArgsConstructor
@Slf4j
public class EdqsProcessor implements TbQueueHandler<TbProtoQueueMsg<ToEdqsMsg>, TbProtoQueueMsg<FromEdqsMsg>> {

    private final EdqsQueueFactory queueFactory;
    private final EdqsConverter converter;
    private final EdqRepository repository;
    private final EdqsConfig config;
    private final EdqsPartitionService partitionService;
    @Autowired @Lazy
    private EdqsStateService stateService;

    private MainQueueConsumerManager<TbProtoQueueMsg<ToEdqsMsg>, QueueConfig> eventsConsumer;
    private TbQueueResponseTemplate<TbProtoQueueMsg<ToEdqsMsg>, TbProtoQueueMsg<FromEdqsMsg>> responseTemplate;

    private ExecutorService consumersExecutor;
    private ExecutorService mgmtExecutor;
    private ScheduledExecutorService scheduler;
    private ListeningExecutorService requestExecutor;

    private final VersionsStore versionsStore = new VersionsStore();

    @PostConstruct
    private void init() {
        consumersExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName("edqs-consumer"));
        mgmtExecutor = ThingsBoardExecutors.newWorkStealingPool(4, "edqs-consumer-mgmt");
        scheduler = ThingsBoardExecutors.newSingleThreadScheduledExecutor("edqs-scheduler");
        requestExecutor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(12, "edqs-requests"));

        eventsConsumer = MainQueueConsumerManager.<TbProtoQueueMsg<ToEdqsMsg>, QueueConfig>builder()
                .queueKey(new QueueKey(ServiceType.EDQS, EdqsQueue.EVENTS.getTopic()))
                .config(QueueConfig.of(true, config.getPollInterval()))
                .msgPackProcessor((msgs, consumer, config) -> {
                    for (TbProtoQueueMsg<ToEdqsMsg> queueMsg : msgs) {
                        try {
                            ToEdqsMsg msg = queueMsg.getValue();
                            log.trace("Processing message: {}", msg);
                            process(msg, EdqsQueue.EVENTS);
                        } catch (Throwable t) {
                            log.error("Failed to process message: {}", queueMsg, t);
                        }
                    }
                    consumer.commit();
                })
                .consumerCreator((config, partitionId) -> queueFactory.createEdqsMsgConsumer(EdqsQueue.EVENTS))
                .consumerExecutor(consumersExecutor)
                .taskExecutor(mgmtExecutor)
                .scheduler(scheduler)
                .build();
        responseTemplate = queueFactory.createEdqsResponseTemplate();
    }

    @AfterStartUp(order = 1)
    public void start() {
        responseTemplate.launch(this);
    }

    @EventListener
    public void onPartitionsChange(PartitionChangeEvent event) {
        if (event.getServiceType() != ServiceType.EDQS) {
            return;
        }
        consumersExecutor.submit(() -> {
            try {
                Set<TopicPartitionInfo> newPartitions = event.getNewPartitions().get(new QueueKey(ServiceType.EDQS));
                Set<TopicPartitionInfo> partitions = newPartitions.stream()
                        .map(tpi -> tpi.withUseInternalPartition(true))
                        .collect(Collectors.toSet());

                try {
                    stateService.restore(withTopic(partitions, EdqsQueue.STATE.getTopic())); // blocks until restored
                } catch (Exception e) {
                    log.error("Failed to process restore for partitions {}", partitions, e);
                }
                eventsConsumer.update(withTopic(partitions, EdqsQueue.EVENTS.getTopic()));
                responseTemplate.subscribe(withTopic(partitions, config.getRequestsTopic()));

                Set<TopicPartitionInfo> oldPartitions = event.getOldPartitions().get(new QueueKey(ServiceType.EDQS));
                Set<Integer> removedPartitions = Sets.difference(oldPartitions, newPartitions).stream()
                        .map(tpi -> tpi.getPartition().orElse(-1)).collect(Collectors.toSet());
                if (config.getPartitioningStrategy() != EdqsPartitioningStrategy.TENANT && !removedPartitions.isEmpty()) {
                    log.warn("Partitions {} were removed but shouldn't be (due to NONE partitioning strategy)", removedPartitions);
                }
                repository.clearIf(tenantId -> {
                    Integer partition = partitionService.resolvePartition(tenantId);
                    return partition != null && removedPartitions.contains(partition);
                });
            } catch (Throwable t) {
                log.error("Failed to handle partition change event {}", event, t);
            }
        });
    }

    @Override
    public ListenableFuture<TbProtoQueueMsg<FromEdqsMsg>> handle(TbProtoQueueMsg<ToEdqsMsg> queueMsg) {
        ToEdqsMsg toEdqsMsg = queueMsg.getValue();
        return requestExecutor.submit(() -> {
            EdqsResponse response = new EdqsResponse();
            try {
                EdqsRequest request = JacksonUtil.fromString(toEdqsMsg.getRequestMsg().getValue(), EdqsRequest.class);
                TenantId tenantId = getTenantId(toEdqsMsg);
                CustomerId customerId = getCustomerId(toEdqsMsg);
                log.info("[{}] Handling request: {}", tenantId, request);

                if (request.getEntityDataQuery() != null) {
                    PageData<QueryResult> result = repository.findEntityDataByQuery(tenantId, customerId,
                            request.getUserPermissions(), request.getEntityDataQuery(), false);
                    response.setEntityDataQueryResult(result.mapData(QueryResult::toOldEntityData));
                } else if (request.getEntityCountQuery() != null) {
                    long result = repository.countEntitiesByQuery(tenantId, customerId, request.getUserPermissions(), request.getEntityCountQuery(), tenantId.isSysTenantId());
                    response.setEntityCountQueryResult(result);
                }

                log.info("Answering with response: {}", response);
            } catch (Throwable e) {
                response.setError(ExceptionUtils.getStackTrace(e)); // TODO: return only the message
                log.info("Answering with error", e);
            }
            return new TbProtoQueueMsg<>(queueMsg.getKey(), FromEdqsMsg.newBuilder()
                    .setResponseMsg(TransportProtos.EdqsResponseMsg.newBuilder()
                            .setValue(JacksonUtil.toString(response))
                            .build())
                    .build(), queueMsg.getHeaders());
        });
    }

    public void process(ToEdqsMsg edqsMsg, EdqsQueue queue) {
        if (edqsMsg.hasEventMsg()) {
            EdqsEventMsg eventMsg = edqsMsg.getEventMsg();
            TenantId tenantId = getTenantId(edqsMsg);
            ObjectType objectType = ObjectType.valueOf(eventMsg.getObjectType());
            EdqsEventType eventType = EdqsEventType.valueOf(eventMsg.getEventType());
            String key = eventMsg.getKey();
            Long version = eventMsg.hasVersion() ? eventMsg.getVersion() : null;

            if (version != null) {
                if (!versionsStore.isNew(key, version)) {
                    return;
                }
            } else {
                log.warn("[{}] {} doesn't have version: {}", tenantId, objectType, edqsMsg);
            }
            if (queue != EdqsQueue.STATE) {
                stateService.save(tenantId, objectType, key, eventType, edqsMsg);
            }

            EdqsObject object = converter.deserialize(objectType, eventMsg.getData().toByteArray());
            log.info("[{}] Processing event [{}] [{}] [{}] [{}]", tenantId, objectType, eventType, key, version);

            EdqsEvent event = EdqsEvent.builder()
                    .tenantId(tenantId)
                    .objectType(objectType)
                    .eventType(eventType)
                    .object(object)
                    .build();
            repository.processEvent(event);
        }
    }

    private TenantId getTenantId(ToEdqsMsg edqsMsg) {
        return TenantId.fromUUID(new UUID(edqsMsg.getTenantIdMSB(), edqsMsg.getTenantIdLSB()));
    }

    private CustomerId getCustomerId(ToEdqsMsg edqsMsg) {
        if (edqsMsg.getCustomerIdMSB() != 0 && edqsMsg.getCustomerIdLSB() != 0) {
            return new CustomerId(new UUID(edqsMsg.getCustomerIdMSB(), edqsMsg.getCustomerIdLSB()));
        } else {
            return null;
        }
    }

    private Set<TopicPartitionInfo> withTopic(Set<TopicPartitionInfo> partitions, String topic) {
        return partitions.stream()
                .map(tpi -> tpi.withTopic(topic))
                .collect(Collectors.toSet());
    }

    @PreDestroy
    public void destroy() throws InterruptedException {
        eventsConsumer.stop();
        eventsConsumer.awaitStop();
        responseTemplate.stop();

        consumersExecutor.shutdownNow();
        mgmtExecutor.shutdownNow();
        scheduler.shutdownNow();
        requestExecutor.shutdownNow();
    }

}
