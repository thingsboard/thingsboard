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
package org.thingsboard.server.service.sync.vc;

import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.vc.BranchInfo;
import org.thingsboard.server.common.data.sync.vc.EntityVersion;
import org.thingsboard.server.common.data.sync.vc.EntityVersionsDiff;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionedEntityInfo;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.CommitRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.EntitiesContentRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.EntityContentRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.GenericRepositoryRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ListEntitiesRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ListVersionsRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PrepareMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToVersionControlServiceMsg;
import org.thingsboard.server.gen.transport.TransportProtos.VersionControlResponseMsg;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.scheduler.SchedulerComponent;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.executors.VersionControlExecutor;
import org.thingsboard.server.service.sync.vc.data.ClearRepositoryGitRequest;
import org.thingsboard.server.service.sync.vc.data.CommitGitRequest;
import org.thingsboard.server.service.sync.vc.data.EntitiesContentGitRequest;
import org.thingsboard.server.service.sync.vc.data.EntityContentGitRequest;
import org.thingsboard.server.service.sync.vc.data.ListBranchesGitRequest;
import org.thingsboard.server.service.sync.vc.data.ListEntitiesGitRequest;
import org.thingsboard.server.service.sync.vc.data.ListVersionsGitRequest;
import org.thingsboard.server.service.sync.vc.data.PendingGitRequest;
import org.thingsboard.server.service.sync.vc.data.VersionsDiffGitRequest;
import org.thingsboard.server.service.sync.vc.data.VoidGitRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@TbCoreComponent
@Service
@Slf4j
@SuppressWarnings("UnstableApiUsage")
public class DefaultGitVersionControlQueueService implements GitVersionControlQueueService {

    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbClusterService clusterService;
    private final DefaultEntitiesVersionControlService entitiesVersionControlService;
    private final SchedulerComponent scheduler;
    private final VersionControlExecutor executor;

    private final Map<UUID, PendingGitRequest<?>> pendingRequestMap = new ConcurrentHashMap<>();
    private final Map<UUID, HashMap<Integer, String[]>> chunkedMsgs = new ConcurrentHashMap<>();

    @Value("${queue.vc.request-timeout:180000}")
    private int requestTimeout;
    @Value("${queue.vc.msg-chunk-size:250000}")
    private int msgChunkSize;

    public DefaultGitVersionControlQueueService(TbServiceInfoProvider serviceInfoProvider, TbClusterService clusterService,
                                                @Lazy DefaultEntitiesVersionControlService entitiesVersionControlService,
                                                SchedulerComponent scheduler, VersionControlExecutor executor) {
        this.serviceInfoProvider = serviceInfoProvider;
        this.clusterService = clusterService;
        this.entitiesVersionControlService = entitiesVersionControlService;
        this.scheduler = scheduler;
        this.executor = executor;
    }

    @Override
    public ListenableFuture<CommitGitRequest> prepareCommit(User user, VersionCreateRequest request) {
        log.debug("Executing prepareCommit [{}][{}]", request.getBranch(), request.getVersionName());
        CommitGitRequest commit = new CommitGitRequest(user.getTenantId(), request);
        ListenableFuture<Void> future = registerAndSend(commit, builder -> builder.setCommitRequest(
                buildCommitRequest(commit).setPrepareMsg(getCommitPrepareMsg(user, request)).build()
        ).build());
        return Futures.transform(future, f -> commit, executor);
    }

    @SneakyThrows
    @Override
    public ListenableFuture<Void> addToCommit(CommitGitRequest commit, EntityExportData<ExportableEntity<EntityId>> entityData) {
        log.debug("Executing addToCommit [{}][{}][{}]", entityData.getEntityType(), entityData.getEntity().getId(), commit.getRequestId());
        String path = getRelativePath(entityData.getEntityType(), entityData.getExternalId());
        String entityDataJson = JacksonUtil.toPrettyString(entityData.sort());

        Iterable<String> entityDataChunks = StringUtils.split(entityDataJson, msgChunkSize);
        String chunkedMsgId = UUID.randomUUID().toString();
        int chunksCount = Iterables.size(entityDataChunks);

        AtomicInteger chunkIndex = new AtomicInteger();
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        entityDataChunks.forEach(chunk -> {
            log.trace("[{}] sending chunk {} for 'addToCommit'", chunkedMsgId, chunkIndex.get());
            ListenableFuture<Void> chunkFuture = registerAndSend(commit, builder -> builder.setCommitRequest(
                    buildCommitRequest(commit).setAddMsg(TransportProtos.AddMsg.newBuilder()
                            .setRelativePath(path).setEntityDataJsonChunk(chunk)
                            .setChunkedMsgId(chunkedMsgId).setChunkIndex(chunkIndex.getAndIncrement())
                            .setChunksCount(chunksCount)
                    ).build()
            ).build());
            futures.add(chunkFuture);
        });
        return Futures.transform(Futures.allAsList(futures), r -> {
            log.trace("[{}] sent all chunks for 'addToCommit'", chunkedMsgId);
            return null;
        }, executor);
    }

    @Override
    public ListenableFuture<Void> deleteAll(CommitGitRequest commit, EntityType entityType) {
        log.debug("Executing deleteAll [{}][{}][{}]", commit.getTenantId(), entityType, commit.getRequestId());
        String path = getRelativePath(entityType, null);
        return registerAndSend(commit, builder -> builder.setCommitRequest(
                buildCommitRequest(commit).setDeleteMsg(
                        TransportProtos.DeleteMsg.newBuilder().setRelativePath(path)
                )).build());
    }

    @Override
    public ListenableFuture<VersionCreationResult> push(CommitGitRequest commit) {
        log.debug("Executing push [{}][{}]", commit.getTenantId(), commit.getRequestId());
        return sendRequest(commit, builder -> builder.setCommitRequest(
                buildCommitRequest(commit).setPushMsg(TransportProtos.PushMsg.getDefaultInstance())
        ));
    }

    @Override
    public ListenableFuture<PageData<EntityVersion>> listVersions(TenantId tenantId, String branch, PageLink pageLink) {
        return listVersions(tenantId,
                applyPageLinkParameters(
                        ListVersionsRequestMsg.newBuilder()
                                .setBranchName(branch),
                        pageLink
                ).build());
    }

    @Override
    public ListenableFuture<PageData<EntityVersion>> listVersions(TenantId tenantId, String branch, EntityType entityType, PageLink pageLink) {
        return listVersions(tenantId,
                applyPageLinkParameters(
                        ListVersionsRequestMsg.newBuilder()
                                .setBranchName(branch)
                                .setEntityType(entityType.name()),
                        pageLink
                ).build());
    }

    @Override
    public ListenableFuture<PageData<EntityVersion>> listVersions(TenantId tenantId, String branch, EntityId entityId, PageLink pageLink) {
        return listVersions(tenantId,
                applyPageLinkParameters(
                        ListVersionsRequestMsg.newBuilder()
                                .setBranchName(branch)
                                .setEntityType(entityId.getEntityType().name())
                                .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                                .setEntityIdLSB(entityId.getId().getLeastSignificantBits()),
                        pageLink
                ).build());
    }

    private ListVersionsRequestMsg.Builder applyPageLinkParameters(ListVersionsRequestMsg.Builder builder, PageLink pageLink) {
        builder.setPageSize(pageLink.getPageSize())
                .setPage(pageLink.getPage());
        if (pageLink.getTextSearch() != null) {
            builder.setTextSearch(pageLink.getTextSearch());
        }
        if (pageLink.getSortOrder() != null) {
            if (pageLink.getSortOrder().getProperty() != null) {
                builder.setSortProperty(pageLink.getSortOrder().getProperty());
            }
            if (pageLink.getSortOrder().getDirection() != null) {
                builder.setSortDirection(pageLink.getSortOrder().getDirection().name());
            }
        }
        return builder;
    }

    private ListenableFuture<PageData<EntityVersion>> listVersions(TenantId tenantId, ListVersionsRequestMsg requestMsg) {
        ListVersionsGitRequest request = new ListVersionsGitRequest(tenantId);
        return sendRequest(request, builder -> builder.setListVersionRequest(requestMsg));
    }

    @Override
    public ListenableFuture<List<VersionedEntityInfo>> listEntitiesAtVersion(TenantId tenantId, String versionId, EntityType entityType) {
        return listEntitiesAtVersion(tenantId, ListEntitiesRequestMsg.newBuilder()
                .setVersionId(versionId)
                .setEntityType(entityType.name())
                .build());
    }

    @Override
    public ListenableFuture<List<VersionedEntityInfo>> listEntitiesAtVersion(TenantId tenantId, String versionId) {
        return listEntitiesAtVersion(tenantId, ListEntitiesRequestMsg.newBuilder()
                .setVersionId(versionId)
                .build());
    }

    private ListenableFuture<List<VersionedEntityInfo>> listEntitiesAtVersion(TenantId tenantId, TransportProtos.ListEntitiesRequestMsg requestMsg) {
        ListEntitiesGitRequest request = new ListEntitiesGitRequest(tenantId);
        return sendRequest(request, builder -> builder.setListEntitiesRequest(requestMsg));
    }

    @Override
    public ListenableFuture<List<BranchInfo>> listBranches(TenantId tenantId) {
        ListBranchesGitRequest request = new ListBranchesGitRequest(tenantId);
        return sendRequest(request, builder -> builder.setListBranchesRequest(TransportProtos.ListBranchesRequestMsg.newBuilder().build()));
    }

    @Override
    public ListenableFuture<List<EntityVersionsDiff>> getVersionsDiff(TenantId tenantId, EntityType entityType, EntityId externalId, String versionId1, String versionId2) {
        String path = entityType != null ? getRelativePath(entityType, externalId) : "";
        VersionsDiffGitRequest request = new VersionsDiffGitRequest(tenantId, path, versionId1, versionId2);
        return sendRequest(request, builder -> builder.setVersionsDiffRequest(TransportProtos.VersionsDiffRequestMsg.newBuilder()
                .setPath(request.getPath())
                .setVersionId1(request.getVersionId1())
                .setVersionId2(request.getVersionId2())
                .build()));
    }

    @Override
    @SuppressWarnings("rawtypes")
    public ListenableFuture<EntityExportData> getEntity(TenantId tenantId, String versionId, EntityId entityId) {
        log.debug("Executing getEntity [{}][{}][{}]", tenantId, versionId, entityId);
        EntityContentGitRequest request = new EntityContentGitRequest(tenantId, versionId, entityId);
        chunkedMsgs.put(request.getRequestId(), new HashMap<>());
        return sendRequest(request, builder -> builder.setEntityContentRequest(EntityContentRequestMsg.newBuilder()
                .setVersionId(versionId)
                .setEntityType(entityId.getEntityType().name())
                .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                .setEntityIdLSB(entityId.getId().getLeastSignificantBits())).build());
    }

    private <T> ListenableFuture<Void> registerAndSend(PendingGitRequest<T> request,
                                                       Function<ToVersionControlServiceMsg.Builder, ToVersionControlServiceMsg> enrichFunction) {
        return registerAndSend(request, enrichFunction, null);
    }

    private <T> ListenableFuture<Void> registerAndSend(PendingGitRequest<T> request,
                                                       Function<ToVersionControlServiceMsg.Builder, ToVersionControlServiceMsg> enrichFunction,
                                                       RepositorySettings settings) {
        if (!request.getFuture().isDone()) {
            pendingRequestMap.putIfAbsent(request.getRequestId(), request);
            var requestBody = enrichFunction.apply(newRequestProto(request, settings));
            log.trace("[{}][{}] PUSHING request: {}", request.getTenantId(), request.getRequestId(), requestBody);
            SettableFuture<Void> submitFuture = SettableFuture.create();
            clusterService.pushMsgToVersionControl(request.getTenantId(), requestBody, new TbQueueCallback() {
                @Override
                public void onSuccess(TbQueueMsgMetadata metadata) {
                    submitFuture.set(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    submitFuture.setException(t);
                }
            });
            if (request.getTimeoutTask() == null) {
                request.setTimeoutTask(scheduler.schedule(() -> processTimeout(request.getRequestId()), requestTimeout, TimeUnit.MILLISECONDS));
            }
            return submitFuture;
        } else {
            try {
                request.getFuture().get();
                throw new RuntimeException("Failed to process the request");
            } catch (Exception e) {
                Throwable cause = ExceptionUtils.getRootCause(e);
                throw new RuntimeException(cause.getMessage(), cause);
            }
        }
    }

    private <T> ListenableFuture<T> sendRequest(PendingGitRequest<T> request, Consumer<ToVersionControlServiceMsg.Builder> enrichFunction) {
        return sendRequest(request, enrichFunction, null);
    }

    private <T> ListenableFuture<T> sendRequest(PendingGitRequest<T> request, Consumer<ToVersionControlServiceMsg.Builder> enrichFunction, RepositorySettings settings) {
        ListenableFuture<Void> submitFuture = registerAndSend(request, builder -> {
            enrichFunction.accept(builder);
            return builder.build();
        }, settings);
        return Futures.transformAsync(submitFuture, input -> request.getFuture(), executor);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public ListenableFuture<List<EntityExportData>> getEntities(TenantId tenantId, String versionId, EntityType entityType, int offset, int limit) {
        log.debug("Executing getEntities [{}][{}][{}]", tenantId, versionId, entityType);
        EntitiesContentGitRequest request = new EntitiesContentGitRequest(tenantId, versionId, entityType);
        chunkedMsgs.put(request.getRequestId(), new HashMap<>());
        return sendRequest(request, builder -> builder.setEntitiesContentRequest(
                EntitiesContentRequestMsg.newBuilder()
                        .setVersionId(versionId)
                        .setEntityType(entityType.name())
                        .setOffset(offset)
                        .setLimit(limit)
        ).build());
    }

    @Override
    public ListenableFuture<Void> initRepository(TenantId tenantId, RepositorySettings settings) {
        log.debug("Executing initRepository [{}]", tenantId);
        VoidGitRequest request = new VoidGitRequest(tenantId);
        return sendRequest(request, builder -> builder.setInitRepositoryRequest(GenericRepositoryRequestMsg.getDefaultInstance()), settings);
    }

    @Override
    public ListenableFuture<Void> testRepository(TenantId tenantId, RepositorySettings settings) {
        log.debug("Executing testRepository [{}]", tenantId);
        VoidGitRequest request = new VoidGitRequest(tenantId);
        return sendRequest(request, builder -> builder.setTestRepositoryRequest(GenericRepositoryRequestMsg.getDefaultInstance()), settings);
    }

    @Override
    public ListenableFuture<Void> clearRepository(TenantId tenantId) {
        log.debug("Executing clearRepository [{}]", tenantId);
        ClearRepositoryGitRequest request = new ClearRepositoryGitRequest(tenantId);
        return sendRequest(request, builder -> builder.setClearRepositoryRequest(GenericRepositoryRequestMsg.getDefaultInstance()));
    }

    @Override
    public void processResponse(VersionControlResponseMsg vcResponseMsg) {
        UUID requestId = new UUID(vcResponseMsg.getRequestIdMSB(), vcResponseMsg.getRequestIdLSB());
        PendingGitRequest<?> request = pendingRequestMap.get(requestId);
        if (request == null) {
            log.debug("[{}] received stale response: {}", requestId, vcResponseMsg);
            return;
        } else {
            log.debug("[{}] processing response: {}", requestId, vcResponseMsg);
        }
        var future = request.getFuture();
        boolean completed = true;
        if (!StringUtils.isEmpty(vcResponseMsg.getError())) {
            future.setException(new RuntimeException(vcResponseMsg.getError()));
        } else {
            try {
                if (vcResponseMsg.hasGenericResponse()) {
                    future.set(null);
                } else if (vcResponseMsg.hasCommitResponse()) {
                    var commitResponse = vcResponseMsg.getCommitResponse();
                    var commitResult = new VersionCreationResult();
                    if (commitResponse.getTs() > 0) {
                        commitResult.setVersion(new EntityVersion(commitResponse.getTs(), commitResponse.getCommitId(), commitResponse.getName(), commitResponse.getAuthor()));
                    }
                    commitResult.setAdded(commitResponse.getAdded());
                    commitResult.setRemoved(commitResponse.getRemoved());
                    commitResult.setModified(commitResponse.getModified());
                    commitResult.setDone(true);
                    ((CommitGitRequest) request).getFuture().set(commitResult);
                } else if (vcResponseMsg.hasListBranchesResponse()) {
                    var listBranchesResponse = vcResponseMsg.getListBranchesResponse();
                    ((ListBranchesGitRequest) request).getFuture().set(listBranchesResponse.getBranchesList().stream().map(this::getBranchInfo).collect(Collectors.toList()));
                } else if (vcResponseMsg.hasListEntitiesResponse()) {
                    var listEntitiesResponse = vcResponseMsg.getListEntitiesResponse();
                    ((ListEntitiesGitRequest) request).getFuture().set(
                            listEntitiesResponse.getEntitiesList().stream().map(this::getVersionedEntityInfo).collect(Collectors.toList()));
                } else if (vcResponseMsg.hasListVersionsResponse()) {
                    var listVersionsResponse = vcResponseMsg.getListVersionsResponse();
                    ((ListVersionsGitRequest) request).getFuture().set(toPageData(listVersionsResponse));
                } else if (vcResponseMsg.hasEntityContentResponse()) {
                    TransportProtos.EntityContentResponseMsg responseMsg = vcResponseMsg.getEntityContentResponse();
                    log.trace("Received chunk {} for 'getEntity'", responseMsg.getChunkIndex());
                    var joined = joinChunks(requestId, responseMsg, 0, 1);
                    if (joined.isPresent()) {
                        log.trace("Collected all chunks for 'getEntity'");
                        ((EntityContentGitRequest) request).getFuture().set(joined.get().get(0));
                    } else {
                        completed = false;
                    }
                } else if (vcResponseMsg.hasEntitiesContentResponse()) {
                    TransportProtos.EntitiesContentResponseMsg responseMsg = vcResponseMsg.getEntitiesContentResponse();
                    TransportProtos.EntityContentResponseMsg item = responseMsg.getItem();
                    if (responseMsg.getItemsCount() > 0) {
                        var joined = joinChunks(requestId, item, responseMsg.getItemIdx(), responseMsg.getItemsCount());
                        if (joined.isPresent()) {
                            ((EntitiesContentGitRequest) request).getFuture().set(joined.get());
                        } else {
                            completed = false;
                        }
                    } else {
                        ((EntitiesContentGitRequest) request).getFuture().set(Collections.emptyList());
                    }
                } else if (vcResponseMsg.hasVersionsDiffResponse()) {
                    TransportProtos.VersionsDiffResponseMsg diffResponse = vcResponseMsg.getVersionsDiffResponse();
                    List<EntityVersionsDiff> entityVersionsDiffList = diffResponse.getDiffList().stream()
                            .map(diff -> EntityVersionsDiff.builder()
                                    .externalId(EntityIdFactory.getByTypeAndUuid(EntityType.valueOf(diff.getEntityType()),
                                            new UUID(diff.getEntityIdMSB(), diff.getEntityIdLSB())))
                                    .entityDataAtVersion1(StringUtils.isNotEmpty(diff.getEntityDataAtVersion1()) ?
                                            toData(diff.getEntityDataAtVersion1()) : null)
                                    .entityDataAtVersion2(StringUtils.isNotEmpty(diff.getEntityDataAtVersion2()) ?
                                            toData(diff.getEntityDataAtVersion2()) : null)
                                    .rawDiff(diff.getRawDiff())
                                    .build())
                            .collect(Collectors.toList());
                    ((VersionsDiffGitRequest) request).getFuture().set(entityVersionsDiffList);
                }
            } catch (Exception e) {
                future.setException(e);
                throw e;
            }
        }
        if (completed) {
            removePendingRequest(requestId);
        }
    }

    @SuppressWarnings("rawtypes")
    private Optional<List<EntityExportData>> joinChunks(UUID requestId, TransportProtos.EntityContentResponseMsg responseMsg, int itemIdx, int expectedMsgCount) {
        var chunksMap = chunkedMsgs.get(requestId);
        if (chunksMap == null) {
            return Optional.empty();
        }
        String[] msgChunks = chunksMap.computeIfAbsent(itemIdx, id -> new String[responseMsg.getChunksCount()]);
        msgChunks[responseMsg.getChunkIndex()] = responseMsg.getData();
        if (chunksMap.size() == expectedMsgCount && chunksMap.values().stream()
                .allMatch(chunks -> CollectionsUtil.countNonNull(chunks) == chunks.length)) {
            return Optional.of(chunksMap.entrySet().stream()
                    .sorted(Comparator.comparingInt(Map.Entry::getKey)).map(Map.Entry::getValue)
                    .map(chunks -> String.join("", chunks))
                    .map(this::toData)
                    .collect(Collectors.toList()));
        } else {
            return Optional.empty();
        }
    }

    private void processTimeout(UUID requestId) {
        PendingGitRequest<?> pendingRequest = removePendingRequest(requestId);
        if (pendingRequest != null) {
            log.debug("[{}] request timed out ({} ms}", requestId, requestTimeout);
            pendingRequest.getFuture().setException(new TimeoutException("Request timed out"));
        }
    }

    private PendingGitRequest<?> removePendingRequest(UUID requestId) {
        PendingGitRequest<?> pendingRequest = pendingRequestMap.remove(requestId);
        if (pendingRequest != null && pendingRequest.getTimeoutTask() != null) {
            pendingRequest.getTimeoutTask().cancel(true);
            pendingRequest.setTimeoutTask(null);
        }
        chunkedMsgs.remove(requestId);
        return pendingRequest;
    }

    private PageData<EntityVersion> toPageData(TransportProtos.ListVersionsResponseMsg listVersionsResponse) {
        var listVersions = listVersionsResponse.getVersionsList().stream().map(this::getEntityVersion).collect(Collectors.toList());
        return new PageData<>(listVersions, listVersionsResponse.getTotalPages(), listVersionsResponse.getTotalElements(), listVersionsResponse.getHasNext());
    }

    private EntityVersion getEntityVersion(TransportProtos.EntityVersionProto proto) {
        return new EntityVersion(proto.getTs(), proto.getId(), proto.getName(), proto.getAuthor());
    }

    private VersionedEntityInfo getVersionedEntityInfo(TransportProtos.VersionedEntityInfoProto proto) {
        return new VersionedEntityInfo(EntityIdFactory.getByTypeAndUuid(proto.getEntityType(), new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB())));
    }

    private BranchInfo getBranchInfo(TransportProtos.BranchInfoProto proto) {
        return new BranchInfo(proto.getName(), proto.getIsDefault());
    }

    @SuppressWarnings("rawtypes")
    @SneakyThrows
    private EntityExportData toData(String data) {
        return JacksonUtil.fromString(data, EntityExportData.class);
    }

    private static String getRelativePath(EntityType entityType, EntityId entityId) {
        String path = entityType.name().toLowerCase();
        if (entityId != null) {
            path += "/" + entityId + ".json";
        }
        return path;
    }

    private static PrepareMsg getCommitPrepareMsg(User user, VersionCreateRequest request) {
        return PrepareMsg.newBuilder().setCommitMsg(request.getVersionName())
                .setBranchName(request.getBranch()).setAuthorName(getAuthorName(user)).setAuthorEmail(user.getEmail()).build();
    }

    private static String getAuthorName(User user) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.isNotBlank(user.getFirstName())) {
            parts.add(user.getFirstName());
        }
        if (StringUtils.isNotBlank(user.getLastName())) {
            parts.add(user.getLastName());
        }
        if (parts.isEmpty()) {
            parts.add(user.getName());
        }
        return String.join(" ", parts);
    }

    private ToVersionControlServiceMsg.Builder newRequestProto(PendingGitRequest<?> request, RepositorySettings settings) {
        var tenantId = request.getTenantId();
        var requestId = request.getRequestId();
        var builder = ToVersionControlServiceMsg.newBuilder()
                .setNodeId(serviceInfoProvider.getServiceId())
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setRequestIdMSB(requestId.getMostSignificantBits())
                .setRequestIdLSB(requestId.getLeastSignificantBits());
        RepositorySettings vcSettings = settings;
        if (vcSettings == null && request.requiresSettings()) {
            vcSettings = entitiesVersionControlService.getVersionControlSettings(tenantId);
        }
        if (vcSettings != null) {
            builder.setVcSettings(ProtoUtils.toProto(vcSettings));
        } else if (request.requiresSettings()) {
            throw new RuntimeException("No entity version control settings provisioned!");
        }
        return builder;
    }

    private CommitRequestMsg.Builder buildCommitRequest(CommitGitRequest commit) {
        return CommitRequestMsg.newBuilder().setTxId(commit.getTxId().toString());
    }

}

