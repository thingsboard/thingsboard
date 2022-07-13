/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.sync.vc;

import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.CollectionsUtil;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionedEntityInfo;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.AddMsg;
import org.thingsboard.server.gen.transport.TransportProtos.BranchInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.CommitRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.CommitResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.DeleteMsg;
import org.thingsboard.server.gen.transport.TransportProtos.EntitiesContentRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.EntitiesContentResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.EntityContentRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.EntityContentResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.EntityVersionProto;
import org.thingsboard.server.gen.transport.TransportProtos.ListBranchesRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ListBranchesResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ListEntitiesRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ListEntitiesResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ListVersionsRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ListVersionsResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PrepareMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreNotificationMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToVersionControlServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.VersionControlResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.VersionedEntityInfoProto;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.NotificationsTopicService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbApplicationEventListener;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.provider.TbVersionControlQueueFactory;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbVersionControlComponent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.thingsboard.server.service.sync.vc.DefaultGitRepositoryService.fromRelativePath;

@Slf4j
@TbVersionControlComponent
@Service
@RequiredArgsConstructor
public class DefaultClusterVersionControlService extends TbApplicationEventListener<PartitionChangeEvent> implements ClusterVersionControlService {

    private final PartitionService partitionService;
    private final TbQueueProducerProvider producerProvider;
    private final TbVersionControlQueueFactory queueFactory;
    private final DataDecodingEncodingService encodingService;
    private final GitRepositoryService vcService;
    private final NotificationsTopicService notificationsTopicService;

    private final ConcurrentMap<TenantId, Lock> tenantRepoLocks = new ConcurrentHashMap<>();
    private final Map<TenantId, PendingCommit> pendingCommitMap = new HashMap<>();

    private volatile ExecutorService consumerExecutor;
    private volatile TbQueueConsumer<TbProtoQueueMsg<ToVersionControlServiceMsg>> consumer;
    private volatile TbQueueProducer<TbProtoQueueMsg<ToCoreNotificationMsg>> producer;
    private volatile boolean stopped = false;

    @Value("${queue.vc.poll-interval:25}")
    private long pollDuration;
    @Value("${queue.vc.pack-processing-timeout:60000}")
    private long packProcessingTimeout;
    @Value("${vc.git.io_pool_size:3}")
    private int ioPoolSize;
    @Value("${queue.vc.msg-chunk-size:500000}")
    private int msgChunkSize;

    //We need to manually manage the threads since tasks for particular tenant need to be processed sequentially.
    private final List<ListeningExecutorService> ioThreads = new ArrayList<>();


    @PostConstruct
    public void init() {
        consumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("vc-consumer"));
        var threadFactory = ThingsBoardThreadFactory.forName("vc-io-thread");
        for (int i = 0; i < ioPoolSize; i++) {
            ioThreads.add(MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor(threadFactory)));
        }
        producer = producerProvider.getTbCoreNotificationsMsgProducer();
        consumer = queueFactory.createToVersionControlMsgConsumer();
    }

    @PreDestroy
    public void stop() {
        stopped = true;
        if (consumer != null) {
            consumer.unsubscribe();
        }
        if (consumerExecutor != null) {
            consumerExecutor.shutdownNow();
        }
        ioThreads.forEach(ExecutorService::shutdownNow);
    }

    @Override
    protected void onTbApplicationEvent(PartitionChangeEvent event) {
        for (TenantId tenantId : vcService.getActiveRepositoryTenants()) {
            if (!partitionService.resolve(ServiceType.TB_VC_EXECUTOR, tenantId, tenantId).isMyPartition()) {
                var lock = getRepoLock(tenantId);
                lock.lock();
                try {
                    pendingCommitMap.remove(tenantId);
                    vcService.clearRepository(tenantId);
                } catch (Exception e) {
                    log.warn("[{}] Failed to cleanup the tenant repository", tenantId, e);
                } finally {
                    lock.unlock();
                }
            }
        }
        consumer.subscribe(event.getPartitions());
    }

    @Override
    protected boolean filterTbApplicationEvent(PartitionChangeEvent event) {
        return ServiceType.TB_VC_EXECUTOR.equals(event.getServiceType());
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(value = 2)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        consumerExecutor.execute(() -> consumerLoop(consumer));
    }

    void consumerLoop(TbQueueConsumer<TbProtoQueueMsg<ToVersionControlServiceMsg>> consumer) {
        while (!stopped && !consumer.isStopped()) {
            List<ListenableFuture<?>> futures = new ArrayList<>();
            try {
                List<TbProtoQueueMsg<ToVersionControlServiceMsg>> msgs = consumer.poll(pollDuration);
                if (msgs.isEmpty()) {
                    continue;
                }
                for (TbProtoQueueMsg<ToVersionControlServiceMsg> msgWrapper : msgs) {
                    ToVersionControlServiceMsg msg = msgWrapper.getValue();
                    var ctx = new VersionControlRequestCtx(msg, msg.hasClearRepositoryRequest() ? null : getEntitiesVersionControlSettings(msg));
                    long startTs = System.currentTimeMillis();
                    log.trace("[{}][{}] RECEIVED task: {}", ctx.getTenantId(), ctx.getRequestId(), msg);
                    int threadIdx = Math.abs(ctx.getTenantId().hashCode() % ioPoolSize);
                    ListenableFuture<Void> future = ioThreads.get(threadIdx).submit(() -> processMessage(ctx, msg));
                    logTaskExecution(ctx, future, startTs);
                    futures.add(future);
                }
                try {
                    Futures.allAsList(futures).get(packProcessingTimeout, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    log.info("Timeout for processing the version control tasks.", e);
                }
                consumer.commit();
            } catch (Exception e) {
                if (!stopped) {
                    log.warn("Failed to obtain version control requests from queue.", e);
                    try {
                        Thread.sleep(pollDuration);
                    } catch (InterruptedException e2) {
                        log.trace("Failed to wait until the server has capacity to handle new version control messages", e2);
                    }
                }
            }
        }
        log.info("TB Version Control request consumer stopped.");
    }

    private Void processMessage(VersionControlRequestCtx ctx, ToVersionControlServiceMsg msg) {
        var lock = getRepoLock(ctx.getTenantId());
        lock.lock();
        try {
            if (msg.hasClearRepositoryRequest()) {
                handleClearRepositoryCommand(ctx);
            } else {
                if (msg.hasTestRepositoryRequest()) {
                    handleTestRepositoryCommand(ctx);
                } else if (msg.hasInitRepositoryRequest()) {
                    handleInitRepositoryCommand(ctx);
                } else {
                    var currentSettings = vcService.getRepositorySettings(ctx.getTenantId());
                    var newSettings = ctx.getSettings();
                    if (!newSettings.equals(currentSettings)) {
                        vcService.initRepository(ctx.getTenantId(), ctx.getSettings());
                    }
                    if (msg.hasCommitRequest()) {
                        handleCommitRequest(ctx, msg.getCommitRequest());
                    } else if (msg.hasListBranchesRequest()) {
                        vcService.fetch(ctx.getTenantId());
                        handleListBranches(ctx, msg.getListBranchesRequest());
                    } else if (msg.hasListEntitiesRequest()) {
                        handleListEntities(ctx, msg.getListEntitiesRequest());
                    } else if (msg.hasListVersionRequest()) {
                        vcService.fetch(ctx.getTenantId());
                        handleListVersions(ctx, msg.getListVersionRequest());
                    } else if (msg.hasEntityContentRequest()) {
                        handleEntityContentRequest(ctx, msg.getEntityContentRequest());
                    } else if (msg.hasEntitiesContentRequest()) {
                        handleEntitiesContentRequest(ctx, msg.getEntitiesContentRequest());
                    } else if (msg.hasVersionsDiffRequest()) {
                        handleVersionsDiffRequest(ctx, msg.getVersionsDiffRequest());
                    }
                }
            }
        } catch (Exception e) {
            reply(ctx, Optional.of(e));
        } finally {
            lock.unlock();
        }
        return null;
    }

    private void handleEntitiesContentRequest(VersionControlRequestCtx ctx, EntitiesContentRequestMsg request) throws Exception {
        var entityType = EntityType.valueOf(request.getEntityType());
        String path = getRelativePath(entityType, null);
        var ids = vcService.listEntitiesAtVersion(ctx.getTenantId(), request.getVersionId(), path)
                .stream().skip(request.getOffset()).limit(request.getLimit()).collect(Collectors.toList());
        if (!ids.isEmpty()) {
            for (VersionedEntityInfo info : ids) {
                var data = vcService.getFileContentAtCommit(ctx.getTenantId(),
                        getRelativePath(info.getExternalId().getEntityType(), info.getExternalId().getId().toString()), request.getVersionId());

                Iterable<String> dataChunks = StringUtils.split(data, msgChunkSize);
                String chunkedMsgId = UUID.randomUUID().toString();
                int chunksCount = Iterables.size(dataChunks);
                AtomicInteger chunkIndex = new AtomicInteger();
                dataChunks.forEach(chunk -> {
                    EntitiesContentResponseMsg.Builder response = EntitiesContentResponseMsg.newBuilder()
                            .setItemsCount(ids.size())
                            .setItem(EntityContentResponseMsg.newBuilder()
                                    .setData(chunk)
                                    .setChunkedMsgId(chunkedMsgId)
                                    .setChunksCount(chunksCount)
                                    .setChunkIndex(chunkIndex.getAndIncrement())
                                    .build());
                    reply(ctx, Optional.empty(), builder -> builder.setEntitiesContentResponse(response));
                });
            }
        } else {
            reply(ctx, Optional.empty(), builder -> builder.setEntitiesContentResponse(
                    EntitiesContentResponseMsg.newBuilder()
                            .setItemsCount(0)));
        }
    }

    private void handleEntityContentRequest(VersionControlRequestCtx ctx, EntityContentRequestMsg request) throws IOException {
        String path = getRelativePath(EntityType.valueOf(request.getEntityType()), new UUID(request.getEntityIdMSB(), request.getEntityIdLSB()).toString());
        String data = vcService.getFileContentAtCommit(ctx.getTenantId(), path, request.getVersionId());

        Iterable<String> dataChunks = StringUtils.split(data, msgChunkSize);
        String chunkedMsgId = UUID.randomUUID().toString();
        int chunksCount = Iterables.size(dataChunks);

        AtomicInteger chunkIndex = new AtomicInteger();
        dataChunks.forEach(chunk -> {
            log.trace("[{}] sending chunk {} for 'getEntity'", chunkedMsgId, chunkIndex.get());
            reply(ctx, Optional.empty(), builder -> builder.setEntityContentResponse(EntityContentResponseMsg.newBuilder()
                    .setData(chunk).setChunkedMsgId(chunkedMsgId).setChunksCount(chunksCount)
                    .setChunkIndex(chunkIndex.getAndIncrement())));
        });
    }

    private void handleListVersions(VersionControlRequestCtx ctx, ListVersionsRequestMsg request) throws Exception {
        String path;
        if (StringUtils.isNotEmpty(request.getEntityType())) {
            var entityType = EntityType.valueOf(request.getEntityType());
            if (request.getEntityIdLSB() != 0 || request.getEntityIdMSB() != 0) {
                path = getRelativePath(entityType, new UUID(request.getEntityIdMSB(), request.getEntityIdLSB()).toString());
            } else {
                path = getRelativePath(entityType, null);
            }
        } else {
            path = null;
        }
        SortOrder sortOrder = null;
        if (StringUtils.isNotEmpty(request.getSortProperty())) {
            var direction = SortOrder.Direction.DESC;
            if (StringUtils.isNotEmpty(request.getSortDirection())) {
                direction = SortOrder.Direction.valueOf(request.getSortDirection());
            }
            sortOrder = new SortOrder(request.getSortProperty(), direction);
        }
        var data = vcService.listVersions(ctx.getTenantId(), request.getBranchName(), path,
                new PageLink(request.getPageSize(), request.getPage(), request.getTextSearch(), sortOrder));
        reply(ctx, Optional.empty(), builder ->
                builder.setListVersionsResponse(ListVersionsResponseMsg.newBuilder()
                        .setTotalPages(data.getTotalPages())
                        .setTotalElements(data.getTotalElements())
                        .setHasNext(data.hasNext())
                        .addAllVersions(data.getData().stream().map(
                                v -> EntityVersionProto.newBuilder().setTs(v.getTimestamp()).setId(v.getId()).setName(v.getName()).setAuthor(v.getAuthor()).build()
                        ).collect(Collectors.toList())))
        );
    }

    private void handleListEntities(VersionControlRequestCtx ctx, ListEntitiesRequestMsg request) throws Exception {
        EntityType entityType = StringUtils.isNotEmpty(request.getEntityType()) ? EntityType.valueOf(request.getEntityType()) : null;
        var path = entityType != null ? getRelativePath(entityType, null) : null;
        var data = vcService.listEntitiesAtVersion(ctx.getTenantId(), request.getVersionId(), path);
        reply(ctx, Optional.empty(), builder ->
                builder.setListEntitiesResponse(ListEntitiesResponseMsg.newBuilder()
                        .addAllEntities(data.stream().map(VersionedEntityInfo::getExternalId).map(
                                id -> VersionedEntityInfoProto.newBuilder()
                                        .setEntityType(id.getEntityType().name())
                                        .setEntityIdMSB(id.getId().getMostSignificantBits())
                                        .setEntityIdLSB(id.getId().getLeastSignificantBits()).build()
                        ).collect(Collectors.toList()))));
    }

    private void handleListBranches(VersionControlRequestCtx ctx, ListBranchesRequestMsg request) {
        var branches = vcService.listBranches(ctx.getTenantId()).stream()
                .map(branchInfo -> BranchInfoProto.newBuilder()
                        .setName(branchInfo.getName())
                        .setIsDefault(branchInfo.isDefault()).build()).collect(Collectors.toList());
        reply(ctx, Optional.empty(), builder -> builder.setListBranchesResponse(ListBranchesResponseMsg.newBuilder().addAllBranches(branches)));
    }

    private void handleVersionsDiffRequest(VersionControlRequestCtx ctx, TransportProtos.VersionsDiffRequestMsg request) throws IOException {
        List<TransportProtos.EntityVersionsDiff> diffList = vcService.getVersionsDiffList(ctx.getTenantId(), request.getPath(), request.getVersionId1(), request.getVersionId2()).stream()
                .map(diff -> {
                    EntityId entityId = fromRelativePath(diff.getFilePath());
                    return TransportProtos.EntityVersionsDiff.newBuilder()
                            .setEntityType(entityId.getEntityType().name())
                            .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                            .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                            .setEntityDataAtVersion1(diff.getFileContentAtCommit1())
                            .setEntityDataAtVersion2(diff.getFileContentAtCommit2())
                            .setRawDiff(diff.getDiffStringValue())
                            .build();
                })
                .collect(Collectors.toList());

        reply(ctx, builder -> builder.setVersionsDiffResponse(TransportProtos.VersionsDiffResponseMsg.newBuilder()
                .addAllDiff(diffList)));
    }

    private void handleCommitRequest(VersionControlRequestCtx ctx, CommitRequestMsg request) throws Exception {
        var tenantId = ctx.getTenantId();
        UUID txId = UUID.fromString(request.getTxId());
        if (request.hasPrepareMsg()) {
            vcService.fetch(ctx.getTenantId());
            prepareCommit(ctx, txId, request.getPrepareMsg());
        } else if (request.hasAbortMsg()) {
            PendingCommit current = pendingCommitMap.get(tenantId);
            if (current != null && current.getTxId().equals(txId)) {
                doAbortCurrentCommit(tenantId, current);
            }
        } else {
            PendingCommit current = pendingCommitMap.get(tenantId);
            if (current != null && current.getTxId().equals(txId)) {
                try {
                    if (request.hasAddMsg()) {
                        addToCommit(ctx, current, request.getAddMsg());
                    } else if (request.hasDeleteMsg()) {
                        deleteFromCommit(ctx, current, request.getDeleteMsg());
                    } else if (request.hasPushMsg()) {
                        var result = vcService.push(current);
                        pendingCommitMap.remove(ctx.getTenantId());
                        reply(ctx, result);
                    }
                } catch (Exception e) {
                    doAbortCurrentCommit(tenantId, current, e);
                    throw e;
                }
            } else {
                log.debug("[{}] Ignore request due to stale commit: {}", txId, request);
            }
        }
    }

    private void prepareCommit(VersionControlRequestCtx ctx, UUID txId, PrepareMsg prepareMsg) {
        var tenantId = ctx.getTenantId();
        var pendingCommit = new PendingCommit(tenantId, ctx.getNodeId(), txId, prepareMsg.getBranchName(),
                prepareMsg.getCommitMsg(), prepareMsg.getAuthorName(), prepareMsg.getAuthorEmail());
        PendingCommit old = pendingCommitMap.get(tenantId);
        if (old != null) {
            doAbortCurrentCommit(tenantId, old);
        }
        pendingCommitMap.put(tenantId, pendingCommit);
        vcService.prepareCommit(pendingCommit);
    }

    private void deleteFromCommit(VersionControlRequestCtx ctx, PendingCommit commit, DeleteMsg deleteMsg) throws IOException {
        vcService.deleteFolderContent(commit, deleteMsg.getRelativePath());
    }

    private void addToCommit(VersionControlRequestCtx ctx, PendingCommit commit, AddMsg addMsg) throws IOException {
        log.trace("[{}] received chunk {} for 'addToCommit'", addMsg.getChunkedMsgId(), addMsg.getChunkIndex());
        Map<String, String[]> chunkedMsgs = commit.getChunkedMsgs();
        String[] msgChunks = chunkedMsgs.computeIfAbsent(addMsg.getChunkedMsgId(), id -> new String[addMsg.getChunksCount()]);
        msgChunks[addMsg.getChunkIndex()] = addMsg.getEntityDataJsonChunk();
        if (CollectionsUtil.countNonNull(msgChunks) == msgChunks.length) {
            log.trace("[{}] collected all chunks for 'addToCommit'", addMsg.getChunkedMsgId());
            String entityDataJson = String.join("", msgChunks);
            chunkedMsgs.remove(addMsg.getChunkedMsgId());
            vcService.add(commit, addMsg.getRelativePath(), entityDataJson);
        }
    }

    private void doAbortCurrentCommit(TenantId tenantId, PendingCommit current) {
        doAbortCurrentCommit(tenantId, current, null);
    }

    private void doAbortCurrentCommit(TenantId tenantId, PendingCommit current, Exception e) {
        vcService.abort(current);
        pendingCommitMap.remove(tenantId);
        //TODO: push notification to core using old.getNodeId() to cancel old commit processing on the caller side.
    }

    private void handleClearRepositoryCommand(VersionControlRequestCtx ctx) {
        try {
            vcService.clearRepository(ctx.getTenantId());
            reply(ctx, Optional.empty());
        } catch (Exception e) {
            log.debug("[{}] Failed to connect to the repository: ", ctx, e);
            reply(ctx, Optional.of(e));
        }
    }

    private void handleInitRepositoryCommand(VersionControlRequestCtx ctx) {
        try {
            vcService.initRepository(ctx.getTenantId(), ctx.getSettings());
            reply(ctx, Optional.empty());
        } catch (Exception e) {
            log.debug("[{}] Failed to connect to the repository: ", ctx, e);
            reply(ctx, Optional.of(e));
        }
    }


    private void handleTestRepositoryCommand(VersionControlRequestCtx ctx) {
        try {
            vcService.testRepository(ctx.getTenantId(), ctx.getSettings());
            reply(ctx, Optional.empty());
        } catch (Exception e) {
            log.debug("[{}] Failed to connect to the repository: ", ctx, e);
            reply(ctx, Optional.of(e));
        }
    }

    private void reply(VersionControlRequestCtx ctx, VersionCreationResult result) {
        var responseBuilder = CommitResponseMsg.newBuilder().setAdded(result.getAdded())
                .setModified(result.getModified())
                .setRemoved(result.getRemoved());

        if (result.getVersion() != null) {
            responseBuilder.setTs(result.getVersion().getTimestamp())
                    .setCommitId(result.getVersion().getId())
                    .setName(result.getVersion().getName())
                    .setAuthor(result.getVersion().getAuthor());
        }

        reply(ctx, Optional.empty(), builder -> builder.setCommitResponse(responseBuilder));
    }

    private void reply(VersionControlRequestCtx ctx, Optional<Exception> e) {
        reply(ctx, e, null);
    }

    private void reply(VersionControlRequestCtx ctx, Function<VersionControlResponseMsg.Builder, VersionControlResponseMsg.Builder> enrichFunction) {
        reply(ctx, Optional.empty(), enrichFunction);
    }

    private void reply(VersionControlRequestCtx ctx, Optional<Exception> e, Function<VersionControlResponseMsg.Builder, VersionControlResponseMsg.Builder> enrichFunction) {
        TopicPartitionInfo tpi = notificationsTopicService.getNotificationsTopic(ServiceType.TB_CORE, ctx.getNodeId());
        VersionControlResponseMsg.Builder builder = VersionControlResponseMsg.newBuilder()
                .setRequestIdMSB(ctx.getRequestId().getMostSignificantBits())
                .setRequestIdLSB(ctx.getRequestId().getLeastSignificantBits());
        if (e.isPresent()) {
            log.debug("[{}][{}] Failed to process task", ctx.getTenantId(), ctx.getRequestId(), e.get());
            var message = e.get().getMessage();
            builder.setError(message != null ? message : e.get().getClass().getSimpleName());
        } else {
            if (enrichFunction != null) {
                builder = enrichFunction.apply(builder);
            } else {
                builder.setGenericResponse(TransportProtos.GenericRepositoryResponseMsg.newBuilder().build());
            }
            log.debug("[{}][{}] Processed task", ctx.getTenantId(), ctx.getRequestId());
        }

        ToCoreNotificationMsg msg = ToCoreNotificationMsg.newBuilder().setVcResponseMsg(builder).build();
        log.trace("[{}][{}] PUSHING reply: {} to: {}", ctx.getTenantId(), ctx.getRequestId(), msg, tpi);
        producer.send(tpi, new TbProtoQueueMsg<>(UUID.randomUUID(), msg), null);
    }

    private RepositorySettings getEntitiesVersionControlSettings(ToVersionControlServiceMsg msg) {
        Optional<RepositorySettings> settingsOpt = encodingService.decode(msg.getVcSettings().toByteArray());
        if (settingsOpt.isPresent()) {
            return settingsOpt.get();
        } else {
            log.warn("Failed to parse VC settings: {}", msg.getVcSettings());
            throw new RuntimeException("Failed to parse vc settings!");
        }
    }

    private String getRelativePath(EntityType entityType, String entityId) {
        String path = entityType.name().toLowerCase();
        if (entityId != null) {
            path += "/" + entityId + ".json";
        }
        return path;
    }

    private Lock getRepoLock(TenantId tenantId) {
        return tenantRepoLocks.computeIfAbsent(tenantId, t -> new ReentrantLock(true));
    }

    private void logTaskExecution(VersionControlRequestCtx ctx, ListenableFuture<Void> future, long startTs) {
        if (log.isTraceEnabled()) {
            Futures.addCallback(future, new FutureCallback<Object>() {

                @Override
                public void onSuccess(@Nullable Object result) {
                    log.trace("[{}][{}] Task processing took: {}ms", ctx.getTenantId(), ctx.getRequestId(), (System.currentTimeMillis() - startTs));
                }

                @Override
                public void onFailure(Throwable t) {
                    log.trace("[{}][{}] Task failed: ", ctx.getTenantId(), ctx.getRequestId(), t);
                }
            }, MoreExecutors.directExecutor());
        }
    }
}
