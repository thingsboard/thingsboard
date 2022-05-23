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
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
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
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@TbCoreComponent
@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultGitVersionControlQueueService implements GitVersionControlQueueService {

    private final ObjectWriter jsonWriter = new ObjectMapper().writer(SerializationFeature.INDENT_OUTPUT);
    private final TbServiceInfoProvider serviceInfoProvider;
    private final TbClusterService clusterService;
    private final Map<UUID, PendingGitRequest<?>> pendingRequestMap = new HashMap<>();

    @Override
    public ListenableFuture<CommitGitRequest> prepareCommit(TenantId tenantId, VersionCreateRequest request) {
        SettableFuture<CommitGitRequest> future = SettableFuture.create();

        CommitGitRequest commit = new CommitGitRequest(tenantId, request);
        registerAndSend(commit, builder -> builder.setCommitRequest(
                CommitRequestMsg.newBuilder().setPrepareMsg(getCommitPrepareMsg(request)).build()
        ).build(), wrap(future, commit));

        return future;
    }

    @Override
    public ListenableFuture<Void> addToCommit(CommitGitRequest commit, EntityExportData<ExportableEntity<EntityId>> entityData) {
        SettableFuture<Void> future = SettableFuture.create();

        String path = getRelativePath(entityData.getEntityType(), entityData.getEntity().getId());
        String entityDataJson;
        try {
            entityDataJson = jsonWriter.writeValueAsString(entityData);
        } catch (IOException e) {
            //TODO: analyze and return meaningful exceptions that we can show to the client;
            throw new RuntimeException(e);
        }

        registerAndSend(commit, builder -> builder.setCommitRequest(
                CommitRequestMsg.newBuilder().setAddMsg(
                        TransportProtos.AddMsg.newBuilder()
                                .setRelativePath(path).setEntityDataJson(entityDataJson).build()
                ).build()
        ).build(), wrap(commit.getFuture(), null));
        return future;
    }

    @Override
    public ListenableFuture<Void> deleteAll(CommitGitRequest commit, EntityType entityType) {
        SettableFuture<Void> future = SettableFuture.create();

        String path = getRelativePath(entityType, null);

        registerAndSend(commit, builder -> builder.setCommitRequest(
                CommitRequestMsg.newBuilder().setDeleteMsg(
                        TransportProtos.DeleteMsg.newBuilder().setRelativePath(path).build()
                ).build()
        ).build(), wrap(commit.getFuture(), null));

        return future;
    }

    @Override
    public ListenableFuture<VersionCreationResult> push(CommitGitRequest commit) {
        registerAndSend(commit, builder -> builder.setCommitRequest(
                CommitRequestMsg.newBuilder().setPushMsg(
                        TransportProtos.PushMsg.newBuilder().build()
                ).build()
        ).build(), wrap(commit.getFuture()));

        return commit.getFuture();
    }

    @Override
    public ListenableFuture<List<EntityVersion>> listVersions(TenantId tenantId, String branch) {
        return listVersions(tenantId, ListVersionsRequestMsg.newBuilder()
                .setBranchName(branch).build());
    }

    @Override
    public ListenableFuture<List<EntityVersion>> listVersions(TenantId tenantId, String branch, EntityType entityType) {
        return listVersions(tenantId, ListVersionsRequestMsg.newBuilder()
                .setBranchName(branch).setEntityType(entityType.name())
                .build());
    }

    @Override
    public ListenableFuture<List<EntityVersion>> listVersions(TenantId tenantId, String branch, EntityId entityId) {
        return listVersions(tenantId, ListVersionsRequestMsg.newBuilder()
                .setBranchName(branch)
                .setEntityType(entityId.getEntityType().name())
                .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                .setEntityIdLSB(entityId.getId().getLeastSignificantBits())
                .build());
    }

    private ListenableFuture<List<EntityVersion>> listVersions(TenantId tenantId, ListVersionsRequestMsg requestMsg) {
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
    public ListenableFuture<EntityExportData> getEntity(TenantId tenantId, String versionId, EntityId entityId) {
        EntityContentGitRequest request = new EntityContentGitRequest(tenantId, versionId, entityId);
        registerAndSend(request, builder -> builder.setEntityContentRequest(EntityContentRequestMsg.newBuilder()
                        .setVersionId(versionId)
                        .setEntityType(entityId.getEntityType().name())
                        .setEntityIdMSB(entityId.getId().getMostSignificantBits())
                        .setEntityIdLSB(entityId.getId().getLeastSignificantBits())).build()
                , wrap(request.getFuture()));

        return request.getFuture();
//        try {
//            String entityDataJson = gitRepositoryService.getFileContentAtCommit(tenantId,
//                    getRelativePath(entityId.getEntityType(), entityId.getId().toString()), versionId);
//            return JacksonUtil.fromString(entityDataJson, EntityExportData.class);
//        } catch (Exception e) {
//            //TODO: analyze and return meaningful exceptions that we can show to the client;
//            throw new RuntimeException(e);
//        }
    }

    private <T> void registerAndSend(PendingGitRequest<T> request, Function<ToVersionControlServiceMsg.Builder, ToVersionControlServiceMsg> enrichFunction, TbQueueCallback callback) {
        if (!request.getFuture().isDone()) {
            pendingRequestMap.putIfAbsent(request.getRequestId(), request);
            clusterService.pushMsgToVersionControl(request.getTenantId(), enrichFunction.apply(newRequestProto(request)), callback);
        } else {
            throw new RuntimeException("Future is already done!");
        }
    }

    @Override
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
                , wrap(request.getFuture()));

        return request.getFuture();
    }

    @Override
    public ListenableFuture<Void> testRepository(TenantId tenantId, EntitiesVersionControlSettings settings) {
        VoidGitRequest request = new VoidGitRequest(tenantId);

        registerAndSend(request, builder -> builder.setTestRepositoryRequest(GenericRepositoryRequestMsg.newBuilder().build()).build()
                , wrap(request.getFuture()));

        return request.getFuture();
    }

    @Override
    public ListenableFuture<Void> clearRepository(TenantId tenantId) {
        VoidGitRequest request = new VoidGitRequest(tenantId);

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
            }
        }
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

    private ToVersionControlServiceMsg.Builder newRequestProto(PendingGitRequest<?> request) {
        var tenantId = request.getTenantId();
        var requestId = request.getRequestId();
        return ToVersionControlServiceMsg.newBuilder()
                .setNodeId(serviceInfoProvider.getServiceId())
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setRequestIdMSB(requestId.getMostSignificantBits())
                .setRequestIdLSB(requestId.getLeastSignificantBits());

    }
}

