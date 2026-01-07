/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.thingsboard.server.edqs.util.EdqsMapper;
import org.thingsboard.server.edqs.util.VersionsStore;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.EdqsEventMsg;
import org.thingsboard.server.gen.transport.TransportProtos.FromEdqsMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdqsMsg;
import org.thingsboard.server.queue.TbQueueHandler;
import org.thingsboard.server.queue.common.PartitionedQueueResponseTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.consumer.PartitionedQueueConsumerManager;
import org.thingsboard.server.queue.discovery.DiscoveryService;
import org.thingsboard.server.queue.discovery.QueueKey;
import org.thingsboard.server.queue.discovery.TopicService;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.edqs.EdqsComponent;
import org.thingsboard.server.queue.edqs.EdqsConfig;
import org.thingsboard.server.queue.edqs.EdqsConfig.EdqsPartitioningStrategy;
import org.thingsboard.server.queue.edqs.EdqsExecutors;
import org.thingsboard.server.queue.edqs.EdqsQueueFactory;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
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
    private final EdqsMapper mapper;
    private final EdqsRepository repository;
    private final EdqsConfig config;
    private final EdqsExecutors edqsExecutors;
    private final EdqsPartitionService partitionService;
    private final DiscoveryService discoveryService;
    private final TopicService topicService;
    private final ConfigurableApplicationContext applicationContext;
    private final EdqsStateService stateService;

    private PartitionedQueueConsumerManager<TbProtoQueueMsg<ToEdqsMsg>> eventConsumer;
    private PartitionedQueueResponseTemplate<TbProtoQueueMsg<ToEdqsMsg>, TbProtoQueueMsg<FromEdqsMsg>> responseTemplate;
    private ListeningExecutorService requestExecutor;
    private VersionsStore versionsStore;
    private final AtomicInteger counter = new AtomicInteger();

    @Getter
    private Consumer<Throwable> errorHandler;

    @PostConstruct
    private void init() {
        errorHandler = error -> {
            if (error instanceof OutOfMemoryError) {
                log.error("OOM detected, shutting down");
                repository.clear();
                discoveryService.setReady(false);
                Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("edqs-shutdown"))
                        .execute(applicationContext::close);
            }
        };
        requestExecutor = edqsExecutors.getRequestExecutor();
        versionsStore = new VersionsStore(config.getVersionsCacheTtl());

        eventConsumer = PartitionedQueueConsumerManager.<TbProtoQueueMsg<ToEdqsMsg>>create()
                .queueKey(new QueueKey(ServiceType.EDQS, config.getEventsTopic()))
                .topic(topicService.buildTopicName(config.getEventsTopic()))
                .pollInterval(config.getPollInterval())
                .msgPackProcessor((msgs, consumer, consumerKey, config) -> {
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
                .consumerExecutor(edqsExecutors.getConsumersExecutor())
                .taskExecutor(edqsExecutors.getConsumerTaskExecutor())
                .scheduler(edqsExecutors.getScheduler())
                .uncaughtErrorHandler(errorHandler)
                .build();
        responseTemplate = queueFactory.createEdqsResponseTemplate(this);

        stateService.init(eventConsumer, List.of(responseTemplate.getRequestConsumer()));
    }

    @EventListener
    public void onPartitionsChange(PartitionChangeEvent event) {
        if (event.getServiceType() != ServiceType.EDQS) {
            return;
        }
        try {
            Set<TopicPartitionInfo> newPartitions = event.getNewPartitions().get(new QueueKey(ServiceType.EDQS));
            stateService.process(withTopic(newPartitions, topicService.buildTopicName(config.getStateTopic())));
            // partitions for event and request consumers are updated by stateService

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

    @Override
    public TbProtoQueueMsg<FromEdqsMsg> constructErrorResponseMsg(TbProtoQueueMsg<ToEdqsMsg> request, Throwable e) {
        EdqsResponse response = new EdqsResponse();
        String errorMessage;
        if (e instanceof org.apache.kafka.common.errors.RecordTooLargeException) {
            errorMessage = "Result set is too large";
        } else if (e instanceof IllegalArgumentException || e instanceof NullPointerException) {
            errorMessage = "Invalid request format or missing data: " + ExceptionUtil.getMessage(e);
        } else {
            errorMessage = ExceptionUtil.getMessage(e);
        }
        response.setError(errorMessage);
        return new TbProtoQueueMsg<>(request.getKey(), FromEdqsMsg.newBuilder()
                .setResponseMsg(TransportProtos.EdqsResponseMsg.newBuilder()
                        .setValue(JacksonUtil.toString(response))
                        .build())
                .build(), request.getHeaders());
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
            Long version = eventMsg.hasVersion() ? eventMsg.getVersion() : null;
            EdqsObject object = mapper.deserialize(objectType, eventMsg.getData().toByteArray(), false);

            if (version != null) {
                if (!versionsStore.isNew(mapper.getKey(object), version)) {
                    return;
                }
            } else if (!ObjectType.unversionedTypes.contains(objectType)) {
                log.warn("[{}] {} doesn't have version: {}", tenantId, objectType, object);
            }
            if (backup) {
                stateService.save(tenantId, objectType, object.stringKey(), eventType, edqsMsg);
            }

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
            log.debug("Processing event: {}", event);
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
        versionsStore.shutdown();
    }

}
