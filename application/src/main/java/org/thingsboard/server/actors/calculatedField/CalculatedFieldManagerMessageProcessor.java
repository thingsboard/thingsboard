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
package org.thingsboard.server.actors.calculatedField;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.function.TriConsumer;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.actors.TbCalculatedFieldEntityActorId;
import org.thingsboard.server.actors.calculatedField.EntityInitCalculatedFieldMsg.StateAction;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.actors.shared.AbstractContextAwareMsgProcessor;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ProfileEntityIdInfo;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.HasRelationPathLevel;
import org.thingsboard.server.common.data.cf.configuration.aggregation.RelatedEntitiesAggregationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationPathQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationPathLevel;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.msg.CalculatedFieldStatePartitionRestoreMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldCacheInitMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldEntityLifecycleMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldPartitionChangeMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.queue.settings.TbQueueCalculatedFieldSettings;
import org.thingsboard.server.service.cf.CalculatedFieldProcessingService;
import org.thingsboard.server.service.cf.CalculatedFieldStateService;
import org.thingsboard.server.service.cf.OwnerService;
import org.thingsboard.server.service.cf.cache.TenantEntityProfileCache;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.thingsboard.server.utils.CalculatedFieldUtils.fromProto;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class CalculatedFieldManagerMessageProcessor extends AbstractContextAwareMsgProcessor {

    private final Map<CalculatedFieldId, CalculatedFieldCtx> calculatedFields = new HashMap<>();
    private final Map<EntityId, List<CalculatedFieldCtx>> entityIdCalculatedFields = new HashMap<>();
    private final Map<EntityId, List<CalculatedFieldLink>> entityIdCalculatedFieldLinks = new HashMap<>();
    private final Map<EntityId, Set<EntityId>> ownerEntities = new HashMap<>();
    private ScheduledFuture<?> cfsReevaluationTask;

    private final CalculatedFieldProcessingService cfExecService;
    private final CalculatedFieldStateService cfStateService;
    private final CalculatedFieldService cfDaoService;
    private final DeviceService deviceService;
    private final AssetService assetService;
    private final CustomerService customerService;
    private final RelationService relationService;
    private final TbAssetProfileCache assetProfileCache;
    private final TbDeviceProfileCache deviceProfileCache;
    private final TenantEntityProfileCache entityProfileCache;
    private final OwnerService ownerService;
    private final TbQueueCalculatedFieldSettings cfSettings;
    protected final TenantId tenantId;

    private long cfCheckInterval;

    protected TbActorCtx ctx;

    CalculatedFieldManagerMessageProcessor(ActorSystemContext systemContext, TenantId tenantId) {
        super(systemContext);
        this.cfExecService = systemContext.getCalculatedFieldProcessingService();
        this.cfStateService = systemContext.getCalculatedFieldStateService();
        this.cfDaoService = systemContext.getCalculatedFieldService();
        this.deviceService = systemContext.getDeviceService();
        this.assetService = systemContext.getAssetService();
        this.customerService = systemContext.getCustomerService();
        this.relationService = systemContext.getRelationService();
        this.assetProfileCache = systemContext.getAssetProfileCache();
        this.deviceProfileCache = systemContext.getDeviceProfileCache();
        this.entityProfileCache = new TenantEntityProfileCache();
        this.ownerService = systemContext.getOwnerService();
        this.cfSettings = systemContext.getCalculatedFieldSettings();
        this.tenantId = tenantId;
    }

    void init(TbActorCtx ctx) {
        this.ctx = ctx;
    }

    public void stop() {
        log.info("[{}] Stopping CF manager actor.", tenantId);
        calculatedFields.values().forEach(CalculatedFieldCtx::close);
        calculatedFields.clear();
        entityIdCalculatedFields.clear();
        entityIdCalculatedFieldLinks.clear();
        cancelReevaluationTask();
        ctx.stop(ctx.getSelf());
    }

    public void onCacheInitMsg(CalculatedFieldCacheInitMsg msg) {
        log.debug("[{}] Processing CF actor init message.", msg.getTenantId().getId());
        initEntitiesCache();
        initCalculatedFields();
        cfCheckInterval = systemContext.getApiLimitService().getLimit(tenantId, DefaultTenantProfileConfiguration::getCfReevaluationCheckInterval);
        scheduleCfsReevaluation();
        msg.getCallback().onSuccess();
    }

    public void onStateRestoreMsg(CalculatedFieldStateRestoreMsg msg) {
        var cfId = msg.getId().cfId();
        var ctx = calculatedFields.get(cfId);

        if (ctx != null) {
            msg.setCtx(ctx);
            log.debug("[{}] Pushing CF state restore msg to specific actor [{}]", tenantId, msg.getId().entityId());
            getOrCreateActor(msg.getId().entityId()).tellWithHighPriority(msg);
        } else if (msg.getState() != null) {
            log.debug("[{}] Received CF state restore msg for non-existing CF [{}]. Removing state", tenantId, cfId);
            cfStateService.deleteState(msg.getId(), msg.getCallback());
        } else {
            msg.getCallback().onSuccess();
        }
    }

    public void onStatePartitionRestoreMsg(CalculatedFieldStatePartitionRestoreMsg msg) {
        ctx.broadcastToChildren(msg, true);
    }

    private void scheduleCfsReevaluation() {
        cfsReevaluationTask = systemContext.getScheduler().scheduleWithFixedDelay(() -> {
            try {
                calculatedFields.values().forEach(cf -> {
                    if (cf.requiresScheduledReevaluation()) {
                        applyToTargetCfEntityActors(cf, TbCallback.EMPTY, (entityId, callback) -> {
                            log.debug("[{}][{}] Pushing scheduled CF reevaluate msg", entityId, cf.getCfId());
                            getOrCreateActor(entityId).tell(new CalculatedFieldReevaluateMsg(tenantId, cf));
                        });
                    }
                });
            } catch (Exception e) {
                log.warn("[{}] Failed to trigger CFs reevaluation", tenantId, e);
            }
        }, cfCheckInterval, cfCheckInterval, TimeUnit.SECONDS);
    }

    public void onEntityLifecycleMsg(CalculatedFieldEntityLifecycleMsg msg) throws CalculatedFieldException {
        var event = msg.getData().getEvent();
        if (ComponentLifecycleEvent.RELATION_UPDATED.equals(event) || ComponentLifecycleEvent.RELATION_DELETED.equals(event)) {
            log.debug("Processing relation [{}] event from entity: [{}]", event, msg.getData().getEntityId());
            onRelationChangedEvent(msg.getData(), msg.getCallback());
            return;
        }
        log.debug("Processing entity lifecycle event: [{}] for entity: [{}]", event, msg.getData().getEntityId());
        var entityType = msg.getData().getEntityId().getEntityType();
        switch (entityType) {
            case CALCULATED_FIELD -> {
                switch (event) {
                    case CREATED -> onCfCreated(msg.getData(), msg.getCallback());
                    case UPDATED -> onCfUpdated(msg.getData(), msg.getCallback());
                    case DELETED -> onCfDeleted(msg.getData(), msg.getCallback());
                    default -> msg.getCallback().onSuccess();
                }
            }
            case DEVICE, ASSET, CUSTOMER -> {
                switch (event) {
                    case CREATED -> onEntityCreated(msg.getData(), msg.getCallback());
                    case UPDATED -> onEntityUpdated(msg.getData(), msg.getCallback());
                    case DELETED -> onEntityDeleted(msg.getData(), msg.getCallback());
                    default -> msg.getCallback().onSuccess();
                }
            }
            case DEVICE_PROFILE, ASSET_PROFILE -> {
                switch (event) {
                    case DELETED -> onProfileDeleted(msg.getData(), msg.getCallback());
                    default -> msg.getCallback().onSuccess();
                }
            }
            case TENANT_PROFILE -> {
                switch (event) {
                    case UPDATED -> onTenantProfileUpdated(msg.getData(), msg.getCallback());
                    default -> msg.getCallback().onSuccess();
                }
            }
            default -> msg.getCallback().onSuccess();
        }
    }

    public void onEntityActionEventMsg(CalculatedFieldEntityActionEventMsg msg) {
        switch (msg.getAction()) {
            case ALARM_ACK, ALARM_CLEAR, ALARM_DELETE -> {
                Alarm alarm = JacksonUtil.treeToValue(msg.getEntity(), Alarm.class);
                CalculatedFieldAlarmActionMsg alarmActionMsg = CalculatedFieldAlarmActionMsg.builder()
                        .tenantId(tenantId)
                        .alarm(alarm)
                        .action(msg.getAction())
                        .callback(msg.getCallback())
                        .build();
                getOrCreateActor(alarm.getOriginator()).tellWithHighPriority(alarmActionMsg);
            }
            default -> msg.getCallback().onSuccess();
        }
    }

    private void onProfileDeleted(ComponentLifecycleMsg msg, TbCallback callback) {
        entityProfileCache.removeProfileId(msg.getEntityId());
        callback.onSuccess();
    }

    private void onTenantProfileUpdated(ComponentLifecycleMsg msg, TbCallback callback) {
        checkCfIntervalForUpdate();

        long maxRelatedEntitiesPerCfArgument = systemContext.getApiLimitService().getLimit(tenantId, DefaultTenantProfileConfiguration::getMaxRelatedEntitiesToReturnPerCfArgument);
        Set<CalculatedFieldCtx> cfsToReinit = new HashSet<>();
        Stream.concat(
                calculatedFields.values().stream(),
                entityIdCalculatedFields.values().stream().flatMap(Collection::stream)
        ).forEach(ctx -> {
            if (ctx.hasRelatedEntities() && ctx.getMaxRelatedEntitiesPerCfArgument() != maxRelatedEntitiesPerCfArgument) {
                cfsToReinit.add(ctx);
            }
            ctx.setTenantProfileProperties();
        });

        if (!cfsToReinit.isEmpty()) {
            MultipleTbCallback cfsReinitCallback = new MultipleTbCallback(cfsToReinit.size(), callback);
            cfsToReinit.forEach(ctx -> applyToTargetCfEntityActors(ctx, cfsReinitCallback, (id, cb) -> initCfForEntity(id, ctx, StateAction.REINIT, cb)));
        } else {
            callback.onSuccess();
        }
    }

    private void checkCfIntervalForUpdate() {
        long updatedCfCheckInterval = systemContext.getApiLimitService().getLimit(tenantId, DefaultTenantProfileConfiguration::getCfReevaluationCheckInterval);
        if (cfCheckInterval != updatedCfCheckInterval) {
            cfCheckInterval = updatedCfCheckInterval;
            cancelReevaluationTask();
            scheduleCfsReevaluation();
        }
    }

    private void onEntityCreated(ComponentLifecycleMsg msg, TbCallback callback) {
        EntityId entityId = msg.getEntityId();
        EntityId profileId = getProfileId(tenantId, entityId);
        if (profileId != null) {
            entityProfileCache.add(profileId, entityId);
        }
        updateEntityOwner(entityId);

        if (!isMyPartition(entityId, callback)) {
            return;
        }
        var entityIdFields = getCalculatedFieldsByEntityId(entityId);
        var profileIdFields = getCalculatedFieldsByEntityId(profileId);
        var fieldsCount = entityIdFields.size() + profileIdFields.size();
        if (fieldsCount > 0) {
            MultipleTbCallback multiCallback = new MultipleTbCallback(fieldsCount, callback);
            entityIdFields.forEach(ctx -> initCfForEntity(entityId, ctx, StateAction.INIT, multiCallback));
            profileIdFields.forEach(ctx -> initCfForEntity(entityId, ctx, StateAction.INIT, multiCallback));
        } else {
            callback.onSuccess();
        }
    }

    private void onEntityUpdated(ComponentLifecycleMsg msg, TbCallback callback) {
        if (msg.getOldProfileId() != null && !msg.getOldProfileId().equals(msg.getProfileId())) {
            entityProfileCache.update(msg.getOldProfileId(), msg.getProfileId(), msg.getEntityId());
            if (!isMyPartition(msg.getEntityId(), callback)) {
                return;
            }
            var oldProfileCfs = getCalculatedFieldsByEntityId(msg.getOldProfileId());
            var newProfileCfs = getCalculatedFieldsByEntityId(msg.getProfileId());
            var fieldsCount = oldProfileCfs.size() + newProfileCfs.size();
            if (fieldsCount > 0) {
                MultipleTbCallback multiCallback = new MultipleTbCallback(fieldsCount, callback);
                var entityId = msg.getEntityId();
                oldProfileCfs.forEach(ctx -> deleteCfForEntity(entityId, ctx.getCfId(), multiCallback));
                newProfileCfs.forEach(ctx -> initCfForEntity(entityId, ctx, StateAction.INIT, multiCallback));
            } else {
                callback.onSuccess();
            }
        } else if (msg.isOwnerChanged()) {
            onEntityOwnerChanged(msg, callback);
        } else {
            callback.onSuccess();
        }
    }

    private void onEntityDeleted(ComponentLifecycleMsg msg, TbCallback callback) {
        switch (msg.getEntityId().getEntityType()) {
            case DEVICE, ASSET -> entityProfileCache.removeEntityId(msg.getEntityId());
            case CUSTOMER -> ownerEntities.remove(msg.getEntityId());
        }
        ownerEntities.values().forEach(entities -> entities.remove(msg.getEntityId()));
        if (isMyPartition(msg.getEntityId(), callback)) {
            log.debug("Pushing entity lifecycle msg to specific actor [{}]", msg.getEntityId());
            getOrCreateActor(msg.getEntityId()).tell(new CalculatedFieldEntityDeleteMsg(tenantId, msg.getEntityId(), callback));
        }
    }

    private void onRelationChangedEvent(ComponentLifecycleMsg msg, TbCallback callback) {
        Function<EntityId, TriConsumer<EntityId, CalculatedFieldCtx, TbCallback>> relationAction = switch (msg.getEvent()) {
            case RELATION_UPDATED -> relatedId -> (entityId, ctx, cb) -> initRelatedEntity(entityId, relatedId, ctx, cb);
            case RELATION_DELETED -> relatedId -> (entityId, ctx, cb) -> deleteRelatedEntity(entityId, relatedId, ctx, cb);
            default -> null;
        };

        if (relationAction == null) {
            callback.onSuccess();
            return;
        }

        EntityRelation entityRelation = JacksonUtil.treeToValue(msg.getInfo(), EntityRelation.class);
        EntityId toId = entityRelation.getTo();
        EntityId fromId = entityRelation.getFrom();
        String relationType = entityRelation.getType();

        if (!(CalculatedField.isSupportedRefEntity(toId) || CalculatedField.isSupportedRefEntity(fromId))) {
            callback.onSuccess();
            return;
        }

        MultipleTbCallback callbackForToAndFrom = new MultipleTbCallback(2, callback);
        processRelationByDirection(EntitySearchDirection.TO, relationType, toId, callbackForToAndFrom, relationAction.apply(fromId));
        processRelationByDirection(EntitySearchDirection.FROM, relationType, fromId, callbackForToAndFrom, relationAction.apply(toId));
    }

    private void processRelationByDirection(EntitySearchDirection direction,
                                            String relationType,
                                            EntityId mainId,
                                            MultipleTbCallback parentCallback,
                                            TriConsumer<EntityId, CalculatedFieldCtx, TbCallback> relationAction) {
        List<CalculatedFieldCtx> cfsByEntityIdAndProfile = getCalculatedFieldsByEntityIdAndProfile(mainId);
        if (cfsByEntityIdAndProfile.isEmpty()) {
            parentCallback.onSuccess();
            return;
        }

        List<CalculatedFieldCtx> matchingCfs = cfsByEntityIdAndProfile.stream()
                .filter(cf -> {
                    if (cf.getCalculatedField().getConfiguration() instanceof HasRelationPathLevel config) {
                        RelationPathLevel relation = config.getRelation();
                        return direction.equals(relation.direction()) && relationType.equals(relation.relationType());
                    }
                    return false;
                })
                .toList();

        MultipleTbCallback directionCallback = new MultipleTbCallback(matchingCfs.size(), parentCallback);
        matchingCfs.forEach(ctx -> relationAction.accept(mainId, ctx, directionCallback));
    }

    private void onCfCreated(ComponentLifecycleMsg msg, TbCallback callback) throws CalculatedFieldException {
        var cfId = new CalculatedFieldId(msg.getEntityId().getId());
        if (calculatedFields.containsKey(cfId)) {
            log.debug("[{}] CF was already initialized [{}]", tenantId, cfId);
            callback.onSuccess();
        } else {
            var cf = cfDaoService.findById(msg.getTenantId(), cfId);
            if (cf == null) {
                log.debug("[{}] Failed to lookup CF by id [{}]", tenantId, cfId);
                callback.onSuccess();
            } else {
                var cfCtx = getCfCtx(cf);
                try {
                    cfCtx.init();
                } catch (Exception e) {
                    throw CalculatedFieldException.builder().ctx(cfCtx).eventEntity(cf.getEntityId()).cause(e).errorMessage("Failed to initialize CF context").build();
                }
                calculatedFields.put(cf.getId(), cfCtx);
                // We use copy on write lists to safely pass the reference to another actor for the iteration.
                // Alternative approach would be to use any list but avoid modifications to the list (change the complete map value instead)
                entityIdCalculatedFields.computeIfAbsent(cf.getEntityId(), id -> new CopyOnWriteArrayList<>()).add(cfCtx);
                addLinks(cf);
                applyToTargetCfEntityActors(cfCtx, callback, (id, cb) -> initCfForEntity(id, cfCtx, StateAction.INIT, cb));
            }
        }
    }

    private CalculatedFieldCtx getCfCtx(CalculatedField cf) {
        return new CalculatedFieldCtx(cf, systemContext);
    }

    private void onCfUpdated(ComponentLifecycleMsg msg, TbCallback callback) throws CalculatedFieldException {
        var cfId = new CalculatedFieldId(msg.getEntityId().getId());
        var oldCfCtx = calculatedFields.get(cfId);
        if (oldCfCtx == null) {
            onCfCreated(msg, callback);
        } else {
            var newCf = cfDaoService.findById(msg.getTenantId(), cfId);
            if (newCf == null) {
                log.debug("[{}] Failed to lookup CF by id [{}]", tenantId, cfId);
                callback.onSuccess();
            } else {
                var newCfCtx = getCfCtx(newCf);
                try {
                    newCfCtx.init();
                } catch (Exception e) {
                    throw CalculatedFieldException.builder().ctx(newCfCtx).eventEntity(newCfCtx.getEntityId()).cause(e).errorMessage("Failed to initialize CF context").build();
                } finally {
                    calculatedFields.put(newCf.getId(), newCfCtx);
                    List<CalculatedFieldCtx> oldCfList = entityIdCalculatedFields.get(newCf.getEntityId());
                    List<CalculatedFieldCtx> newCfList = new CopyOnWriteArrayList<>();
                    boolean found = false;
                    for (CalculatedFieldCtx oldCtx : oldCfList) {
                        if (oldCtx.getCfId().equals(newCf.getId())) {
                            newCfList.add(newCfCtx);
                            found = true;
                        } else {
                            newCfList.add(oldCtx);
                        }
                    }
                    if (!found) {
                        newCfList.add(newCfCtx);
                    }
                    // We use copy on write lists to safely pass the reference to another actor for the iteration.
                    // Alternative approach would be to use any list but avoid modifications to the list (change the complete map value instead)
                    entityIdCalculatedFields.put(newCf.getEntityId(), newCfList);
                    deleteLinks(oldCfCtx);
                    addLinks(newCf);
                }

                StateAction stateAction;
                if (newCfCtx.getCfType() != oldCfCtx.getCfType()) {
                    stateAction = StateAction.RECREATE; // completely recreate state, then calculate
                } else if (newCfCtx.hasStateChanges(oldCfCtx)) {
                    stateAction = StateAction.REINIT; // refetch arguments, call state.init, then calculate
                } else if (newCfCtx.hasContextOnlyChanges(oldCfCtx)) {
                    stateAction = StateAction.REPROCESS; // call state.setCtx, then calculate
                } else if (newCfCtx.hasRefreshContextOnlyChanges(oldCfCtx)) {
                    stateAction = StateAction.REFRESH_CTX;
                } else {
                    callback.onSuccess();
                    return;
                }

                applyToTargetCfEntityActors(newCfCtx, new TbCallback() {
                    @Override
                    public void onSuccess() {
                        oldCfCtx.close();
                        callback.onSuccess();
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        oldCfCtx.close();
                        callback.onFailure(t);
                    }
                }, (id, cb) -> initCfForEntity(id, newCfCtx, stateAction, cb));
            }
        }
    }

    private void onCfDeleted(ComponentLifecycleMsg msg, TbCallback callback) {
        var cfId = new CalculatedFieldId(msg.getEntityId().getId());
        var cfCtx = calculatedFields.remove(cfId);
        if (cfCtx == null) {
            log.debug("[{}] CF was already deleted [{}]", tenantId, cfId);
            callback.onSuccess();
            return;
        }
        entityIdCalculatedFields.get(cfCtx.getEntityId()).remove(cfCtx);
        deleteLinks(cfCtx);
        applyToTargetCfEntityActors(cfCtx, new TbCallback() {
            @Override
            public void onSuccess() {
                cfCtx.close();
                callback.onSuccess();
            }

            @Override
            public void onFailure(Throwable t) {
                cfCtx.close();
                callback.onFailure(t);
            }
        }, (id, cb) -> deleteCfForEntity(id, cfId, cb));
    }

    public void onTelemetryMsg(CalculatedFieldTelemetryMsg msg) {
        EntityId entityId = msg.getEntityId();
        log.debug("Received telemetry msg from entity [{}]", entityId);
        // 4 = 1 for CF processing + 1 for links processing + 1 for owner entity processing + 1 for aggregation processing
        MultipleTbCallback callback = new MultipleTbCallback(4, msg.getCallback());
        // process all cfs related to entity, or it's profile;
        var entityIdFields = getCalculatedFieldsByEntityId(entityId);
        var profileIdFields = getCalculatedFieldsByEntityId(getProfileId(tenantId, entityId));
        if (!entityIdFields.isEmpty() || !profileIdFields.isEmpty()) {
            log.debug("Pushing telemetry msg to specific actor [{}]", entityId);
            getOrCreateActor(entityId).tell(new EntityCalculatedFieldTelemetryMsg(msg, entityIdFields, profileIdFields, callback));
        } else {
            callback.onSuccess();
        }
        // process all links (if any);
        List<CalculatedFieldEntityCtxId> linkedCalculatedFields = filterCalculatedFieldLinks(msg);
        var linksSize = linkedCalculatedFields.size();
        if (linksSize > 0) {
            cfExecService.pushMsgToLinks(msg, linkedCalculatedFields, callback);
        } else {
            callback.onSuccess();
        }
        // process all cfs related to owner entity
        if (entityId.getEntityType().isOneOf(EntityType.TENANT, EntityType.CUSTOMER)) {
            List<CalculatedFieldEntityCtxId> ownedEntitiesCFs = filterOwnedEntitiesCFs(msg);
            if (!ownedEntitiesCFs.isEmpty()) {
                cfExecService.pushMsgToLinks(msg, ownedEntitiesCFs, callback);
            } else {
                callback.onSuccess();
            }
        } else {
            callback.onSuccess();
        }
        // process all aggregation cfs (if any);
        List<CalculatedFieldEntityCtxId> aggregationCalculatedFields = filterAggregationCfs(msg);
        if (!aggregationCalculatedFields.isEmpty()) {
            cfExecService.pushMsgToLinks(msg, aggregationCalculatedFields, callback);
        } else {
            callback.onSuccess();
        }
    }

    private List<CalculatedFieldEntityCtxId> filterAggregationCfs(CalculatedFieldTelemetryMsg msg) {
        EntityId entityId = msg.getEntityId();
        return calculatedFields.values().stream()
                .filter(cf -> CalculatedFieldType.RELATED_ENTITIES_AGGREGATION.equals(cf.getCfType()))
                .filter(cf -> cf.relatedEntityMatches(msg.getProto()))
                .flatMap(cf -> findRelationsForCf(entityId, cf).stream())
                .toList();
    }

    private List<CalculatedFieldEntityCtxId> findRelationsForCf(EntityId entityId, CalculatedFieldCtx cf) {
        List<CalculatedFieldEntityCtxId> result = new ArrayList<>();
        if (cf.getCalculatedField().getConfiguration() instanceof RelatedEntitiesAggregationCalculatedFieldConfiguration configuration) {
            RelationPathLevel relation = configuration.getRelation();
            EntitySearchDirection inverseDirection = switch (relation.direction()) {
                case FROM -> EntitySearchDirection.TO;
                case TO -> EntitySearchDirection.FROM;
            };
            RelationPathLevel inverseRelation = new RelationPathLevel(inverseDirection, relation.relationType());
            List<EntityRelation> byRelationPathQuery = relationService.findByRelationPathQuery(tenantId, new EntityRelationPathQuery(entityId, List.of(inverseRelation)));
            EntityId cfEntityId = cf.getEntityId();
            Predicate<EntityId> matchesCfEntity = relatedEntity -> cfEntityId.equals(relatedEntity) || cfEntityId.equals(getProfileId(tenantId, relatedEntity));
            if (byRelationPathQuery != null && !byRelationPathQuery.isEmpty()) {
                switch (relation.direction()) {
                    case FROM -> byRelationPathQuery.stream()
                            .filter(entityRelation -> matchesCfEntity.test(entityRelation.getFrom()))
                            .forEach(entityRelation -> result.add(new CalculatedFieldEntityCtxId(tenantId, cf.getCfId(), entityRelation.getFrom())));
                    case TO -> byRelationPathQuery.stream()
                            .filter(entityRelation -> matchesCfEntity.test(entityRelation.getTo()))
                            .forEach(entityRelation -> result.add(new CalculatedFieldEntityCtxId(tenantId, cf.getCfId(), entityRelation.getTo())));
                }
            }
        }
        return result;
    }

    public void onLinkedTelemetryMsg(CalculatedFieldLinkedTelemetryMsg msg) {
        EntityId sourceEntityId = msg.getEntityId();
        log.debug("Received linked telemetry msg from entity [{}]", sourceEntityId);
        var proto = msg.getProto();
        var callback = msg.getCallback();
        var linksList = proto.getLinksList();
        if (linksList.isEmpty()) {
            log.debug("[{}] No CF links to process new telemetry.", msg.getTenantId());
            callback.onSuccess();
        }
        for (var linkProto : linksList) {
            var link = fromProto(linkProto);
            var cf = calculatedFields.get(link.cfId());
            withTargetEntities(link.entityId(), callback, (ids, cb) -> {
                var linkedTelemetryMsg = new EntityCalculatedFieldLinkedTelemetryMsg(tenantId, sourceEntityId, proto.getMsg(), cf, cb);
                ids.forEach(id -> linkedTelemetryMsgForEntity(id, linkedTelemetryMsg));
            });
        }
    }

    private void onEntityOwnerChanged(ComponentLifecycleMsg msg, TbCallback msgCallback) {
        EntityId entityId = msg.getEntityId();
        log.debug("Received changed owner msg from entity [{}]", entityId);
        updateEntityOwner(entityId);
        List<CalculatedFieldCtx> cfs = getCalculatedFieldsByEntityIdAndProfile(entityId);
        if (cfs.isEmpty()) {
            msgCallback.onSuccess();
            return;
        }
        MultipleTbCallback callback = new MultipleTbCallback(cfs.size(), msgCallback);
        cfs.forEach(cf -> {
            if (isMyPartition(entityId, callback)) {
                if (cf.hasCurrentOwnerSourceArguments()) {
                    CalculatedFieldArgumentResetMsg argResetMsg = new CalculatedFieldArgumentResetMsg(tenantId, cf, callback);
                    log.debug("Pushing CF argument reset msg to specific actor [{}]", entityId);
                    getOrCreateActor(entityId).tell(argResetMsg);
                } else {
                    callback.onSuccess();
                }
            }
        });
    }

    private List<CalculatedFieldEntityCtxId> filterCalculatedFieldLinks(CalculatedFieldTelemetryMsg msg) {
        EntityId entityId = msg.getEntityId();
        var proto = msg.getProto();
        List<CalculatedFieldEntityCtxId> result = new ArrayList<>();
        for (var link : getCalculatedFieldLinksByEntityId(entityId)) {
            CalculatedFieldCtx ctx = calculatedFields.get(link.calculatedFieldId());
            if (ctx.linkMatches(entityId, proto)) {
                result.add(ctx.toCalculatedFieldEntityCtxId());
            }
        }
        return result;
    }

    private List<CalculatedFieldEntityCtxId> filterOwnedEntitiesCFs(CalculatedFieldTelemetryMsg msg) {
        Set<EntityId> entities = getOwnedEntities(msg.getEntityId());
        var proto = msg.getProto();
        List<CalculatedFieldEntityCtxId> result = new ArrayList<>();
        for (var entityId : entities) {
            var ownerEntityCFs = getCalculatedFieldsByEntityId(entityId);
            for (var ctx : ownerEntityCFs) {
                if (ctx.dynamicSourceMatches(proto)) {
                    result.add(new CalculatedFieldEntityCtxId(tenantId, ctx.getCfId(), entityId));
                }
            }
            var ownerEntityProfileCFs = getCalculatedFieldsByEntityId(getProfileId(tenantId, entityId));
            for (var ctx : ownerEntityProfileCFs) {
                if (ctx.dynamicSourceMatches(proto)) {
                    result.add(new CalculatedFieldEntityCtxId(tenantId, ctx.getCfId(), entityId));
                }
            }
        }
        return result;
    }

    private List<CalculatedFieldCtx> getCalculatedFieldsByEntityId(EntityId entityId) {
        if (entityId == null) {
            return Collections.emptyList();
        }
        var result = entityIdCalculatedFields.get(entityId);
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;
    }

    private List<CalculatedFieldCtx> getCalculatedFieldsByEntityIdAndProfile(EntityId entityId) {
        List<CalculatedFieldCtx> cfsByEntityIdAndProfile = new ArrayList<>();
        cfsByEntityIdAndProfile.addAll(getCalculatedFieldsByEntityId(entityId));
        EntityId profileId = getProfileId(tenantId, entityId);
        if (profileId != null) {
            cfsByEntityIdAndProfile.addAll(getCalculatedFieldsByEntityId(profileId));
        }
        return cfsByEntityIdAndProfile;
    }

    private List<CalculatedFieldLink> getCalculatedFieldLinksByEntityId(EntityId entityId) {
        if (entityId == null) {
            return Collections.emptyList();
        }
        var result = entityIdCalculatedFieldLinks.get(entityId);
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;
    }

    private Set<EntityId> getOwnedEntities(EntityId entityId) {
        if (entityId == null) {
            return Collections.emptySet();
        }
        var result = ownerEntities.get(entityId);
        if (result == null) {
            result = Collections.emptySet();
        }
        return result;
    }

    private void linkedTelemetryMsgForEntity(EntityId entityId, EntityCalculatedFieldLinkedTelemetryMsg msg) {
        log.debug("Pushing linked telemetry msg to specific actor [{}]", entityId);
        getOrCreateActor(entityId).tell(msg);
    }

    private void deleteRelatedEntity(EntityId entityId, EntityId relatedEntityId, CalculatedFieldCtx cf, TbCallback callback) {
        log.debug("Pushing delete related entity msg to specific actor [{}]", relatedEntityId);
        getOrCreateActor(entityId).tell(new CalculatedFieldRelationActionMsg(tenantId, relatedEntityId, ActionType.DELETED, cf, callback));
    }

    private void initRelatedEntity(EntityId entityId, EntityId relatedEntityId, CalculatedFieldCtx cf, TbCallback callback) {
        log.debug("Pushing init related entity msg to specific actor [{}]", relatedEntityId);
        getOrCreateActor(entityId).tell(new CalculatedFieldRelationActionMsg(tenantId, relatedEntityId, ActionType.UPDATED, cf, callback));
    }

    private void deleteCfForEntity(EntityId entityId, CalculatedFieldId cfId, TbCallback callback) {
        log.debug("Pushing delete CF msg to specific actor [{}]", entityId);
        getOrCreateActor(entityId).tell(new CalculatedFieldEntityDeleteMsg(tenantId, cfId, callback));
    }

    private void initCfForEntity(EntityId entityId, CalculatedFieldCtx cfCtx, StateAction stateAction, TbCallback callback) {
        log.debug("Pushing entity init CF msg to specific actor [{}]", entityId);
        getOrCreateActor(entityId).tell(new EntityInitCalculatedFieldMsg(tenantId, cfCtx, stateAction, callback));
    }

    private boolean isMyPartition(EntityId entityId, TbCallback callback) {
        if (!systemContext.getPartitionService().resolve(ServiceType.TB_RULE_ENGINE, DataConstants.CF_QUEUE_NAME, tenantId, entityId).isMyPartition()) {
            log.debug("[{}] Entity belongs to external partition.", entityId);
            callback.onSuccess();
            return false;
        }
        return true;
    }

    private static boolean isProfileEntity(EntityType entityType) {
        return EntityType.DEVICE_PROFILE.equals(entityType) || EntityType.ASSET_PROFILE.equals(entityType);
    }

    private EntityId getProfileId(TenantId tenantId, EntityId entityId) {
        return switch (entityId.getEntityType()) {
            case ASSET -> Optional.ofNullable(assetProfileCache.get(tenantId, (AssetId) entityId)).map(AssetProfile::getId).orElse(null);
            case DEVICE -> Optional.ofNullable(deviceProfileCache.get(tenantId, (DeviceId) entityId)).map(DeviceProfile::getId).orElse(null);
            default -> null;
        };
    }

    private TbActorRef getOrCreateActor(EntityId entityId) {
        return ctx.getOrCreateChildActor(new TbCalculatedFieldEntityActorId(entityId),
                () -> DefaultActorService.CF_ENTITY_DISPATCHER_NAME,
                () -> new CalculatedFieldEntityActorCreator(systemContext, tenantId, entityId),
                () -> true);
    }

    private void addLinks(CalculatedField newCf) {
        var newLinks = newCf.getConfiguration().buildCalculatedFieldLinks(tenantId, newCf.getEntityId(), newCf.getId());
        newLinks.forEach(link -> entityIdCalculatedFieldLinks.computeIfAbsent(link.entityId(), id -> new CopyOnWriteArrayList<>()).add(link));
    }

    private void deleteLinks(CalculatedFieldCtx cfCtx) {
        var oldCf = cfCtx.getCalculatedField();
        var oldLinks = oldCf.getConfiguration().buildCalculatedFieldLinks(tenantId, oldCf.getEntityId(), oldCf.getId());
        oldLinks.forEach(link -> entityIdCalculatedFieldLinks.computeIfAbsent(link.entityId(), id -> new CopyOnWriteArrayList<>()).remove(link));
    }

    public void onPartitionChange(CalculatedFieldPartitionChangeMsg msg) {
        ctx.broadcastToChildren(msg, true);
    }

    public void initCalculatedFields() {
        PageDataIterable<CalculatedField> cfs = new PageDataIterable<>(pageLink -> cfDaoService.findCalculatedFieldsByTenantId(tenantId, pageLink), cfSettings.getInitTenantFetchPackSize());
        cfs.forEach(cf -> {
            log.trace("Processing calculated field record: {}", cf);
            try {
                initCalculatedField(cf);
                initCalculatedFieldLinks(cf);
            } catch (CalculatedFieldException e) {
                log.error("Failed to process calculated field record: {}", cf, e);
            }
        });
    }

    private void initCalculatedField(CalculatedField cf) throws CalculatedFieldException {
        var cfCtx = new CalculatedFieldCtx(cf, systemContext);
        try {
            cfCtx.init();
        } catch (Exception e) {
            throw CalculatedFieldException.builder().ctx(cfCtx).eventEntity(cf.getEntityId()).cause(e).errorMessage("Failed to initialize CF context").build();
        } finally {
            calculatedFields.put(cf.getId(), cfCtx);
            // We use copy on write lists to safely pass the reference to another actor for the iteration.
            // Alternative approach would be to use any list but avoid modifications to the list (change the complete map value instead)
            entityIdCalculatedFields.computeIfAbsent(cf.getEntityId(), id -> new CopyOnWriteArrayList<>()).add(cfCtx);
        }
    }

    private void initCalculatedFieldLinks(CalculatedField cf) {
        List<CalculatedFieldLink> links = cf.getConfiguration().buildCalculatedFieldLinks(cf.getTenantId(), cf.getEntityId(), cf.getId());
        for (CalculatedFieldLink link : links) {
            // We use copy on write lists to safely pass the reference to another actor for the iteration.
            // Alternative approach would be to use any list but avoid modifications to the list (change the complete map value instead)
            entityIdCalculatedFieldLinks.computeIfAbsent(link.entityId(), id -> new CopyOnWriteArrayList<>()).add(link);
        }
    }

    private void initEntitiesCache() {
        PageDataIterable<ProfileEntityIdInfo> deviceIdInfos = new PageDataIterable<>(pageLink -> deviceService.findProfileEntityIdInfosByTenantId(tenantId, pageLink), cfSettings.getInitTenantFetchPackSize());
        for (ProfileEntityIdInfo idInfo : deviceIdInfos) {
            log.trace("Processing device record: {}", idInfo);
            try {
                entityProfileCache.add(idInfo.getProfileId(), idInfo.getEntityId());
                ownerEntities.computeIfAbsent(idInfo.getOwnerId(), __ -> new HashSet<>()).add(idInfo.getEntityId());
            } catch (Exception e) {
                log.error("Failed to process device record: {}", idInfo, e);
            }
        }

        PageDataIterable<ProfileEntityIdInfo> assetIdInfos = new PageDataIterable<>(pageLink -> assetService.findProfileEntityIdInfosByTenantId(tenantId, pageLink), cfSettings.getInitTenantFetchPackSize());
        for (ProfileEntityIdInfo idInfo : assetIdInfos) {
            log.trace("Processing asset record: {}", idInfo);
            try {
                entityProfileCache.add(idInfo.getProfileId(), idInfo.getEntityId());
                ownerEntities.computeIfAbsent(idInfo.getOwnerId(), __ -> new HashSet<>()).add(idInfo.getEntityId());
            } catch (Exception e) {
                log.error("Failed to process asset record: {}", idInfo, e);
            }
        }

        PageDataIterable<Customer> customers = new PageDataIterable<>(pageLink -> customerService.findCustomersByTenantId(tenantId, pageLink), cfSettings.getInitTenantFetchPackSize());
        for (Customer customer : customers) {
            log.trace("Processing customer record: {}", customer);
            try {
                ownerEntities.computeIfAbsent(customer.getTenantId(), __ -> new HashSet<>()).add(customer.getId());
            } catch (Exception e) {
                log.error("Failed to process customer record: {}", customer, e);
            }
        }
    }

    private void updateEntityOwner(EntityId entityId) {
        ownerEntities.values().forEach(entities -> entities.remove(entityId));
        EntityId owner = ownerService.getOwner(tenantId, entityId);
        ownerEntities.computeIfAbsent(owner, ownerId -> new HashSet<>()).add(entityId);
    }

    private void applyToTargetCfEntityActors(CalculatedFieldCtx ctx,
                                             TbCallback callback,
                                             BiConsumer<EntityId, TbCallback> action) {
        withTargetEntities(ctx.getEntityId(), callback, (ids, cb) -> ids.forEach(id -> action.accept(id, cb)));
    }

    private void withTargetEntities(EntityId entityId, TbCallback parentCallback, BiConsumer<List<EntityId>, TbCallback> consumer) {
        if (isProfileEntity(entityId.getEntityType())) {
            var ids = entityProfileCache.getEntityIdsByProfileId(entityId);
            if (ids.isEmpty()) {
                parentCallback.onSuccess();
                return;
            }
            var multiCallback = new MultipleTbCallback(ids.size(), parentCallback);
            var profileEntityIds = ids.stream().filter(id -> isMyPartition(id, multiCallback)).toList();
            if (profileEntityIds.isEmpty()) {
                return;
            }
            consumer.accept(profileEntityIds, multiCallback);
            return;
        }
        if (isMyPartition(entityId, parentCallback)) {
            consumer.accept(List.of(entityId), parentCallback);
        }
    }

    private void cancelReevaluationTask() {
        if (cfsReevaluationTask != null) {
            cfsReevaluationTask.cancel(true);
            cfsReevaluationTask = null;
        }
    }

}
