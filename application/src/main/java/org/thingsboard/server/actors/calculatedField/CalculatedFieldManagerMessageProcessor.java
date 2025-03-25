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
package org.thingsboard.server.actors.calculatedField;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.actors.TbCalculatedFieldEntityActorId;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.actors.shared.AbstractContextAwareMsgProcessor;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.cf.CalculatedFieldEntityLifecycleMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldInitMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldLinkInitMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldPartitionChangeMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.service.cf.CalculatedFieldProcessingService;
import org.thingsboard.server.service.cf.CalculatedFieldStateService;
import org.thingsboard.server.service.cf.cache.CalculatedFieldEntityProfileCache;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.thingsboard.server.utils.CalculatedFieldUtils.fromProto;


/**
 * @author Andrew Shvayka
 */
@Slf4j
public class CalculatedFieldManagerMessageProcessor extends AbstractContextAwareMsgProcessor {

    private final Map<CalculatedFieldId, CalculatedFieldCtx> calculatedFields = new HashMap<>();
    private final Map<EntityId, List<CalculatedFieldCtx>> entityIdCalculatedFields = new HashMap<>();
    private final Map<EntityId, List<CalculatedFieldLink>> entityIdCalculatedFieldLinks = new HashMap<>();

    private final CalculatedFieldProcessingService cfExecService;
    private final CalculatedFieldStateService cfStateService;
    private final CalculatedFieldEntityProfileCache cfEntityCache;
    private final CalculatedFieldService cfDaoService;
    private final TbAssetProfileCache assetProfileCache;
    private final TbDeviceProfileCache deviceProfileCache;
    protected final TenantId tenantId;

    protected TbActorCtx ctx;

    CalculatedFieldManagerMessageProcessor(ActorSystemContext systemContext, TenantId tenantId) {
        super(systemContext);
        this.cfEntityCache = systemContext.getCalculatedFieldEntityProfileCache();
        this.cfExecService = systemContext.getCalculatedFieldProcessingService();
        this.cfStateService = systemContext.getCalculatedFieldStateService();
        this.cfDaoService = systemContext.getCalculatedFieldService();
        this.assetProfileCache = systemContext.getAssetProfileCache();
        this.deviceProfileCache = systemContext.getDeviceProfileCache();
        this.tenantId = tenantId;
    }

    void init(TbActorCtx ctx) {
        this.ctx = ctx;
    }

    public void onFieldInitMsg(CalculatedFieldInitMsg msg) throws CalculatedFieldException {
        log.info("[{}] Processing CF init message.", msg.getCf().getId());
        var cf = msg.getCf();
        var cfCtx = new CalculatedFieldCtx(cf, systemContext.getTbelInvokeService(), systemContext.getApiLimitService());
        try {
            cfCtx.init();
        } catch (Exception e) {
            throw CalculatedFieldException.builder().ctx(cfCtx).eventEntity(cf.getEntityId()).cause(e).errorMessage("Failed to initialize CF context").build();
        }
        calculatedFields.put(cf.getId(), cfCtx);
        // We use copy on write lists to safely pass the reference to another actor for the iteration.
        // Alternative approach would be to use any list but avoid modifications to the list (change the complete map value instead)
        entityIdCalculatedFields.computeIfAbsent(cf.getEntityId(), id -> new CopyOnWriteArrayList<>()).add(cfCtx);
        msg.getCallback().onSuccess();
    }

    public void onLinkInitMsg(CalculatedFieldLinkInitMsg msg) {
        log.info("[{}] Processing CF link init message for entity [{}].", msg.getLink().getCalculatedFieldId(), msg.getLink().getEntityId());
        var link = msg.getLink();
        // We use copy on write lists to safely pass the reference to another actor for the iteration.
        // Alternative approach would be to use any list but avoid modifications to the list (change the complete map value instead)
        entityIdCalculatedFieldLinks.computeIfAbsent(link.getEntityId(), id -> new CopyOnWriteArrayList<>()).add(link);
        msg.getCallback().onSuccess();
    }

    public void onStateRestoreMsg(CalculatedFieldStateRestoreMsg msg) {
        var cfId = msg.getId().cfId();
        var calculatedField = calculatedFields.get(cfId);

        if (calculatedField != null) {
            msg.getState().setRequiredArguments(calculatedField.getArgNames());
            log.debug("Pushing CF state restore msg to specific actor [{}]", msg.getId().entityId());
            getOrCreateActor(msg.getId().entityId()).tell(msg);
        } else {
            cfStateService.removeState(msg.getId(), msg.getCallback());
        }
    }

    public void onEntityLifecycleMsg(CalculatedFieldEntityLifecycleMsg msg) throws CalculatedFieldException {
        log.info("Processing entity lifecycle event: [{}] for entity: [{}]", msg.getData().getEvent(), msg.getData().getEntityId());
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
        EntityId profileId = getProfileId(tenantId, entityId);
        cfEntityCache.add(tenantId, profileId, entityId);
        if (!isMyPartition(entityId, callback)) {
            return;
        }
        var entityIdFields = getCalculatedFieldsByEntityId(entityId);
        var profileIdFields = getCalculatedFieldsByEntityId(profileId);
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
            cfEntityCache.update(tenantId, msg.getOldProfileId(), msg.getProfileId(), msg.getEntityId());
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
                newProfileCfs.forEach(ctx -> initCfForEntity(entityId, ctx, true, multiCallback));
            } else {
                callback.onSuccess();
            }
        }
    }

    private void onEntityDeleted(ComponentLifecycleMsg msg, TbCallback callback) {
        cfEntityCache.evict(tenantId, msg.getEntityId());
        if (isMyPartition(msg.getEntityId(), callback)) {
            log.debug("Pushing entity lifecycle msg to specific actor [{}]", msg.getEntityId());
            getOrCreateActor(msg.getEntityId()).tell(new CalculatedFieldEntityDeleteMsg(tenantId, msg.getEntityId(), callback));
        }
    }

    private void onCfCreated(ComponentLifecycleMsg msg, TbCallback callback) throws CalculatedFieldException {
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
                var cfCtx = new CalculatedFieldCtx(cf, systemContext.getTbelInvokeService(), systemContext.getApiLimitService());
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
                initCf(cfCtx, callback, false);
            }
        }
    }

    private void onCfUpdated(ComponentLifecycleMsg msg, TbCallback callback) throws CalculatedFieldException {
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
                var newCfCtx = new CalculatedFieldCtx(newCf, systemContext.getTbelInvokeService(), systemContext.getApiLimitService());
                try {
                    newCfCtx.init();
                } catch (Exception e) {
                    throw CalculatedFieldException.builder().ctx(newCfCtx).eventEntity(newCfCtx.getEntityId()).cause(e).errorMessage("Failed to initialize CF context").build();
                }
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
                entityIdCalculatedFields.put(newCf.getEntityId(), newCfList);

                deleteLinks(oldCfCtx);
                addLinks(newCf);

                // We use copy on write lists to safely pass the reference to another actor for the iteration.
                // Alternative approach would be to use any list but avoid modifications to the list (change the complete map value instead)
                var stateChanges = newCfCtx.hasStateChanges(oldCfCtx);
                if (stateChanges || newCfCtx.hasOtherSignificantChanges(oldCfCtx)) {
                    initCf(newCfCtx, callback, stateChanges);
                } else {
                    callback.onSuccess();
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
            entityIdCalculatedFields.get(cfCtx.getEntityId()).remove(cfCtx);
            deleteLinks(cfCtx);

            EntityId entityId = cfCtx.getEntityId();
            EntityType entityType = cfCtx.getEntityId().getEntityType();
            if (isProfileEntity(entityType)) {
                var entityIds = cfEntityCache.getMyEntityIdsByProfileId(tenantId, entityId);
                if (!entityIds.isEmpty()) {
                    //TODO: no need to do this if we cache all created actors and know which one belong to us;
                    var multiCallback = new MultipleTbCallback(entityIds.size(), callback);
                    entityIds.forEach(id -> deleteCfForEntity(id, cfId, multiCallback));
                } else {
                    callback.onSuccess();
                }
            } else {
                if (isMyPartition(entityId, callback)) {
                    deleteCfForEntity(entityId, cfId, callback);
                }
            }
        }
    }

    public void onTelemetryMsg(CalculatedFieldTelemetryMsg msg) {
        EntityId entityId = msg.getEntityId();
        log.debug("Received telemetry msg from entity [{}]", entityId);
        // 2 = 1 for CF processing + 1 for links processing
        MultipleTbCallback callback = new MultipleTbCallback(2, msg.getCallback());
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
    }

    public void onLinkedTelemetryMsg(CalculatedFieldLinkedTelemetryMsg msg) {
        EntityId sourceEntityId = msg.getEntityId();
        log.debug("Received linked telemetry msg from entity [{}]", sourceEntityId);
        var proto = msg.getProto();
        var linksList = proto.getLinksList();
        for (var linkProto : linksList) {
            var link = fromProto(linkProto);
            var targetEntityId = link.entityId();
            var targetEntityType = targetEntityId.getEntityType();
            var cf = calculatedFields.get(link.cfId());
            if (EntityType.DEVICE_PROFILE.equals(targetEntityType) || EntityType.ASSET_PROFILE.equals(targetEntityType)) {
                // iterate over all entities that belong to profile and push the message for corresponding CF
                var entityIds = cfEntityCache.getMyEntityIdsByProfileId(tenantId, targetEntityId);
                if (!entityIds.isEmpty()) {
                    MultipleTbCallback callback = new MultipleTbCallback(entityIds.size(), msg.getCallback());
                    var newMsg = new EntityCalculatedFieldLinkedTelemetryMsg(tenantId, sourceEntityId, proto.getMsg(), cf, callback);
                    entityIds.forEach(entityId -> {
                        log.debug("Pushing linked telemetry msg to specific actor [{}]", entityId);
                        getOrCreateActor(entityId).tell(newMsg);
                    });
                } else {
                    msg.getCallback().onSuccess();
                }
            } else {
                log.debug("Pushing linked telemetry msg to specific actor [{}]", targetEntityId);
                var newMsg = new EntityCalculatedFieldLinkedTelemetryMsg(tenantId, sourceEntityId, proto.getMsg(), cf, msg.getCallback());
                getOrCreateActor(targetEntityId).tell(newMsg);
            }
        }
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

    private void initCf(CalculatedFieldCtx cfCtx, TbCallback callback, boolean forceStateReinit) {
        EntityId entityId = cfCtx.getEntityId();
        EntityType entityType = cfCtx.getEntityId().getEntityType();
        if (isProfileEntity(entityType)) {
            var entityIds = cfEntityCache.getMyEntityIdsByProfileId(tenantId, entityId);
            if (!entityIds.isEmpty()) {
                var multiCallback = new MultipleTbCallback(entityIds.size(), callback);
                entityIds.forEach(id -> initCfForEntity(id, cfCtx, forceStateReinit, multiCallback));
            } else {
                callback.onSuccess();
            }
        } else {
            if (isMyPartition(entityId, callback)) {
                initCfForEntity(entityId, cfCtx, forceStateReinit, callback);
            }
        }
    }

    private void deleteCfForEntity(EntityId entityId, CalculatedFieldId cfId, TbCallback callback) {
        log.debug("Pushing delete CF msg to specific actor [{}]", entityId);
        getOrCreateActor(entityId).tell(new CalculatedFieldEntityDeleteMsg(tenantId, cfId, callback));
    }

    private void initCfForEntity(EntityId entityId, CalculatedFieldCtx cfCtx, boolean forceStateReinit, TbCallback callback) {
        log.debug("Pushing entity init CF msg to specific actor [{}]", entityId);
        getOrCreateActor(entityId).tell(new EntityInitCalculatedFieldMsg(tenantId, cfCtx, callback, forceStateReinit));
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

    private void addLinks(CalculatedField newCf) {
        var newLinks = newCf.getConfiguration().buildCalculatedFieldLinks(tenantId, newCf.getEntityId(), newCf.getId());
        newLinks.forEach(link -> entityIdCalculatedFieldLinks.computeIfAbsent(link.getEntityId(), id -> new CopyOnWriteArrayList<>()).add(link));
    }

    private void deleteLinks(CalculatedFieldCtx cfCtx) {
        var oldCf = cfCtx.getCalculatedField();
        var oldLinks = oldCf.getConfiguration().buildCalculatedFieldLinks(tenantId, oldCf.getEntityId(), oldCf.getId());
        oldLinks.forEach(link -> entityIdCalculatedFieldLinks.computeIfAbsent(link.getEntityId(), id -> new CopyOnWriteArrayList<>()).remove(link));
    }

    public void onPartitionChange(CalculatedFieldPartitionChangeMsg msg) {
        ctx.broadcastToChildren(msg, true);
    }
}
