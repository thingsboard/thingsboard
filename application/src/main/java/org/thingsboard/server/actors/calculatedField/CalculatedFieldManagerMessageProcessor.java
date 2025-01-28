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
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.TbActorCtx;
import org.thingsboard.server.actors.TbActorRef;
import org.thingsboard.server.actors.TbCalculatedFieldEntityActorId;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.actors.shared.AbstractContextAwareMsgProcessor;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.cf.CalculatedFieldInitMsg;
import org.thingsboard.server.common.msg.cf.CalculatedFieldLinkInitMsg;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * @author Andrew Shvayka
 */
@Slf4j
public class CalculatedFieldManagerMessageProcessor extends AbstractContextAwareMsgProcessor {

    private final Map<CalculatedFieldId, CalculatedField> calculatedFields = new HashMap<>();
    private final Map<EntityId, List<CalculatedFieldCtx>> entityIdCalculatedFields = new ConcurrentHashMap<>();
    private final ConcurrentMap<EntityId, List<CalculatedFieldLink>> entityIdCalculatedFieldLinks = new ConcurrentHashMap<>();

    private final TbAssetProfileCache assetProfileCache;
    private final TbDeviceProfileCache deviceProfileCache;

    protected TbActorCtx ctx;
    final TenantId tenantId;

    CalculatedFieldManagerMessageProcessor(ActorSystemContext systemContext, TenantId tenantId) {
        super(systemContext);
        this.assetProfileCache = systemContext.getAssetProfileCache();
        this.deviceProfileCache = systemContext.getDeviceProfileCache();
        this.tenantId = tenantId;
    }

    void init(TbActorCtx ctx) {
        this.ctx = ctx;
    }

    public void onFieldInitMsg(CalculatedFieldInitMsg msg) {
        var cf = msg.getCf();
        calculatedFields.put(cf.getId(), cf);
        // We use copy on write lists to safely pass the reference to another actor for the iteration.
        // Alternative approach would be to use any list but avoid modifications to the list (change the complete map value instead)
        entityIdCalculatedFields.computeIfAbsent(cf.getEntityId(), id -> new CopyOnWriteArrayList<>())
                .add(new CalculatedFieldCtx(cf, systemContext.getTbelInvokeService()));
        msg.getCallback().onSuccess();
    }

    public void onLinkInitMsg(CalculatedFieldLinkInitMsg msg) {
        var link = msg.getLink();
        // We use copy on write lists to safely pass the reference to another actor for the iteration.
        // Alternative approach would be to use any list but avoid modifications to the list (change the complete map value instead)
        entityIdCalculatedFieldLinks.computeIfAbsent(link.getEntityId(), id -> new CopyOnWriteArrayList<>()).add(link);
        msg.getCallback().onSuccess();
    }

    public void onStateRestoreMsg(CalculatedFieldStateRestoreMsg msg) {
        if (calculatedFields.containsKey(msg.getId().cfId())) {
            getOrCreateActor(msg.getId().entityId()).tell(msg);
        } else {
            // TODO: remove state from storage
        }
    }

    public void onTelemetryMsg(CalculatedFieldTelemetryMsg msg) {
        EntityId entityId = msg.getEntityId();
        var proto = msg.getProto();
        // process all cfs related to entity, or it's profile;
        var entityIdFields = getCalculatedFieldsByEntityId(entityId);
        var profileIdFields = getCalculatedFieldsByEntityId(getProfileId(tenantId, entityId));
        //TODO: Transfer only 'part' of the original callback.
        getOrCreateActor(entityId).tell(new EntityCalculatedFieldTelemetryMsg(msg, entityIdFields, profileIdFields, msg.getCallback()));
        // process all links (if any);
        var links = getCalculatedFieldLinksByEntityId(entityId);
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

    private EntityId getProfileId(TenantId tenantId, EntityId entityId) {
        return switch (entityId.getEntityType()) {
            case ASSET -> assetProfileCache.get(tenantId, (AssetId) entityId).getId();
            case DEVICE -> deviceProfileCache.get(tenantId, (DeviceId) entityId).getId();
            default -> null;
        };
    }

    protected TbActorRef getOrCreateActor(EntityId entityId) {
        return ctx.getOrCreateChildActor(new TbCalculatedFieldEntityActorId(entityId),
                () -> DefaultActorService.CF_ENTITY_DISPATCHER_NAME,
                () -> new CalculatedFieldEntityActorCreator(systemContext, tenantId, entityId),
                () -> true);
    }

}
