/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ExceptionUtil;
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
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.edqs.repo.EdqsRepository;
import org.thingsboard.server.edqs.state.EdqsPartitionService;
import org.thingsboard.server.edqs.state.EdqsStateService;
import org.thingsboard.server.edqs.util.EdqsConverter;
import org.thingsboard.server.edqs.util.VersionsStore;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.EdqsEventMsg;
import org.thingsboard.server.gen.transport.TransportProtos.FromEdqsMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdqsMsg;
import org.thingsboard.server.queue.TbQueueHandler;
import org.thingsboard.server.queue.TbQueueResponseTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.PartitionedQueueConsumerManager;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.edqs.EdqsComponent;
import org.thingsboard.server.queue.edqs.EdqsConfig;
import org.thingsboard.server.queue.edqs.EdqsConfig.EdqsPartitioningStrategy;
import org.thingsboard.server.queue.edqs.EdqsQueueFactory;
import org.thingsboard.server.queue.util.AfterStartUp;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.msg.queue.TopicPartitionInfo.withTopic;

@EdqsComponent
@Service
@RequiredArgsConstructor
@Slf4j
public class EdqsProcessor implements TbQueueHandler<TbProtoQueueMsg<ToEdqsMsg>, TbProtoQueueMsg<FromEdqsMsg>> {

    private final EdqsQueueFactory queueFactory;
    private final EdqsConverter converter;
    private final EdqsRepository repository;
    private final EdqsConfig config;
    private final EdqsPartitionService partitionService;
    private final ConfigurableApplicationContext applicationContext;
    private final EdqsStateService stateService;

    private PartitionedQueueConsumerManager<TbProtoQueueMsg<ToEdqsMsg>> eventConsumer;
    private TbQueueResponseTemplate<TbProtoQueueMsg<ToEdqsMsg>, TbProtoQueueMsg<FromEdqsMsg>> responseTemplate;

    private ExecutorService consumersExecutor;
    private ExecutorService taskExecutor;
    private ScheduledExecutorService scheduler;
    private ListeningExecutorService requestExecutor;

    private final VersionsStore versionsStore = new VersionsStore();

    private final AtomicInteger counter = new AtomicInteger();

    @Getter
    private Consumer<Throwable> errorHandler;

    @PostConstruct
    private void init() {
        consumersExecutor = Executors.newCachedThreadPool(ThingsBoardThreadFactory.forName("edqs-consumer"));
        taskExecutor = ThingsBoardExecutors.newWorkStealingPool(4, "edqs-consumer-task-executor");
        scheduler = ThingsBoardExecutors.newSingleThreadScheduledExecutor("edqs-scheduler");
        requestExecutor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(12, "edqs-requests"));
        errorHandler = error -> {
            if (error instanceof OutOfMemoryError) {
                log.error("OOM detected, shutting down");
                repository.clear();
                Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("edqs-shutdown"))
                        .execute(applicationContext::close);
            }
        };

        eventConsumer = PartitionedQueueConsumerManager.<TbProtoQueueMsg<ToEdqsMsg>>create()
                .queueKey(new QueueKey(ServiceType.EDQS, config.getEventsTopic()))
                .topic(config.getEventsTopic())
                .pollInterval(config.getPollInterval())
                .msgPackProcessor((msgs, consumer, config) -> {
                    for (TbProtoQueueMsg<ToEdqsMsg> queueMsg : msgs) {
                        if (consumer.isStopped()) {
                            return;
                        }
                        try {
                            ToEdqsMsg msg = queueMsg.getValue();
                            process(msg, true);
                        } catch (Exception t) {
                            log.error("Failed to process message: {}", queueMsg, t);
                        }
                    }
                    consumer.commit();
                })
                .consumerCreator((config, tpi) -> queueFactory.createEdqsEventsConsumer())
                .queueAdmin(queueFactory.getEdqsQueueAdmin())
                .consumerExecutor(consumersExecutor)
                .taskExecutor(taskExecutor)
                .scheduler(scheduler)
                .uncaughtErrorHandler(errorHandler)
                .build();
        stateService.init(eventConsumer);

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
        try {
            Set<TopicPartitionInfo> newPartitions = event.getNewPartitions().get(new QueueKey(ServiceType.EDQS));

            stateService.process(withTopic(newPartitions, config.getStateTopic()));
            // eventsConsumer's partitions are updated by stateService
            responseTemplate.subscribe(withTopic(newPartitions, config.getRequestsTopic())); // TODO: we subscribe to partitions before we are ready. implement consumer-per-partition version for request template

            Set<TopicPartitionInfo> oldPartitions = event.getOldPartitions().get(new QueueKey(ServiceType.EDQS));
            if (CollectionsUtil.isNotEmpty(oldPartitions)) {
                Set<Integer> removedPartitions = Sets.difference(oldPartitions, newPartitions).stream()
                        .map(tpi -> tpi.getPartition().orElse(-1)).collect(Collectors.toSet());
                if (removedPartitions.isEmpty()) {
                    return;
                }

                if (config.getPartitioningStrategy() == EdqsPartitioningStrategy.TENANT) {
                    repository.clearIf(tenantId -> {
                        Integer partition = partitionService.resolvePartition(tenantId, null);
                        return removedPartitions.contains(partition);
                    });
                } else {
                    log.warn("Partitions {} were removed but shouldn't be (due to NONE partitioning strategy)", removedPartitions);
                }
            }
        } catch (Throwable t) {
            log.error("Failed to handle partition change event {}", event, t);
        }
    }

    @Override
    public ListenableFuture<TbProtoQueueMsg<FromEdqsMsg>> handle(TbProtoQueueMsg<ToEdqsMsg> queueMsg) {
        ToEdqsMsg toEdqsMsg = queueMsg.getValue();
        return requestExecutor.submit(() -> {
            EdqsRequest request;
            TenantId tenantId;
            CustomerId customerId;
            try {
                request = Objects.requireNonNull(JacksonUtil.fromString(toEdqsMsg.getRequestMsg().getValue(), EdqsRequest.class));
                tenantId = getTenantId(toEdqsMsg);
                customerId = getCustomerId(toEdqsMsg);
            } catch (Exception e) {
                log.error("Failed to parse request msg: {}", toEdqsMsg, e);
                throw e;
            }

            EdqsResponse response = processRequest(tenantId, customerId, request);
            return new TbProtoQueueMsg<>(queueMsg.getKey(), FromEdqsMsg.newBuilder()
                    .setResponseMsg(TransportProtos.EdqsResponseMsg.newBuilder()
                            .setValue(JacksonUtil.toString(response))
                            .build())
                    .build(), queueMsg.getHeaders());
        });
    }

    private EdqsResponse processRequest(TenantId tenantId, CustomerId customerId, EdqsRequest request) {
        EdqsResponse response = new EdqsResponse();
        try {
            if (request.getEntityDataQuery() != null) {
                PageData<QueryResult> result = repository.findEntityDataByQuery(tenantId, customerId,
                        request.getEntityDataQuery(), false);
                response.setEntityDataQueryResult(result.mapData(QueryResult::toOldEntityData));
            } else if (request.getEntityCountQuery() != null) {
                long result = repository.countEntitiesByQuery(tenantId, customerId, request.getEntityCountQuery(), tenantId.isSysTenantId());
                response.setEntityCountQueryResult(result);
            }
            log.trace("[{}] Request: {}, response: {}", tenantId, request, response);
        } catch (Throwable e) {
            log.error("[{}] Failed to process request: {}", tenantId, request, e);
            response.setError(ExceptionUtil.getMessage(e));
        }
        return response;
    }

    public void process(ToEdqsMsg edqsMsg, boolean backup) {
        log.trace("Processing message: {}", edqsMsg);
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
            } else if (!ObjectType.unversionedTypes.contains(objectType)) {
                log.warn("[{}] {} {} doesn't have version", tenantId, objectType, key);
            }
            if (backup) {
                stateService.save(tenantId, objectType, key, eventType, edqsMsg);
            }

            EdqsObject object = converter.deserialize(objectType, eventMsg.getData().toByteArray());
            log.debug("[{}] Processing event [{}] [{}] [{}] [{}]", tenantId, objectType, eventType, key, version);
            int count = counter.incrementAndGet();
            if (count % 100000 == 0) {
                log.info("Processed {} events", count);
            }

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

    @PreDestroy
    public void destroy() throws InterruptedException {
        eventConsumer.stop();
        eventConsumer.awaitStop();
        responseTemplate.stop();
        stateService.stop();

        consumersExecutor.shutdownNow();
        taskExecutor.shutdownNow();
        scheduler.shutdownNow();
        requestExecutor.shutdownNow();
    }

}
