// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.sync.vc;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.TbStopWatch;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ExportableEntity;
import org.thingsboard.server.common.data.HasOwnerId;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.sync.ie.EntityExportData;
import org.thingsboard.server.common.data.sync.ie.EntityExportSettings;
import org.thingsboard.server.common.data.sync.ie.EntityImportResult;
import org.thingsboard.server.common.data.sync.ie.EntityImportSettings;
import org.thingsboard.server.common.data.sync.vc.BranchInfo;
import org.thingsboard.server.common.data.sync.vc.EntityDataDiff;
import org.thingsboard.server.common.data.sync.vc.EntityDataInfo;
import org.thingsboard.server.common.data.sync.vc.EntityLoadError;
import org.thingsboard.server.common.data.sync.vc.EntityTypeLoadResult;
import org.thingsboard.server.common.data.sync.vc.EntityVersion;
import org.thingsboard.server.common.data.sync.vc.RepositorySettings;
import org.thingsboard.server.common.data.sync.vc.VersionCreationResult;
import org.thingsboard.server.common.data.sync.vc.VersionLoadResult;
import org.thingsboard.server.common.data.sync.vc.VersionedEntityInfo;
import org.thingsboard.server.common.data.sync.vc.request.create.AutoVersionCreateConfig;
import org.thingsboard.server.common.data.sync.vc.request.create.ComplexVersionCreateRequest;
import org.thingsboard.server.common.data.sync.vc.request.create.EntityTypeVersionCreateConfig;
import org.thingsboard.server.common.data.sync.vc.request.create.SingleEntityVersionCreateRequest;
import org.thingsboard.server.common.data.sync.vc.request.create.SyncStrategy;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;
import org.thingsboard.server.common.data.sync.vc.request.load.EntityTypeVersionLoadRequest;
import org.thingsboard.server.common.data.sync.vc.request.load.SingleEntityVersionLoadRequest;
import org.thingsboard.server.common.data.sync.vc.request.load.VersionLoadConfig;
import org.thingsboard.server.common.data.sync.vc.request.load.VersionLoadRequest;
import org.thingsboard.server.common.data.util.ThrowingRunnable;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.exception.DeviceCredentialsValidationException;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.owner.OwnerService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.entitiy.TbLogEntityActionService;
import org.thingsboard.server.service.executors.VersionControlExecutor;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.sync.ie.EntitiesExportImportService;
import org.thingsboard.server.service.sync.ie.exporting.ExportableEntitiesService;
import org.thingsboard.server.service.sync.ie.importing.MissingEntityException;
import org.thingsboard.server.service.sync.vc.autocommit.TbAutoCommitSettingsService;
import org.thingsboard.server.service.sync.vc.data.CommitGitRequest;
import org.thingsboard.server.service.sync.vc.data.ComplexEntitiesExportCtx;
import org.thingsboard.server.service.sync.vc.data.EntitiesExportCtx;
import org.thingsboard.server.service.sync.vc.data.EntitiesImportCtx;
import org.thingsboard.server.service.sync.vc.data.EntityTypeExportCtx;
import org.thingsboard.server.service.sync.vc.data.EntityTypeExportTask;
import org.thingsboard.server.service.sync.vc.data.ReimportTask;
import org.thingsboard.server.service.sync.vc.data.SimpleEntitiesExportCtx;
import org.thingsboard.server.service.sync.vc.repository.TbRepositorySettingsService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.util.concurrent.Futures.transform;
import static org.thingsboard.server.common.data.sync.vc.VcUtils.checkBranchName;

@Service
@TbCoreComponent
@RequiredArgsConstructor
@Slf4j
public class DefaultEntitiesVersionControlService implements EntitiesVersionControlService {

    private final TbRepositorySettingsService repositorySettingsService;
    private final TbAutoCommitSettingsService autoCommitSettingsService;
    private final GitVersionControlQueueService gitServiceQueue;
    private final EntitiesExportImportService exportImportService;
    private final ExportableEntitiesService exportableEntitiesService;
    private final TbLogEntityActionService logEntityActionService;
    private final TransactionTemplate transactionTemplate;
    private final CustomerService customerService;
    private final OwnerService ownersService;
    private final EntityGroupService groupService;
    private final TbTransactionalCache<UUID, VersionControlTaskCacheEntry> taskCache;
    private final VersionControlExecutor executor;

    private static final Set<EntityType> GROUP_ENTITIES = EnumSet.of(
            EntityType.CUSTOMER, EntityType.DEVICE, EntityType.ASSET, EntityType.DASHBOARD, EntityType.ENTITY_VIEW, EntityType.USER
    );

    @Override
    public ListenableFuture<UUID> saveEntitiesVersion(SecurityUser user, VersionCreateRequest request) {
        checkBranchName(request.getBranch());
        var pendingCommit = gitServiceQueue.prepareCommit(user, request);
        DonAsynchron.withCallback(pendingCommit, commit -> {
            cachePut(commit.getTxId(), new VersionCreationResult());
            try {
                EntitiesExportCtx<?> theCtx;
                switch (request.getType()) {
                    case SINGLE_ENTITY: {
                        var ctx = new SimpleEntitiesExportCtx(user, commit, (SingleEntityVersionCreateRequest) request);
                        handleSingleEntityRequest(ctx);
                        theCtx = ctx;
                        break;
                    }
                    case COMPLEX: {
                        var ctx = new ComplexEntitiesExportCtx(user, commit, (ComplexVersionCreateRequest) request);
                        handleComplexRequest(ctx);
                        theCtx = ctx;
                        break;
                    }
                    default:
                        throw new RuntimeException("Unsupported request type: " + request.getType());
                }
                var resultFuture = Futures.transformAsync(Futures.allAsList(theCtx.getFutures()), f -> gitServiceQueue.push(commit), executor);
                DonAsynchron.withCallback(resultFuture, result -> cachePut(commit.getTxId(), result), e -> processCommitError(user, request, commit, e), executor);
            } catch (Exception e) {
                processCommitError(user, request, commit, e);
            }
        }, t -> log.debug("[{}] Failed to prepare the commit: {}", user.getId(), request, t));

        return transform(pendingCommit, CommitGitRequest::getTxId, MoreExecutors.directExecutor());
    }

    @Override
    public VersionCreationResult getVersionCreateStatus(SecurityUser user, UUID requestId) throws ThingsboardException {
        return getStatus(user, requestId, VersionControlTaskCacheEntry::getExportResult);
    }

    @Override
    public VersionLoadResult getVersionLoadStatus(SecurityUser user, UUID requestId) throws ThingsboardException {
        return getStatus(user, requestId, VersionControlTaskCacheEntry::getImportResult);
    }

    private <T> T getStatus(SecurityUser user, UUID requestId, Function<VersionControlTaskCacheEntry, T> getter) throws ThingsboardException {
        var cacheEntry = taskCache.get(requestId);
        if (cacheEntry == null || cacheEntry.get() == null) {
            log.debug("[{}] No cache record: {}", requestId, cacheEntry);
            throw new ThingsboardException("Task execution timed-out", ThingsboardErrorCode.ITEM_NOT_FOUND);
        } else {
            var entry = cacheEntry.get();
            log.trace("[{}] Cache get: {}", requestId, entry);
            var result = getter.apply(entry);
            if (result == null) {
                throw new ThingsboardException("Invalid task", ThingsboardErrorCode.BAD_REQUEST_PARAMS);
            } else {
                return result;
            }
        }
    }

    private void handleSingleEntityRequest(SimpleEntitiesExportCtx ctx) throws Exception {
        EntityId entityId = ctx.getRequest().getEntityId();
        if (EntityType.ENTITY_GROUP.equals(entityId.getEntityType())) {
            exportGroup(ctx, new EntityGroupId(entityId.getId()));
        } else {
            EntityExportData<ExportableEntity<EntityId>> entityData = exportImportService.exportEntity(ctx, entityId);
            ExportableEntity<EntityId> entity = entityData.getEntity();
            if (entity instanceof HasOwnerId) {
                List<CustomerId> hierarchy = addCustomerHierarchyToCommit(ctx, entityId, (HasOwnerId) entity);
                ctx.add(gitServiceQueue.addToCommit(ctx.getCommit(), hierarchy, entityData));
            } else {
                ctx.add(gitServiceQueue.addToCommit(ctx.getCommit(), entityData));
            }
        }
    }

    private void handleComplexRequest(ComplexEntitiesExportCtx parentCtx) throws Exception {
        ComplexVersionCreateRequest request = parentCtx.getRequest();
        request.getEntityTypes().forEach((entityType, config) -> {
            EntityTypeExportCtx ctx = new EntityTypeExportCtx(parentCtx, config, request.getSyncStrategy(), entityType);
            if (ctx.isOverwrite()) {
                ctx.add(gitServiceQueue.deleteAll(ctx.getCommit(), entityType));
            }

            if (GROUP_ENTITIES.contains(entityType)) {
                if (config.isAllEntities()) {
                    ctx.addTask(new EntityTypeExportTask(Collections.emptyList(), ctx.getUser().getTenantId()));
                    while (true) {
                        EntityTypeExportTask task = ctx.pollTask();
                        if (task == null) {
                            break;
                        }
                        exportGroupEntities(ctx, task);
                    }
                } else {
                    for (UUID groupId : config.getEntityIds()) {
                        exportGroup(ctx, new EntityGroupId(groupId));
                    }
                }
            } else {
                if (config.isAllEntities()) {
                    //For Entity Types that belong to Tenant Level Only.
                    DaoUtil.processInBatches(pageLink -> exportableEntitiesService.findEntitiesIdsByTenantId(ctx.getTenantId(), entityType, pageLink)
                            , 100, entityId -> {
                                saveEntityData(ctx, entityId);
                            });
                } else {
                    for (UUID entityId : config.getEntityIds()) {
                        saveEntityData(ctx, EntityIdFactory.getByTypeAndUuid(entityType, entityId));
                    }
                }
            }
        });
    }

    @SneakyThrows
    private void exportGroup(EntitiesExportCtx<?> ctx, EntityGroupId entityId) {
        EntityExportData<ExportableEntity<EntityGroupId>> entityData = exportImportService.exportEntity(ctx, entityId);
        EntityGroup group = (EntityGroup) (entityData.getEntity());
        List<CustomerId> hierarchy = addCustomerHierarchyToCommit(ctx, entityId, group);
        ctx.add(gitServiceQueue.addToCommit(ctx.getCommit(), hierarchy, entityData));
        if (ctx.getSettings().isExportGroupEntities() && ctx.shouldExportEntities(group.getType())) {
            PageDataIterable<EntityId> entityIdsIterator = new PageDataIterable<>(
                    link -> groupService.findEntityIds(ctx.getTenantId(), group.getType(), entityId, link), 1024);
            List<EntityId> groupEntityIds = new ArrayList<>();
            for (EntityId groupEntityId : entityIdsIterator) {
                if (!group.isGroupAll()) {
                    groupEntityIds.add(Optional.ofNullable(exportableEntitiesService.getExternalIdByInternal(groupEntityId)).orElse(groupEntityId));
                }
                if (ctx.isExportRelatedEntities()) {
                    EntityExportData<ExportableEntity<EntityId>> groupEntityData = exportImportService.exportEntity(ctx, groupEntityId);
                    ctx.add(gitServiceQueue.addToCommit(ctx.getCommit(), hierarchy, groupEntityData));
                }
            }
            if (!group.isGroupAll()) {
                ctx.add(gitServiceQueue.addToCommit(ctx.getCommit(), hierarchy, group.getType(), entityData.getExternalId(), groupEntityIds));
            }
        }
    }

    private List<CustomerId> addCustomerHierarchyToCommit(EntitiesExportCtx<?> ctx, EntityId entityId, HasOwnerId entity) throws ThingsboardException {
        Map<CustomerId, CustomerId> customerIds = getOrderedCustomerIdsMap(ctx.getTenantId(), entityId, entity);
        List<CustomerId> hierarchy = new ArrayList<>(customerIds.size());
        for (var idPair : customerIds.entrySet()) {
            var internalId = idPair.getKey();
            var externalId = idPair.getValue();
            if (ctx.isExportRelatedCustomers()) {
                EntityExportData<ExportableEntity<EntityId>> ownerData = exportImportService.exportEntity(ctx, internalId);
                ctx.add(gitServiceQueue.addToCommit(ctx.getCommit(), new ArrayList<>(hierarchy), ownerData));
            }
            hierarchy.add(externalId);
        }
        return hierarchy;
    }

    @SneakyThrows
    private void exportGroupEntities(EntityTypeExportCtx ctx, EntityTypeExportTask task) {
        List<CustomerId> parents = new ArrayList<>(task.getParents());
        CustomerId customerId = EntityType.CUSTOMER.equals(task.getOwnerId().getEntityType()) ? new CustomerId(task.getOwnerId().getId()) : null;
        if (customerId != null) {
            CustomerId externalCustomerId = ctx.getExternalId(customerId);
            if (externalCustomerId == null) {
                Customer customer = customerService.findCustomerById(ctx.getTenantId(), customerId);
                externalCustomerId = customer.getExternalId() != null ? customer.getExternalId() : customerId;
                ctx.putExternalId(customerId, externalCustomerId);
            }
            parents.add(externalCustomerId);
        }
        if (ctx.shouldExportEntities(ctx.getEntityType())) {
            DaoUtil.processInBatches(pageLink -> exportableEntitiesService.findEntityIdsByTenantIdAndCustomerId(ctx.getTenantId(), customerId, ctx.getEntityType(), pageLink)
                    , 1024, entityId -> {
                        try {
                            EntityExportData<ExportableEntity<EntityId>> entityData = exportImportService.exportEntity(ctx, entityId);
                            ctx.getFutures().add(gitServiceQueue.addToCommit(ctx.getCommit(), parents, entityData));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
        DaoUtil.processInBatches(pageLink -> groupService.findEntityGroupsByType(ctx.getTenantId(), task.getOwnerId(), ctx.getEntityType(), pageLink)
                , 1024, group -> {
                    try {
                        EntityExportData<ExportableEntity<EntityGroupId>> entityData = exportImportService.exportEntity(ctx, group.getId());
                        ctx.getFutures().add(gitServiceQueue.addToCommit(ctx.getCommit(), parents, entityData));
                        if (!group.isGroupAll() && ctx.shouldExportEntities(ctx.getEntityType())) {
                            PageDataIterable<EntityId> entityIdsIterator = new PageDataIterable<>(
                                    link -> groupService.findEntityIds(ctx.getTenantId(), group.getType(), group.getId(), link), 1024);
                            List<EntityId> groupEntityIds = new ArrayList<>();
                            for (EntityId groupEntityId : entityIdsIterator) {
                                var entityExternalId = ctx.getExternalId(groupEntityId);
                                if (entityExternalId != null) {
                                    groupEntityIds.add(entityExternalId);
                                }
                            }
                            ctx.getFutures().add(gitServiceQueue.addToCommit(ctx.getCommit(), parents, group.getType(), entityData.getExternalId(), groupEntityIds));
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        DaoUtil.processInBatches(pageLink -> exportableEntitiesService.findEntityIdsByTenantIdAndCustomerId(ctx.getTenantId(), customerId, EntityType.CUSTOMER, pageLink)
                , 1024, cId -> ctx.addTask(new EntityTypeExportTask(parents, cId)));
    }

    @SneakyThrows
    private void saveEntityData(EntitiesExportCtx<?> ctx, EntityId entityId) {
        EntityExportData<ExportableEntity<EntityId>> entityData = exportImportService.exportEntity(ctx, entityId);
        ctx.add(gitServiceQueue.addToCommit(ctx.getCommit(), entityData));
    }

    @Override
    public ListenableFuture<PageData<EntityVersion>> listEntityVersions(TenantId tenantId, String branch, EntityId externalId, EntityId internalId, PageLink pageLink) throws Exception {
        if (internalId == null) {
            internalId = externalId;
        }
        List<CustomerId> customerExternalIds = getCustomerExternalIds(tenantId, internalId);
        if (EntityType.ENTITY_GROUP.equals(internalId.getEntityType())) {
            var entity = findExportableEntityInDb(tenantId, internalId);
            return gitServiceQueue.listVersions(tenantId, branch,
                    customerExternalIds, ((EntityGroup) entity).getType(), externalId, pageLink);
        } else {
            return gitServiceQueue.listVersions(tenantId, branch,
                    customerExternalIds, externalId, pageLink);
        }
    }

    @Override
    public ListenableFuture<PageData<EntityVersion>> listEntityTypeVersions(TenantId tenantId, String branch, EntityType entityType, PageLink pageLink) throws Exception {
        return gitServiceQueue.listVersions(tenantId, branch, entityType, pageLink);
    }

    @Override
    public ListenableFuture<PageData<EntityVersion>> listVersions(TenantId tenantId, String branch, PageLink pageLink) throws Exception {
        return gitServiceQueue.listVersions(tenantId, branch, pageLink);
    }

    @Override
    public ListenableFuture<List<VersionedEntityInfo>> listEntitiesAtVersion(TenantId tenantId, String versionId, EntityType entityType) throws Exception {
        return gitServiceQueue.listEntitiesAtVersion(tenantId, versionId, entityType);
    }

    @Override
    public ListenableFuture<List<VersionedEntityInfo>> listAllEntitiesAtVersion(TenantId tenantId, String versionId) throws Exception {
        return gitServiceQueue.listEntitiesAtVersion(tenantId, versionId);
    }

    @Override
    public UUID loadEntitiesVersion(SecurityUser user, VersionLoadRequest request) throws Exception {
        EntitiesImportCtx ctx = new EntitiesImportCtx(UUID.randomUUID(), user, request.getVersionId());
        cachePut(ctx.getRequestId(), VersionLoadResult.empty());
        switch (request.getType()) {
            case SINGLE_ENTITY: {
                SingleEntityVersionLoadRequest versionLoadRequest = (SingleEntityVersionLoadRequest) request;
                ctx.setRollbackOnError(true);
                executor.submit(() -> load(ctx, request, c -> loadSingleEntity(c, versionLoadRequest)));
                break;
            }
            case ENTITY_TYPE: {
                EntityTypeVersionLoadRequest versionLoadRequest = (EntityTypeVersionLoadRequest) request;
                ctx.setRollbackOnError(versionLoadRequest.isRollbackOnError());
                executor.submit(() -> load(ctx, request, c -> loadMultipleEntities(c, versionLoadRequest)));
                break;
            }
            default:
                throw new IllegalArgumentException("Unsupported version load request");
        }

        return ctx.getRequestId();
    }

    private <R> VersionLoadResult load(EntitiesImportCtx ctx, VersionLoadRequest request, Function<EntitiesImportCtx, VersionLoadResult> loadFunction) {
        try {
            VersionLoadResult result;
            if (ctx.isRollbackOnError()) {
                result = transactionTemplate.execute(status -> {
                    try {
                        return loadFunction.apply(ctx);
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new RuntimeException(e); // to prevent UndeclaredThrowableException
                    }
                });
                for (ThrowingRunnable eventCallback : ctx.getEventCallbacks()) {
                    eventCallback.run();
                }
            } else {
                result = loadFunction.apply(ctx);
            }
            result.setDone(true);
            return cachePut(ctx.getRequestId(), result);
        } catch (LoadEntityException e) {
            return cachePut(ctx.getRequestId(), onError(e.getExternalId(), e.getCause()));
        } catch (Throwable e) {
            log.info("[{}] Failed to process request [{}] due to: ", ctx.getTenantId(), request, e);
            return cachePut(ctx.getRequestId(), VersionLoadResult.error(EntityLoadError.runtimeError(e)));
        }
    }

    @SneakyThrows
    @SuppressWarnings("rawtypes")
    private VersionLoadResult loadSingleEntity(EntitiesImportCtx ctx, SingleEntityVersionLoadRequest request) {
        EntityId internalId = request.getInternalEntityId();
        List<CustomerId> ownerIds = internalId != null ? getCustomerExternalIds(ctx.getTenantId(), internalId) : Collections.emptyList();

        VersionLoadConfig config = request.getConfig();
        var settings = EntityImportSettings.builder()
                .updateRelations(config.isLoadRelations())
                .saveAttributes(config.isLoadAttributes())
                .saveCredentials(config.isLoadCredentials())
                .saveCalculatedFields(config.isLoadCalculatedFields())
                .saveUserGroupPermissions(config.isLoadPermissions())
                .autoGenerateIntegrationKey(config.isAutoGenerateIntegrationKey())
                .findExistingByName(false)
                .build();
        ctx.setFinalImportAttempt(true);
        ctx.setSettings(settings);

        if (EntityType.ENTITY_GROUP.equals(request.getExternalEntityId().getEntityType())) {
            var entity = findExportableEntityInDb(ctx.getTenantId(), internalId != null ? internalId : request.getExternalEntityId());
            EntityExportData groupData = gitServiceQueue.getEntityGroup(ctx.getTenantId(), request.getVersionId(), ownerIds, ((EntityGroup) entity).getType(), request.getExternalEntityId()).get();
            EntityImportResult<?> importResult = exportImportService.importEntity(ctx, groupData);
            EntityGroup savedGroup = (EntityGroup) importResult.getSavedEntity();
            if (config.isLoadGroupEntities() && ctx.shouldImportEntities(savedGroup.getType())) {
                if (savedGroup.isGroupAll()) {
                    importEntities(ctx, ownerIds, savedGroup.getType(), false);
                } else {
                    importGroupEntities(ctx, ownerIds, savedGroup.getType(), savedGroup.getId(), groupData.getExternalId());
                }
                reimport(ctx);
            }
            exportImportService.saveReferencesAndRelations(ctx);
            return VersionLoadResult.success(EntityTypeLoadResult.builder()
                    .entityType(importResult.getEntityType())
                    .created(importResult.getOldEntity() == null ? 1 : 0)
                    .updated(importResult.getOldEntity() != null ? 1 : 0)
                    .deleted(0)
                    .build());
        } else {
            EntityExportData entityData = gitServiceQueue.getEntity(ctx.getTenantId(), ctx.getVersionId(), ownerIds, request.getExternalEntityId()).get();
            try {
                EntityImportResult<?> importResult = exportImportService.importEntity(ctx, entityData);
                exportImportService.saveReferencesAndRelations(ctx);
                return VersionLoadResult.success(EntityTypeLoadResult.builder()
                        .entityType(importResult.getEntityType())
                        .created(importResult.getOldEntity() == null ? 1 : 0)
                        .updated(importResult.getOldEntity() != null ? 1 : 0)
                        .deleted(0)
                        .build());
            } catch (Exception e) {
                throw new LoadEntityException(entityData.getExternalId(), e);
            }
        }
    }

    private List<CustomerId> getCustomerExternalIds(TenantId tenantId, EntityId entityId) {
        return getCustomerExternalIds(tenantId, entityId, null);
    }

    private List<CustomerId> getCustomerExternalIds(TenantId tenantId, EntityId entityId, HasOwnerId entity) {
        var map = getOrderedCustomerIdsMap(tenantId, entityId, entity);
        if (map.isEmpty()) {
            return Collections.emptyList();
        } else {
            return new ArrayList<>(map.values());
        }
    }

    // Ordered Map<InternalId, ExternalId> from parent customer to sub-customer.
    private Map<CustomerId, CustomerId> getOrderedCustomerIdsMap(TenantId tenantId, EntityId entityId, HasOwnerId entity) {
        Set<EntityId> ownersSet;
        if (entity != null) {
            ownersSet = ownersService.getOwners(tenantId, entityId, entity);
        } else {
            ownersSet = ownersService.getOwners(tenantId, entityId);
        }
        List<EntityId> owners = new ArrayList<>(ownersSet);
        if (owners.size() == 1) {
            return Collections.emptyMap();
        } else {
            Collections.reverse(owners);
            LinkedHashMap<CustomerId, CustomerId> result = new LinkedHashMap<>(Math.max(1, owners.size() - 1));
            for (EntityId ownerId : owners) {
                if (EntityType.TENANT.equals(ownerId.getEntityType())) {
                    continue;
                }
                CustomerId internalId = new CustomerId(ownerId.getId());
                Customer customer = customerService.findCustomerById(tenantId, internalId);
                if (customer == null) {
                    throw new RuntimeException("Failed to fetch customer with id: " + internalId);
                }
                result.put(internalId, customer.getExternalId() != null ? customer.getExternalId() : internalId);
            }
            return result;
        }
    }

    @SneakyThrows
    private VersionLoadResult loadMultipleEntities(EntitiesImportCtx ctx, EntityTypeVersionLoadRequest request) {
        var sw = TbStopWatch.create("before");
        List<EntityType> entityTypes = request.getEntityTypes().keySet().stream()
                .sorted(exportImportService.getEntityTypeComparatorForImport()).toList();
        for (EntityType entityType : entityTypes) {
            log.debug("[{}] LOADING {} entities", ctx.getTenantId(), entityType);
            sw.startNew("Entities " + entityType.name());
            ctx.setSettings(getEntityImportSettings(request, entityType));
            importEntities(ctx, Collections.emptyList(), entityType, true);
        }

        for (EntityType groupType : GROUP_ENTITIES) {
            if (!entityTypes.contains(groupType)) {
                continue;
            }
            log.debug("[{}] Loading {} groups", ctx.getTenantId(), groupType);
            sw.startNew("Groups " + groupType.name());
            ctx.setSettings(getEntityImportSettings(request, groupType));
            importEntityGroups(ctx, groupType);
            persistToCache(ctx);
        }

        sw.startNew("Reimport");
        reimport(ctx);
        persistToCache(ctx);

        sw.startNew("Remove Others");
        request.getEntityTypes().keySet().stream()
                .filter(entityType -> request.getEntityTypes().get(entityType).isRemoveOtherEntities())
                .sorted(exportImportService.getEntityTypeComparatorForImport().reversed())
                .forEach(entityType -> removeOtherEntities(ctx, entityType));

        sw.startNew("References and Relations");
        exportImportService.saveReferencesAndRelations(ctx);
        sw.stop();
        for (var task : sw.getTaskInfo()) {
            log.debug("[{}] Executed: {} in {}ms", ctx.getTenantId(), task.getTaskName(), task.getTimeMillis());
        }
        log.debug("[{}] Total import time: {}ms", ctx.getTenantId(), sw.getTotalTimeMillis());
        return VersionLoadResult.success(new ArrayList<>(ctx.getResults().values()));
    }

    private EntityImportSettings getEntityImportSettings(EntityTypeVersionLoadRequest request, EntityType entityType) {
        var config = request.getEntityTypes().get(entityType);
        return EntityImportSettings.builder()
                .updateRelations(config.isLoadRelations())
                .saveAttributes(config.isLoadAttributes())
                .saveCredentials(config.isLoadCredentials())
                .saveCalculatedFields(config.isLoadCalculatedFields())
                .saveUserGroupPermissions(config.isLoadPermissions())
                .findExistingByName(config.isFindExistingEntityByName())
                .autoGenerateIntegrationKey(config.isAutoGenerateIntegrationKey())
                .build();
    }

    @SneakyThrows
    private void importGroupEntities(EntitiesImportCtx ctx, List<CustomerId> ownerIds, EntityType entityType, EntityGroupId internalGroupId,
                                     EntityId externalGroupId) {

        List<EntityId> allGroupEntityIds = gitServiceQueue.getGroupEntityIds(ctx.getTenantId(), ctx.getVersionId(), ownerIds, entityType, externalGroupId).get();

        for (List<UUID> entityIds : Lists.partition(allGroupEntityIds.stream().map(EntityId::getId).collect(Collectors.toList()), 100)) {
            List<EntityExportData> entityDataList;
            try {
                entityDataList = gitServiceQueue.getEntities(ctx.getTenantId(), ctx.getVersionId(), ownerIds, entityType, entityIds).get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
            var result = importEntityDataList(ctx, entityType, entityDataList);
            var internalIds = result.stream().map(EntityImportResult::getSavedEntity).map(HasId::getId).collect(Collectors.toCollection(ArrayList<EntityId>::new));
            groupService.addEntitiesToEntityGroup(ctx.getTenantId(), internalGroupId, internalIds);
        }
    }

    @SuppressWarnings("rawtypes")
    @SneakyThrows
    private void importEntities(EntitiesImportCtx ctx, List<CustomerId> ownerIds, EntityType entityType, boolean recursive) {
        int limit = 100;
        int offset = 0;
        List<EntityExportData> entityDataList;
        do {
            long ts = System.currentTimeMillis();
            try {
                entityDataList = gitServiceQueue.getEntities(ctx.getTenantId(), ctx.getVersionId(), ownerIds, entityType, false, recursive, offset, limit).get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
            long getEntities = System.currentTimeMillis() - ts;
            importEntityDataList(ctx, entityType, entityDataList);
            long importEntities = System.currentTimeMillis() - ts;
            log.info("[{}][{}] Import: get -> {}, import -> {}", entityType, entityDataList.size(), getEntities, importEntities);
            offset += limit;
        } while (entityDataList.size() == limit);
    }

    @SuppressWarnings("rawtypes")
    @SneakyThrows
    private void importEntityGroups(EntitiesImportCtx ctx, EntityType entityType) {
        int limit = 100;
        int offset = 0;
        List<EntityExportData> entityDataList;
        Map<EntityId, List<CustomerId>> ownersCache = new HashMap<>();
        do {
            try {
                entityDataList = gitServiceQueue.getEntities(ctx.getTenantId(), ctx.getVersionId(), Collections.emptyList(), entityType, true, true, offset, limit).get();
            } catch (ExecutionException e) {
                throw e.getCause();
            }
            List<EntityImportResult<?>> entityGroupResults = importEntityDataList(ctx, entityType, entityDataList);
            for (EntityImportResult<?> entityImportResult : entityGroupResults) {
                EntityGroup savedGroup = (EntityGroup) entityImportResult.getSavedEntity();
                if (!savedGroup.isGroupAll() && ctx.shouldImportEntities(savedGroup.getType())) {
                    var ownerIds = ownersCache.get(savedGroup.getOwnerId());
                    if (ownerIds == null) {
                        ownerIds = getCustomerExternalIds(ctx.getTenantId(), savedGroup.getId(), savedGroup);
                        ownersCache.put(savedGroup.getOwnerId(), ownerIds);
                    }
                    List<EntityId> allGroupEntityIds = gitServiceQueue.getGroupEntityIds(ctx.getTenantId(), ctx.getVersionId(), ownerIds, entityType, savedGroup.getExternalId()).get();
                    createGroupRelations(ctx, savedGroup.getId(), allGroupEntityIds);
                }
            }
            offset += limit;
        } while (entityDataList.size() == limit);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<EntityImportResult<?>> importEntityDataList(EntitiesImportCtx ctx, EntityType entityType,
                                                             List<EntityExportData> entityDataList) {
        log.debug("[{}] Loading {} entities pack ({})", ctx.getTenantId(), entityType, entityDataList.size());
        List<EntityImportResult<?>> importResults = new ArrayList<>();
        for (EntityExportData entityData : entityDataList) {
            EntityExportData reimportBackup = JacksonUtil.clone(entityData);
            log.debug("[{}] Loading {} entities", ctx.getTenantId(), entityType);
            EntityImportResult<?> importResult;
            try {
                importResult = exportImportService.importEntity(ctx, entityData);
                importResults.add(importResult);
            } catch (Exception e) {
                throw new LoadEntityException(entityData.getExternalId(), e);
            }
            registerResult(ctx, entityType, importResult, entityData);

            if (!importResult.isUpdatedAllExternalIds()) {
                ctx.getToReimport().put(entityData.getEntity().getExternalId(), new ReimportTask(reimportBackup, ctx.getSettings()));
                continue;
            }

            EntityId savedEntityId = importResult.getSavedEntity().getId();
            ctx.getImportedEntities().computeIfAbsent(entityType, t -> new HashSet<>()).add(savedEntityId);
        }

        persistToCache(ctx);
        log.debug("Imported {} pack ({}) for tenant {}", entityType, entityDataList.size(), ctx.getTenantId());
        return importResults;
    }

    private void createGroupRelations(EntitiesImportCtx ctx, EntityGroupId groupId, List<EntityId> externalIds) {
        List<EntityId> internalIds = new ArrayList<>(externalIds.size());
        for (EntityId externalId : externalIds) {
            var internalId = ctx.getInternalId(externalId);
            if (internalId == null) {
                internalId = Optional.<HasId<EntityId>>ofNullable(exportableEntitiesService.findEntityByTenantIdAndExternalId(ctx.getTenantId(), externalId))
                        .or(() -> Optional.ofNullable(exportableEntitiesService.findEntityByTenantIdAndId(ctx.getTenantId(), externalId)))
                        .map(HasId::getId)
                        .orElseThrow(() -> new LoadEntityException(groupId, new MissingEntityException(externalId)));
            }
            ctx.putInternalId(externalId, internalId);
            internalIds.add(internalId);
        }
        groupService.addEntitiesToEntityGroup(ctx.getTenantId(), groupId, internalIds);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void reimport(EntitiesImportCtx ctx) {
        ctx.setFinalImportAttempt(true);
        ctx.getToReimport().forEach((externalId, task) -> {
            try {
                EntityExportData entityData = task.getData();
                var settings = task.getSettings();
                ctx.setSettings(settings);
                EntityImportResult<?> importResult = exportImportService.importEntity(ctx, entityData);

                EntityId savedEntityId = importResult.getSavedEntity().getId();
                ctx.getImportedEntities().computeIfAbsent(externalId.getEntityType(), t -> new HashSet<>()).add(savedEntityId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void removeOtherEntities(EntitiesImportCtx ctx, EntityType entityType) {
        var entities = new PageDataIterable<>(link -> exportableEntitiesService.findEntitiesIdsByTenantId(ctx.getTenantId(), entityType, link), 100);
        Set<EntityId> toRemove = new LinkedHashSet<>();
        for (EntityId entityId : entities) {
            if (ctx.getImportedEntities().get(entityType) == null || !ctx.getImportedEntities().get(entityType).contains(entityId)) {
                toRemove.add(entityId);
            }
        }

        var entityGroups = new PageDataIterable<>(link -> groupService.findEntityGroupsByType(ctx.getTenantId(), entityType, link), 100);
        for (EntityGroup entityGroup : entityGroups) {
            //Skip reserved groups (All) even if they are not part of the restored commit.
            if (entityGroup.isGroupAll()) {
                continue;
            }
            if (ctx.getImportedEntities().get(entityType) == null || !ctx.getImportedEntities().get(entityType).contains(entityGroup.getId())) {
                toRemove.add(entityGroup.getId());
            }
        }


        for (EntityId entityId : toRemove) {
            ExportableEntity<EntityId> entity = exportableEntitiesService.findEntityById(entityId);
            exportableEntitiesService.removeById(ctx.getTenantId(), entityId);

            ThrowingRunnable callback = () -> {
                logEntityActionService.logEntityAction(ctx.getTenantId(), entity.getId(), entity, null,
                        ActionType.DELETED, ctx.getUser());
            };
            if (ctx.isRollbackOnError()) {
                ctx.addEventCallback(callback);
            } else {
                try {
                    callback.run();
                } catch (ThingsboardException e) {
                    throw new RuntimeException(e);
                }
            }
            ctx.registerDeleted(entityType, entityId.getEntityType() == EntityType.ENTITY_GROUP);
        }
        persistToCache(ctx);
    }

    private VersionLoadResult onError(EntityId externalId, Throwable e) {
        return analyze(e, externalId).orElse(VersionLoadResult.error(EntityLoadError.runtimeError(e, externalId)));
    }

    private Optional<VersionLoadResult> analyze(Throwable e, EntityId externalId) {
        if (e == null) {
            return Optional.empty();
        } else {
            if (e instanceof DeviceCredentialsValidationException) {
                return Optional.of(VersionLoadResult.error(EntityLoadError.credentialsError(externalId)));
            } else if (e instanceof MissingEntityException) {
                return Optional.of(VersionLoadResult.error(EntityLoadError.referenceEntityError(externalId, ((MissingEntityException) e).getEntityId())));
            } else if (e instanceof DataValidationException) {
                var dve = (DataValidationException) e;
                if (dve.getMessage().equals("Integration with such routing key already exists!")) {
                    return Optional.of(VersionLoadResult.error(EntityLoadError.routingKeyError(externalId)));
                }
            }
            return analyze(e.getCause(), externalId);
        }
    }

    @SuppressWarnings({"rawtypes"})
    @Override
    public ListenableFuture<EntityDataDiff> compareEntityDataToVersion(SecurityUser user, EntityId entityId, String versionId) {
        HasId<? extends EntityId> entity = findExportableEntityInDb(user.getTenantId(), entityId);

        EntityId externalId = ((ExportableEntity<? extends EntityId>) entity).getExternalId();
        if (externalId == null) externalId = entityId;

        var customerIds = getCustomerExternalIds(user.getTenantId(), entityId);
        ListenableFuture<EntityExportData> future;
        if (EntityType.ENTITY_GROUP.equals(entity.getId().getEntityType())) {
            future = gitServiceQueue.getEntityGroup(user.getTenantId(), versionId, customerIds, ((EntityGroup) entity).getType(), externalId);
        } else {
            future = gitServiceQueue.getEntity(user.getTenantId(), versionId, customerIds, externalId);
        }

        return transform(future,
                otherVersion -> {
                    SimpleEntitiesExportCtx ctx = new SimpleEntitiesExportCtx(user, null, null, EntityExportSettings.builder()
                            .exportRelations(otherVersion.hasRelations())
                            .exportAttributes(otherVersion.hasAttributes())
                            .exportCredentials(otherVersion.hasCredentials())
                            .exportCalculatedFields(otherVersion.hasCalculatedFields())
                            .exportPermissions(otherVersion.hasPermissions())
                            .exportGroupEntities(otherVersion.hasGroupEntities())
                            .build());
                    EntityExportData<?> currentVersion;
                    try {
                        currentVersion = exportImportService.exportEntity(ctx, entityId);
                    } catch (ThingsboardException e) {
                        throw new RuntimeException(e);
                    }
                    return new EntityDataDiff(currentVersion.sort(), otherVersion.sort());
                }, MoreExecutors.directExecutor());
    }

    @SuppressWarnings({"rawtypes"})
    @Override
    public ListenableFuture<EntityDataInfo> getEntityDataInfo(User user, EntityId externalId, EntityId internalId, String versionId) {
        List<CustomerId> customerIds = internalId != null ? getCustomerExternalIds(user.getTenantId(), internalId) : Collections.emptyList();
        ListenableFuture<EntityExportData> future;
        if (EntityType.ENTITY_GROUP.equals(externalId.getEntityType())) {
            HasId<? extends EntityId> entity = findExportableEntityInDb(user.getTenantId(), internalId != null ? internalId : externalId);
            future = gitServiceQueue.getEntityGroup(user.getTenantId(), versionId, customerIds, ((EntityGroup) entity).getType(), externalId);
        } else {
            future = gitServiceQueue.getEntity(user.getTenantId(), versionId, customerIds, externalId);
        }
        return Futures.transform(future,
                entity -> new EntityDataInfo(entity.hasRelations(), entity.hasAttributes(), entity.hasCredentials(), entity.hasCalculatedFields(), entity.hasPermissions(), entity.hasGroupEntities()), MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<List<BranchInfo>> listBranches(TenantId tenantId) {
        return gitServiceQueue.listBranches(tenantId);
    }

    @Override
    public RepositorySettings getVersionControlSettings(TenantId tenantId) {
        return repositorySettingsService.get(tenantId);
    }

    @Override
    public ListenableFuture<RepositorySettings> saveVersionControlSettings(TenantId tenantId, RepositorySettings versionControlSettings) {
        checkBranchName(versionControlSettings.getDefaultBranch());
        var restoredSettings = this.repositorySettingsService.restore(tenantId, versionControlSettings);
        try {
            var future = gitServiceQueue.initRepository(tenantId, restoredSettings);
            return Futures.transform(future, f -> repositorySettingsService.save(tenantId, restoredSettings), MoreExecutors.directExecutor());
        } catch (Exception e) {
            log.debug("{} Failed to init repository: {}", tenantId, versionControlSettings, e);
            throw new RuntimeException("Failed to init repository!", e);
        }
    }

    @Override
    public ListenableFuture<Void> deleteVersionControlSettings(TenantId tenantId) {
        log.debug("[{}] Deleting version control settings", tenantId);
        repositorySettingsService.delete(tenantId);
        return gitServiceQueue.clearRepository(tenantId);
    }

    @Override
    public ListenableFuture<Void> checkVersionControlAccess(TenantId tenantId, RepositorySettings settings) throws ThingsboardException {
        checkBranchName(settings.getDefaultBranch());
        settings = this.repositorySettingsService.restore(tenantId, settings);
        try {
            return gitServiceQueue.testRepository(tenantId, settings);
        } catch (Exception e) {
            throw new ThingsboardException(String.format("Unable to access repository: %s", getCauseMessage(e)),
                    ThingsboardErrorCode.GENERAL);
        }
    }

    @Override
    public ListenableFuture<UUID> autoCommit(User user, EntityId entityId) {
        SecurityUser securityUser = asSecurityUser(user);
        var repositorySettings = repositorySettingsService.get(user.getTenantId());
        if (repositorySettings == null || repositorySettings.isReadOnly()) {
            return Futures.immediateFuture(null);
        }
        var autoCommitSettings = autoCommitSettingsService.get(user.getTenantId());
        if (autoCommitSettings == null) {
            return Futures.immediateFuture(null);
        }
        var entityType = entityId.getEntityType();
        AutoVersionCreateConfig autoCommitConfig = autoCommitSettings.get(entityType);
        if (autoCommitConfig == null) {
            return Futures.immediateFuture(null);
        }
        SingleEntityVersionCreateRequest vcr = new SingleEntityVersionCreateRequest();
        var autoCommitBranchName = autoCommitConfig.getBranch();
        if (StringUtils.isEmpty(autoCommitBranchName)) {
            autoCommitBranchName = StringUtils.isNotEmpty(repositorySettings.getDefaultBranch()) ? repositorySettings.getDefaultBranch() : "auto-commits";
        }
        vcr.setBranch(autoCommitBranchName);
        vcr.setVersionName("auto-commit at " + Instant.ofEpochSecond(System.currentTimeMillis() / 1000));
        vcr.setEntityId(entityId);
        vcr.setConfig(autoCommitConfig);
        return saveEntitiesVersion(securityUser, vcr);
    }

    @Override
    public ListenableFuture<UUID> autoCommit(User user, EntityType entityType, EntityGroupId groupId) throws Exception {
        SecurityUser securityUser = asSecurityUser(user);
        var repositorySettings = repositorySettingsService.get(user.getTenantId());
        if (repositorySettings == null || repositorySettings.isReadOnly()) {
            return Futures.immediateFuture(null);
        }
        var autoCommitSettings = autoCommitSettingsService.get(user.getTenantId());
        if (autoCommitSettings == null) {
            return Futures.immediateFuture(null);
        }
        AutoVersionCreateConfig autoCommitConfig = autoCommitSettings.get(entityType);
        if (autoCommitConfig == null) {
            return Futures.immediateFuture(null);
        }
        AutoVersionCreateConfig groupConfig = autoCommitConfig.copy();
        groupConfig.setSaveGroupEntities(false);
        SingleEntityVersionCreateRequest vcr = new SingleEntityVersionCreateRequest();
        var autoCommitBranchName = groupConfig.getBranch();
        if (StringUtils.isEmpty(autoCommitBranchName)) {
            autoCommitBranchName = StringUtils.isNotEmpty(repositorySettings.getDefaultBranch()) ? repositorySettings.getDefaultBranch() : "auto-commits";
        }
        vcr.setBranch(autoCommitBranchName);
        vcr.setVersionName("auto-commit at " + Instant.ofEpochSecond(System.currentTimeMillis() / 1000));
        vcr.setEntityId(groupId);
        vcr.setConfig(groupConfig);
        return saveEntitiesVersion(securityUser, vcr);
    }

    @Override
    public ListenableFuture<UUID> autoCommit(User user, EntityType entityType, List<UUID> entityIds) {
        SecurityUser securityUser = asSecurityUser(user);
        var repositorySettings = repositorySettingsService.get(user.getTenantId());
        if (repositorySettings == null || repositorySettings.isReadOnly()) {
            return Futures.immediateFuture(null);
        }
        var autoCommitSettings = autoCommitSettingsService.get(user.getTenantId());
        if (autoCommitSettings == null) {
            return Futures.immediateFuture(null);
        }
        AutoVersionCreateConfig autoCommitConfig = autoCommitSettings.get(entityType);
        if (autoCommitConfig == null) {
            return Futures.immediateFuture(null);
        }
        var autoCommitBranchName = autoCommitConfig.getBranch();
        if (StringUtils.isEmpty(autoCommitBranchName)) {
            autoCommitBranchName = StringUtils.isNotEmpty(repositorySettings.getDefaultBranch()) ? repositorySettings.getDefaultBranch() : "auto-commits";
        }
        ComplexVersionCreateRequest vcr = new ComplexVersionCreateRequest();
        vcr.setBranch(autoCommitBranchName);
        vcr.setVersionName("auto-commit at " + Instant.ofEpochSecond(System.currentTimeMillis() / 1000));
        vcr.setSyncStrategy(SyncStrategy.MERGE);

        EntityTypeVersionCreateConfig vcrConfig = new EntityTypeVersionCreateConfig();
        vcrConfig.setEntityIds(entityIds);
        vcr.setEntityTypes(Collections.singletonMap(entityType, vcrConfig));
        return saveEntitiesVersion(securityUser, vcr);
    }

    private static SecurityUser asSecurityUser(User user) {
        if (user instanceof SecurityUser securityUser) {
            return securityUser;
        }
        throw new IllegalStateException("VC operation requires a SecurityUser, got: "
                + (user == null ? "null" : user.getClass().getName()));
    }

    private String getCauseMessage(Exception e) {
        String message;
        if (e.getCause() != null && StringUtils.isNotEmpty(e.getCause().getMessage())) {
            message = e.getCause().getMessage();
        } else {
            message = e.getMessage();
        }
        return message;
    }

    private HasId<? extends EntityId> findExportableEntityInDb(TenantId tenantId, EntityId entityId) {
        HasId<? extends EntityId> entity = exportableEntitiesService.findEntityByTenantIdAndId(tenantId, entityId);
        if (entity == null) throw new IllegalArgumentException("Can't find the entity with id: " + entityId);
        if (!(entity instanceof ExportableEntity)) throw new IllegalArgumentException("Unsupported entity type");
        return entity;
    }

    private void registerResult(EntitiesImportCtx ctx, EntityType entityType, EntityImportResult<?> importResult, EntityExportData exportData) {
        boolean isGroup = exportData.getEntity() instanceof EntityGroup;
        if (isGroup) {
            entityType = ((EntityGroup) exportData.getEntity()).getType();
        }
        if (importResult.isCreated()) {
            ctx.registerResult(entityType, isGroup, true);
        } else if (importResult.isUpdated() || importResult.isUpdatedRelatedEntities()) {
            ctx.registerResult(entityType, isGroup, false);
        }
    }

    private void processCommitError(User user, VersionCreateRequest request, CommitGitRequest commit, Throwable e) {
        log.debug("[{}] Failed to prepare the commit: {}", user.getId(), request, e);
        cachePut(commit.getTxId(), new VersionCreationResult(e.getMessage()));
    }

    private void processLoadError(EntitiesImportCtx ctx, Throwable e) {
        log.debug("[{}] Failed to load the commit: {}", ctx.getRequestId(), ctx.getVersionId(), e);
        cachePut(ctx.getRequestId(), VersionLoadResult.error(EntityLoadError.runtimeError(e)));
    }

    private void cachePut(UUID requestId, VersionCreationResult result) {
        taskCache.put(requestId, VersionControlTaskCacheEntry.newForExport(result));
    }

    private VersionLoadResult cachePut(UUID requestId, VersionLoadResult result) {
        log.trace("[{}] Cache put: {}", requestId, result);
        taskCache.put(requestId, VersionControlTaskCacheEntry.newForImport(result));
        return result;
    }

    private void persistToCache(EntitiesImportCtx ctx) {
        cachePut(ctx.getRequestId(), VersionLoadResult.success(new ArrayList<>(ctx.getResults().values())));
    }

}
