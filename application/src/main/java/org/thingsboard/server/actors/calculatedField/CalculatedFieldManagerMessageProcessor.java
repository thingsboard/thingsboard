/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
import org.thingsboard.common.util.DebugModeUtil;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.actors.TbCalculatedFieldEntityActorId;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.actors.shared.AbstractContextAwareMsgProcessor;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.msg.cf.CalculatedFieldEntityLifecycleMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldInitMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldLinkInitMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldEntityCtxIdProto;
import org.thingsboard.server.service.cf.CalculatedFieldExecutionService;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * @author Andrew Shvayka
 */
@Slf4j
public class CalculatedFieldManagerMessageProcessor extends AbstractContextAwareMsgProcessor {

    private final Map<CalculatedFieldId, CalculatedFieldCtx> calculatedFields = new HashMap<>();
    private final Map<EntityId, List<CalculatedFieldCtx>> entityIdCalculatedFields = new ConcurrentHashMap<>();
    private final ConcurrentMap<EntityId, List<CalculatedFieldLink>> entityIdCalculatedFieldLinks = new ConcurrentHashMap<>();
    private final ConcurrentMap<EntityId, Set<EntityId>> profileEntities = new ConcurrentHashMap<>();

    private final CalculatedFieldExecutionService cfExecService;
    private final CalculatedFieldService cfDaoService;
    private final TbAssetProfileCache assetProfileCache;
    private final TbDeviceProfileCache deviceProfileCache;

    protected TbActorCtx ctx;
    final TenantId tenantId;
    private final static int initFetchPackSize = 1024;

    CalculatedFieldManagerMessageProcessor(ActorSystemContext systemContext, TenantId tenantId) {
        super(systemContext);
        this.cfExecService = systemContext.getCalculatedFieldExecutionService();
        this.cfDaoService = systemContext.getCalculatedFieldService();
        this.assetProfileCache = systemContext.getAssetProfileCache();
        this.deviceProfileCache = systemContext.getDeviceProfileCache();
        this.tenantId = tenantId;
    }

    void init(TbActorCtx ctx) {
        this.ctx = ctx;
    }

    public void onFieldInitMsg(CalculatedFieldInitMsg msg) {
        var cf = msg.getCf();
        var cfCtx = new CalculatedFieldCtx(cf, systemContext.getTbelInvokeService());
        try {
            cfCtx.init();
        } catch (Exception e) {
            if (DebugModeUtil.isDebugAllAvailable(cf)) {
                systemContext.persistCalculatedFieldDebugEvent(cf.getTenantId(), cf.getId(), cf.getEntityId(), null, null, null, null, e);
            }
        }
        calculatedFields.put(cf.getId(), cfCtx);
        // We use copy on write lists to safely pass the reference to another actor for the iteration.
        // Alternative approach would be to use any list but avoid modifications to the list (change the complete map value instead)
        entityIdCalculatedFields.computeIfAbsent(cf.getEntityId(), id -> new ArrayList<>()).add(cfCtx);
        msg.getCallback().onSuccess();
    }

    public void onLinkInitMsg(CalculatedFieldLinkInitMsg msg) {
        var link = msg.getLink();
        // We use copy on write lists to safely pass the reference to another actor for the iteration.
        // Alternative approach would be to use any list but avoid modifications to the list (change the complete map value instead)
        entityIdCalculatedFieldLinks.computeIfAbsent(link.getEntityId(), id -> new ArrayList<>()).add(link);
        msg.getCallback().onSuccess();
    }

    public void onStateRestoreMsg(CalculatedFieldStateRestoreMsg msg) {
        if (calculatedFields.containsKey(msg.getId().cfId())) {
            getOrCreateActor(msg.getId().entityId()).tell(msg);
        } else {
            cfExecService.deleteStateFromStorage(msg.getId(), msg.getCallback());
        }
    }

    public void onEntityLifecycleMsg(CalculatedFieldEntityLifecycleMsg msg) {
        var entityType = msg.getData().getEntityId().getEntityType();
        var event = msg.getData().getEvent();
        switch (entityType) {
            case CALCULATED_FIELD: {
                switch (event) {
                    case CREATED:
                        onCfCreated(msg.getData(), msg.getCallback());
                        break;
                    case UPDATED:
                        onCfUpdated(msg.getData(), msg.getCallback());
                        break;
                    case DELETED:
                        onCfDeleted(msg.getData(), msg.getCallback());
                        break;
                    default:
                        msg.getCallback().onSuccess();
                        break;
                }
                break;
            }
            case DEVICE:
            case ASSET: {
                switch (event) {
                    case CREATED:
                        onEntityCreated(msg.getData(), msg.getCallback());
                        break;
                    case UPDATED:
                        onEntityUpdated(msg.getData(), msg.getCallback());
                        break;
                    case DELETED:
                        onEntityDeleted(msg.getData(), msg.getCallback());
                        break;
                    default:
                        msg.getCallback().onSuccess();
                        break;
                }
                break;
            }
            default: {
                msg.getCallback().onSuccess();
            }
        }
    }

    private void onEntityCreated(ComponentLifecycleMsg msg, TbCallback callback) {
        EntityId entityId = msg.getEntityId();
        var entityIdFields = getCalculatedFieldsByEntityId(entityId);
        var profileIdFields = getCalculatedFieldsByEntityId(getProfileId(tenantId, entityId));
        var fieldsCount = entityIdFields.size() + profileIdFields.size();
        if (fieldsCount > 0) {
            MultipleTbCallback multiCallback = new MultipleTbCallback(fieldsCount, callback);
            entityIdFields.forEach(ctx -> initCfForEntity(entityId, ctx, true, multiCallback));
            profileIdFields.forEach(ctx -> initCfForEntity(entityId, ctx, true, multiCallback));
        } else {
            callback.onSuccess();
        }
    }

    private void onEntityUpdated(ComponentLifecycleMsg msg, TbCallback callback) {
        if (msg.getOldProfileId() != null && msg.getOldProfileId() != msg.getProfileId()) {
            var oldProfileCfs = getCalculatedFieldsByEntityId(msg.getOldProfileId());
            var newProfileCfs = getCalculatedFieldsByEntityId(msg.getProfileId());
            var fieldsCount = oldProfileCfs.size() + newProfileCfs.size();
            if (fieldsCount > 0) {
                MultipleTbCallback multiCallback = new MultipleTbCallback(fieldsCount, callback);
                var entityId = msg.getEntityId();
                oldProfileCfs.forEach(ctx -> deleteCfForEntity(entityId, ctx.getCfId(), callback));
                newProfileCfs.forEach(ctx -> initCfForEntity(entityId, ctx, true, multiCallback));
            } else {
                callback.onSuccess();
            }
        }
    }

    private void onEntityDeleted(ComponentLifecycleMsg msg, TbCallback callback) {
        getOrCreateActor(msg.getEntityId()).tell(new CalculatedFieldEntityDeleteMsg(tenantId, msg.getEntityId(), callback));
    }

    private void onCfCreated(ComponentLifecycleMsg msg, TbCallback callback) {
        var cfId = new CalculatedFieldId(msg.getEntityId().getId());
        if (calculatedFields.containsKey(cfId)) {
            log.warn("[{}] CF was already initialized [{}]", tenantId, cfId);
            callback.onSuccess();
        } else {
            var cf = cfDaoService.findById(msg.getTenantId(), cfId);
            if (cf == null) {
                log.warn("[{}] Failed to lookup CF by id [{}]", tenantId, cfId);
                callback.onSuccess();
            } else {
                var cfCtx = new CalculatedFieldCtx(cf, systemContext.getTbelInvokeService());
                try {
                    cfCtx.init();
                } catch (Exception e) {
                    if (DebugModeUtil.isDebugAllAvailable(cf)) {
                        systemContext.persistCalculatedFieldDebugEvent(cf.getTenantId(), cf.getId(), cf.getEntityId(), null, null, null, null, e);
                    }
                }
                calculatedFields.put(cf.getId(), cfCtx);
                // We use copy on write lists to safely pass the reference to another actor for the iteration.
                // Alternative approach would be to use any list but avoid modifications to the list (change the complete map value instead)
                entityIdCalculatedFields.computeIfAbsent(cf.getEntityId(), id -> new CopyOnWriteArrayList<>()).add(cfCtx);
                initCf(cfCtx, callback, false);
            }
        }
    }

    private void onCfUpdated(ComponentLifecycleMsg msg, TbCallback callback) {
        var cfId = new CalculatedFieldId(msg.getEntityId().getId());
        var oldCfCtx = calculatedFields.get(cfId);
        if (oldCfCtx == null) {
            onCfCreated(msg, callback);
        } else {
            var newCf = cfDaoService.findById(msg.getTenantId(), cfId);
            if (newCf == null) {
                log.warn("[{}] Failed to lookup CF by id [{}]", tenantId, cfId);
                callback.onSuccess();
            } else {
                var newCfCtx = new CalculatedFieldCtx(newCf, systemContext.getTbelInvokeService());
                calculatedFields.put(newCf.getId(), newCfCtx);
                List<CalculatedFieldCtx> oldCfList = entityIdCalculatedFields.get(newCf.getId());
                List<CalculatedFieldCtx> newCfList = new ArrayList<>(oldCfList.size());
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
                entityIdCalculatedFields.put(newCf.getId(), newCfList);
                // We use copy on write lists to safely pass the reference to another actor for the iteration.
                // Alternative approach would be to use any list but avoid modifications to the list (change the complete map value instead)
                if (newCfCtx.hasSignificantChanges(oldCfCtx)) {
                    try {
                        newCfCtx.init();
                    } catch (Exception e) {
                        if (DebugModeUtil.isDebugAllAvailable(newCf)) {
                            systemContext.persistCalculatedFieldDebugEvent(newCf.getTenantId(), newCf.getId(), newCf.getEntityId(), null, null, null, null, e);
                        }
                    }
                    initCf(newCfCtx, callback, true);
                }
            }
        }

    }

    private void onCfDeleted(ComponentLifecycleMsg msg, TbCallback callback) {
        var cfId = new CalculatedFieldId(msg.getEntityId().getId());
        var cfCtx = calculatedFields.remove(cfId);
        if (cfCtx == null) {
            log.warn("[{}] CF was already deleted [{}]", tenantId, cfId);
            callback.onSuccess();
        } else {
            EntityId entityId = cfCtx.getEntityId();
            EntityType entityType = cfCtx.getEntityId().getEntityType();
            if (isProfileEntity(entityType)) {
                var entityIds = getEntitiesByProfile(entityId);
                if (!entityIds.isEmpty()) {
                    //TODO: no need to do this if we cache all created actors and know which one belong to us;
                    var multiCallback = new MultipleTbCallback(entityIds.size(), callback);
                    entityIds.forEach(id -> deleteCfForEntity(entityId, cfId, multiCallback));
                } else {
                    callback.onSuccess();
                }
            } else {
                deleteCfForEntity(entityId, cfId, callback);
            }
        }
    }

    public void onTelemetryMsg(CalculatedFieldTelemetryMsg msg) {
        EntityId entityId = msg.getEntityId();
        // 2 = 1 for CF processing + 1 for links processing
        MultipleTbCallback callback = new MultipleTbCallback(2, msg.getCallback());
        // process all cfs related to entity, or it's profile;
        var entityIdFields = getCalculatedFieldsByEntityId(entityId);
        var profileIdFields = getCalculatedFieldsByEntityId(getProfileId(tenantId, entityId));
        if (!entityIdFields.isEmpty() || !profileIdFields.isEmpty()) {
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
    }

    public void onLinkedTelemetryMsg(CalculatedFieldLinkedTelemetryMsg msg) {
        EntityId sourceEntityId = msg.getEntityId();
        var proto = msg.getProto();
        var linksList = proto.getLinksList();
        for (var linkProto : linksList) {
            var link = toCalculatedFieldEntityCtxId(linkProto);
            var targetEntityId = link.entityId();
            var targetEntityType = targetEntityId.getEntityType();
            var cf = calculatedFields.get(link.cfId());
            if (EntityType.DEVICE_PROFILE.equals(targetEntityType) || EntityType.ASSET_PROFILE.equals(targetEntityType)) {
                // iterate over all entities that belong to profile and push the message for corresponding CF
                var entityIds = getEntitiesByProfile(targetEntityId);
                if (!entityIds.isEmpty()) {
                    MultipleTbCallback callback = new MultipleTbCallback(entityIds.size(), msg.getCallback());
                    var newMsg = new EntityCalculatedFieldLinkedTelemetryMsg(tenantId, sourceEntityId, proto.getMsg(), cf, callback);
                    entityIds.forEach(entityId -> getOrCreateActor(entityId).tell(newMsg));
                } else {
                    msg.getCallback().onSuccess();
                }
            } else {
                // push the message to specific entity;
                var newMsg = new EntityCalculatedFieldLinkedTelemetryMsg(tenantId, sourceEntityId, proto.getMsg(), cf, msg.getCallback());
                getOrCreateActor(targetEntityId).tell(newMsg);
            }
        }
    }

    private CalculatedFieldEntityCtxId toCalculatedFieldEntityCtxId(CalculatedFieldEntityCtxIdProto ctxIdProto) {
        EntityId entityId = EntityIdFactory.getByTypeAndUuid(ctxIdProto.getEntityType(), new UUID(ctxIdProto.getEntityIdMSB(), ctxIdProto.getEntityIdLSB()));
        CalculatedFieldId calculatedFieldId = new CalculatedFieldId(new UUID(ctxIdProto.getCalculatedFieldIdMSB(), ctxIdProto.getCalculatedFieldIdLSB()));
        return new CalculatedFieldEntityCtxId(tenantId, calculatedFieldId, entityId);
    }

    private List<CalculatedFieldEntityCtxId> filterCalculatedFieldLinks(CalculatedFieldTelemetryMsg msg) {
        EntityId entityId = msg.getEntityId();
        var proto = msg.getProto();
        List<CalculatedFieldEntityCtxId> result = new ArrayList<>();
        for (var link : getCalculatedFieldLinksByEntityId(entityId)) {
            CalculatedFieldCtx ctx = calculatedFields.get(link.getCalculatedFieldId());
            if (ctx.linkMatches(entityId, proto)) {
                result.add(ctx.toCalculatedFieldEntityCtxId());
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

    private Set<EntityId> getEntitiesByProfile(EntityId entityProfileId) {
        Set<EntityId> entities = profileEntities.get(entityProfileId);
        if (entities == null) {
            entities = switch (entityProfileId.getEntityType()) {
                case ASSET_PROFILE -> profileEntities.computeIfAbsent(entityProfileId, profileId -> {
                    Set<EntityId> assetIds = new HashSet<>();
                    (new PageDataIterable<>(pageLink ->
                            systemContext.getAssetService().findAssetIdsByTenantIdAndAssetProfileId(tenantId, (AssetProfileId) profileId, pageLink), initFetchPackSize)).forEach(assetIds::add);
                    return assetIds;
                });
                case DEVICE_PROFILE -> profileEntities.computeIfAbsent(entityProfileId, profileId -> {
                    Set<EntityId> deviceIds = new HashSet<>();
                    (new PageDataIterable<>(pageLink ->
                            systemContext.getDeviceService().findDeviceIdsByTenantIdAndDeviceProfileId(tenantId, (DeviceProfileId) entityProfileId, pageLink), initFetchPackSize)).forEach(deviceIds::add);
                    return deviceIds;
                });
                default -> throw new IllegalArgumentException("Entity type should be ASSET_PROFILE or DEVICE_PROFILE.");
            };
        }
        log.trace("[{}] Found entities by profile in cache: {}", entityProfileId, entities);
        return entities;
    }

    private void initCf(CalculatedFieldCtx cfCtx, TbCallback callback, boolean forceStateReinit) {
        EntityId entityId = cfCtx.getEntityId();
        EntityType entityType = cfCtx.getEntityId().getEntityType();
        if (isProfileEntity(entityType)) {
            var entityIds = getEntitiesByProfile(entityId);
            if (!entityIds.isEmpty()) {
                var multiCallback = new MultipleTbCallback(entityIds.size(), callback);
                entityIds.forEach(id -> initCfForEntity(id, cfCtx, forceStateReinit, multiCallback));
            } else {
                callback.onSuccess();
            }
        } else {
            initCfForEntity(entityId, cfCtx, forceStateReinit, callback);
        }
    }

    private void deleteCfForEntity(EntityId entityId, CalculatedFieldId cfId, TbCallback callback) {
        getOrCreateActor(entityId).tell(new CalculatedFieldEntityDeleteMsg(tenantId, cfId, callback));
    }

    private void initCfForEntity(EntityId entityId, CalculatedFieldCtx cfCtx, boolean forceStateReinit, TbCallback callback) {
        getOrCreateActor(entityId).tell(new EntityInitCalculatedFieldMsg(tenantId, cfCtx, callback, forceStateReinit));
    }

    private static boolean isProfileEntity(EntityType entityType) {
        return EntityType.DEVICE_PROFILE.equals(entityType) || EntityType.ASSET_PROFILE.equals(entityType);
    }

    private EntityId getProfileId(TenantId tenantId, EntityId entityId) {
        return switch (entityId.getEntityType()) {
            case ASSET -> assetProfileCache.get(tenantId, (AssetId) entityId).getId();
            case DEVICE -> deviceProfileCache.get(tenantId, (DeviceId) entityId).getId();
            default -> null;
        };
    }

    private TbActorRef getOrCreateActor(EntityId entityId) {
        return ctx.getOrCreateChildActor(new TbCalculatedFieldEntityActorId(entityId),
                () -> DefaultActorService.CF_ENTITY_DISPATCHER_NAME,
                () -> new CalculatedFieldEntityActorCreator(systemContext, tenantId, entityId),
                () -> true);
    }

}
