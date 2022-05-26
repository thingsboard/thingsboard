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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ByteString;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.vc.EntitiesVersionControlSettings;
import org.thingsboard.server.common.data.sync.vc.EntityVersion;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionedEntityInfo;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;
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
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@TbCoreComponent
@Service
@Slf4j
public class DefaultGitVersionControlQueueService implements GitVersionControlQueueService {

    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbClusterService clusterService;
    private final DataDecodingEncodingService encodingService;
    private final DefaultEntitiesVersionControlService entitiesVersionControlService;

    private final Map<UUID, PendingGitRequest<?>> pendingRequestMap = new HashMap<>();
    private final ObjectMapper jsonMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public DefaultGitVersionControlQueueService(TbServiceInfoProvider serviceInfoProvider, TbClusterService clusterService,
                                                DataDecodingEncodingService encodingService,
                                                @Lazy DefaultEntitiesVersionControlService entitiesVersionControlService) {
        this.serviceInfoProvider = serviceInfoProvider;
        this.clusterService = clusterService;
        this.encodingService = encodingService;
        this.entitiesVersionControlService = entitiesVersionControlService;
    }

    @Override
    public ListenableFuture<CommitGitRequest> prepareCommit(TenantId tenantId, VersionCreateRequest request) {
        SettableFuture<CommitGitRequest> future = SettableFuture.create();

        CommitGitRequest commit = new CommitGitRequest(tenantId, request);
        registerAndSend(commit, builder -> builder.setCommitRequest(
                buildCommitRequest(commit).setPrepareMsg(getCommitPrepareMsg(request)).build()
        ).build(), wrap(future, commit));
        return future;
    }

    @Override
    public ListenableFuture<Void> addToCommit(CommitGitRequest commit, EntityExportData<ExportableEntity<EntityId>> entityData) {
        SettableFuture<Void> future = SettableFuture.create();

        String path = getRelativePath(entityData.getEntityType(), entityData.getEntity().getId());
        String entityDataJson;
        try {
            entityDataJson = jsonMapper.writeValueAsString(entityData);
        } catch (IOException e) {
            //TODO: analyze and return meaningful exceptions that we can show to the client;
            throw new RuntimeException(e);
        }

        registerAndSend(commit, builder -> builder.setCommitRequest(
                buildCommitRequest(commit).setAddMsg(
                        TransportProtos.AddMsg.newBuilder()
                                .setRelativePath(path).setEntityDataJson(entityDataJson).build()
                ).build()
        ).build(), wrap(future, null));
        return future;
    }

    @Override
    public ListenableFuture<Void> deleteAll(CommitGitRequest commit, EntityType entityType) {
        SettableFuture<Void> future = SettableFuture.create();

        String path = getRelativePath(entityType, null);

        registerAndSend(commit, builder -> builder.setCommitRequest(
                buildCommitRequest(commit).setDeleteMsg(
                        TransportProtos.DeleteMsg.newBuilder().setRelativePath(path).build()
                ).build()
        ).build(), wrap(commit.getFuture(), null));

        return future;
    }

    @Override
    public ListenableFuture<VersionCreationResult> push(CommitGitRequest commit) {
        registerAndSend(commit, builder -> builder.setCommitRequest(
                buildCommitRequest(commit).setPushMsg(
                        TransportProtos.PushMsg.newBuilder().build()
                ).build()
        ).build(), wrap(commit.getFuture()));

        return commit.getFuture();
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

        registerAndSend(request, builder -> builder.setListVersionRequest(requestMsg).build(), wrap(request.getFuture()));

        return request.getFuture();
    }

    @Override
    public ListenableFuture<List<VersionedEntityInfo>> listEntitiesAtVersion(TenantId tenantId, String branch, String versionId, EntityType entityType) {
        return listEntitiesAtVersion(tenantId, ListEntitiesRequestMsg.newBuilder()
                .setBranchName(branch)
                .setVersionId(versionId)
                .setEntityType(entityType.name())
                .build());
    }

    @Override
    public ListenableFuture<List<VersionedEntityInfo>> listEntitiesAtVersion(TenantId tenantId, String branch, String versionId) {
        return listEntitiesAtVersion(tenantId, ListEntitiesRequestMsg.newBuilder()
                .setBranchName(branch)
                .setVersionId(versionId)
                .build());
    }

    private ListenableFuture<List<VersionedEntityInfo>> listEntitiesAtVersion(TenantId tenantId, TransportProtos.ListEntitiesRequestMsg requestMsg) {
        ListEntitiesGitRequest request = new ListEntitiesGitRequest(tenantId);

        registerAndSend(request, builder -> builder.setListEntitiesRequest(requestMsg).build(), wrap(request.getFuture()));

        return request.getFuture();
    }

    @Override
    public ListenableFuture<List<String>> listBranches(TenantId tenantId) {
        ListBranchesGitRequest request = new ListBranchesGitRequest(tenantId);

        registerAndSend(request, builder -> builder.setListBranchesRequest(TransportProtos.ListBranchesRequestMsg.newBuilder().build()).build(), wrap(request.getFuture()));

        return request.getFuture();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public ListenableFuture<EntityExportData> getEntity(TenantId tenantId, String versionId, EntityId entityId) {
        EntityContentGitRequest request = new EntityContentGitRequest(tenantId, versionId, entityId);
        registerAndSend(request, builder -> builder.setEntityContentRequest(EntityContentRequestMsg.newBuilder()
                        .setVersionId(versionId)
                        .setEntityType(entityId.getEntityType().name())
                        .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                        .setEntityIdLSB(entityId.getId().getLeastSignificantBits())).build()
                , wrap(request.getFuture()));
        return request.getFuture();
    }

    private <T> void registerAndSend(PendingGitRequest<T> request,
                                     Function<ToVersionControlServiceMsg.Builder, ToVersionControlServiceMsg> enrichFunction, TbQueueCallback callback) {
        registerAndSend(request, enrichFunction, null, callback);
    }

    private <T> void registerAndSend(PendingGitRequest<T> request,
                                     Function<ToVersionControlServiceMsg.Builder, ToVersionControlServiceMsg> enrichFunction, EntitiesVersionControlSettings settings, TbQueueCallback callback) {
        if (!request.getFuture().isDone()) {
            pendingRequestMap.putIfAbsent(request.getRequestId(), request);
            var requestBody = enrichFunction.apply(newRequestProto(request, settings));
            log.trace("[{}][{}] PUSHING request: {}", request.getTenantId(), request.getRequestId(), requestBody);
            clusterService.pushMsgToVersionControl(request.getTenantId(), requestBody, callback);
        } else {
            throw new RuntimeException("Future is already done!");
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public ListenableFuture<List<EntityExportData>> getEntities(TenantId tenantId, String versionId, EntityType entityType, int offset, int limit) {
        EntitiesContentGitRequest request = new EntitiesContentGitRequest(tenantId, versionId, entityType);

        registerAndSend(request, builder -> builder.setEntitiesContentRequest(EntitiesContentRequestMsg.newBuilder()
                        .setVersionId(versionId)
                        .setEntityType(entityType.name())
                        .setOffset(offset)
                        .setLimit(limit)
                ).build()
                , wrap(request.getFuture()));

        return request.getFuture();
    }

    @Override
    public ListenableFuture<Void> initRepository(TenantId tenantId, EntitiesVersionControlSettings settings) {
        VoidGitRequest request = new VoidGitRequest(tenantId);

        registerAndSend(request, builder -> builder.setInitRepositoryRequest(GenericRepositoryRequestMsg.newBuilder().build()).build()
                , settings, wrap(request.getFuture()));

        return request.getFuture();
    }

    @Override
    public ListenableFuture<Void> testRepository(TenantId tenantId, EntitiesVersionControlSettings settings) {
        VoidGitRequest request = new VoidGitRequest(tenantId);

        registerAndSend(request, builder -> builder
                        .setTestRepositoryRequest(GenericRepositoryRequestMsg.newBuilder().build()).build()
                , settings, wrap(request.getFuture()));

        return request.getFuture();
    }

    @Override
    public ListenableFuture<Void> clearRepository(TenantId tenantId) {
        ClearRepositoryGitRequest request = new ClearRepositoryGitRequest(tenantId);

        registerAndSend(request, builder -> builder.setClearRepositoryRequest(GenericRepositoryRequestMsg.newBuilder().build()).build()
                , wrap(request.getFuture()));

        return request.getFuture();
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
        if (!StringUtils.isEmpty(vcResponseMsg.getError())) {
            future.setException(new RuntimeException(vcResponseMsg.getError()));
        } else {
            if (vcResponseMsg.hasGenericResponse()) {
                future.set(null);
            } else if (vcResponseMsg.hasCommitResponse()) {
                var commitResponse = vcResponseMsg.getCommitResponse();
                var commitResult = new VersionCreationResult();
                commitResult.setVersion(new EntityVersion(commitResponse.getTs(), commitResponse.getCommitId(), commitResponse.getName()));
                commitResult.setAdded(commitResponse.getAdded());
                commitResult.setRemoved(commitResponse.getRemoved());
                commitResult.setModified(commitResponse.getModified());
                ((CommitGitRequest) request).getFuture().set(commitResult);
            } else if (vcResponseMsg.hasListBranchesResponse()) {
                var listBranchesResponse = vcResponseMsg.getListBranchesResponse();
                ((ListBranchesGitRequest) request).getFuture().set(listBranchesResponse.getBranchesList());
            } else if (vcResponseMsg.hasListEntitiesResponse()) {
                var listEntitiesResponse = vcResponseMsg.getListEntitiesResponse();
                ((ListEntitiesGitRequest) request).getFuture().set(
                        listEntitiesResponse.getEntitiesList().stream().map(this::getVersionedEntityInfo).collect(Collectors.toList()));
            } else if (vcResponseMsg.hasListVersionsResponse()) {
                var listVersionsResponse = vcResponseMsg.getListVersionsResponse();
                ((ListVersionsGitRequest) request).getFuture().set(toPageData(listVersionsResponse));
            } else if (vcResponseMsg.hasEntityContentResponse()) {
                var data = vcResponseMsg.getEntityContentResponse().getData();
                ((EntityContentGitRequest) request).getFuture().set(toData(data));
            } else if (vcResponseMsg.hasEntitiesContentResponse()) {
                var dataList = vcResponseMsg.getEntitiesContentResponse().getDataList();
                ((EntitiesContentGitRequest) request).getFuture()
                        .set(dataList.stream().map(this::toData).collect(Collectors.toList()));
            }
        }
    }

    private PageData<EntityVersion> toPageData(TransportProtos.ListVersionsResponseMsg listVersionsResponse) {
        var listVersions = listVersionsResponse.getVersionsList().stream().map(this::getEntityVersion).collect(Collectors.toList());
        return new PageData<>(listVersions, listVersionsResponse.getTotalPages(), listVersionsResponse.getTotalElements(), listVersionsResponse.getHasNext());
    }

    private EntityVersion getEntityVersion(TransportProtos.EntityVersionProto proto) {
        return new EntityVersion(proto.getTs(), proto.getId(), proto.getName());
    }

    private VersionedEntityInfo getVersionedEntityInfo(TransportProtos.VersionedEntityInfoProto proto) {
        return new VersionedEntityInfo(EntityIdFactory.getByTypeAndUuid(proto.getEntityType(), new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB())));
    }

    @SuppressWarnings("rawtypes")
    @SneakyThrows
    private EntityExportData toData(String data) {
        return jsonMapper.readValue(data, EntityExportData.class);
    }

    private static <T> TbQueueCallback wrap(SettableFuture<T> future) {
        return new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
            }

            @Override
            public void onFailure(Throwable t) {
                future.setException(t);
            }
        };
    }

    private static <T> TbQueueCallback wrap(SettableFuture<T> future, T value) {
        return new TbQueueCallback() {
            @Override
            public void onSuccess(TbQueueMsgMetadata metadata) {
                future.set(value);
            }

            @Override
            public void onFailure(Throwable t) {
                future.setException(t);
            }
        };
    }

    private static String getRelativePath(EntityType entityType, EntityId entityId) {
        String path = entityType.name().toLowerCase();
        if (entityId != null) {
            path += "/" + entityId + ".json";
        }
        return path;
    }

    private static PrepareMsg getCommitPrepareMsg(VersionCreateRequest request) {
        return PrepareMsg.newBuilder().setCommitMsg(request.getVersionName()).setBranchName(request.getBranch()).build();
    }

    private ToVersionControlServiceMsg.Builder newRequestProto(PendingGitRequest<?> request, EntitiesVersionControlSettings settings) {
        var tenantId = request.getTenantId();
        var requestId = request.getRequestId();
        var builder = ToVersionControlServiceMsg.newBuilder()
                .setNodeId(serviceInfoProvider.getServiceId())
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setRequestIdMSB(requestId.getMostSignificantBits())
                .setRequestIdLSB(requestId.getLeastSignificantBits());
        EntitiesVersionControlSettings vcSettings = settings;
        if (vcSettings == null && request.requiresSettings()) {
            vcSettings = entitiesVersionControlService.getVersionControlSettings(tenantId);
        }
        if (vcSettings != null) {
            builder.setVcSettings(ByteString.copyFrom(encodingService.encode(vcSettings)));
        } else if (request.requiresSettings()) {
            throw new RuntimeException("No entity version control settings provisioned!");
        }
        return builder;
    }

    private CommitRequestMsg.Builder buildCommitRequest(CommitGitRequest commit) {
        return CommitRequestMsg.newBuilder().setTxId(commit.getTxId().toString());
    }
}

