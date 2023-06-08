/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor.relation;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.gen.edge.v1.RelationUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

import java.util.UUID;

@Slf4j
public abstract class BaseRelationProcessor extends BaseEdgeProcessor {

    public ListenableFuture<Void> processRelationMsg(TenantId tenantId, RelationUpdateMsg relationUpdateMsg) {
        log.trace("[{}] processRelationFromEdge [{}]", tenantId, relationUpdateMsg);
        try {
            EntityRelation entityRelation = new EntityRelation();

            UUID fromUUID = new UUID(relationUpdateMsg.getFromIdMSB(), relationUpdateMsg.getFromIdLSB());
            EntityId fromId = EntityIdFactory.getByTypeAndUuid(EntityType.valueOf(relationUpdateMsg.getFromEntityType()), fromUUID);
            entityRelation.setFrom(fromId);

            UUID toUUID = new UUID(relationUpdateMsg.getToIdMSB(), relationUpdateMsg.getToIdLSB());
            EntityId toId = EntityIdFactory.getByTypeAndUuid(EntityType.valueOf(relationUpdateMsg.getToEntityType()), toUUID);
            entityRelation.setTo(toId);

            entityRelation.setType(relationUpdateMsg.getType());
            entityRelation.setTypeGroup(relationUpdateMsg.hasTypeGroup()
                    ? RelationTypeGroup.valueOf(relationUpdateMsg.getTypeGroup()) : RelationTypeGroup.COMMON);
            entityRelation.setAdditionalInfo(JacksonUtil.toJsonNode(relationUpdateMsg.getAdditionalInfo()));
            switch (relationUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    if (isEntityExists(tenantId, entityRelation.getTo())
                            && isEntityExists(tenantId, entityRelation.getFrom())) {
                        return Futures.transform(relationService.saveRelationAsync(tenantId, entityRelation),
                                (result) -> null, dbCallbackExecutorService);
                    } else {
                        log.warn("Skipping relating update msg because from/to entity doesn't exists on edge, {}", relationUpdateMsg);
                        return Futures.immediateFuture(null);
                    }
                case ENTITY_DELETED_RPC_MESSAGE:
                    return Futures.transform(relationService.deleteRelationAsync(tenantId, entityRelation),
                            (result) -> null, dbCallbackExecutorService);
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(relationUpdateMsg.getMsgType());
            }
        } catch (Exception e) {
            log.error("[{}] Failed to process relation update msg [{}]", tenantId, relationUpdateMsg, e);
            return Futures.immediateFailedFuture(e);
        }
    }
}
