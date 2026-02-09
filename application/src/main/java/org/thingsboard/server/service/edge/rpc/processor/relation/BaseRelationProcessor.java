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
package org.thingsboard.server.service.edge.rpc.processor.relation;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.gen.edge.v1.RelationUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
public abstract class BaseRelationProcessor extends BaseEdgeProcessor {

    protected ListenableFuture<Void> processRelationMsg(TenantId tenantId, RelationUpdateMsg relationUpdateMsg) {
        log.trace("[{}] processRelationMsg [{}]", tenantId, relationUpdateMsg);
        try {
            EntityRelation entityRelation = JacksonUtil.fromString(relationUpdateMsg.getEntity(), EntityRelation.class, true);
            if (entityRelation == null) {
                throw new RuntimeException("[{" + tenantId + "}] relationUpdateMsg {" + relationUpdateMsg + "} cannot be converted to entity relation");
            }
            switch (relationUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    if (isEntityExists(tenantId, entityRelation.getTo()) && isEntityExists(tenantId, entityRelation.getFrom())) {
                        edgeCtx.getRelationService().saveRelation(tenantId, entityRelation);
                    } else {
                        log.warn("[{}] Skipping relating update msg because from/to entity doesn't exists on edge, {}", tenantId, relationUpdateMsg);
                    }
                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                    edgeCtx.getRelationService().deleteRelation(tenantId, entityRelation);
                    break;
                case UNRECOGNIZED:
                default:
                    return handleUnsupportedMsgType(relationUpdateMsg.getMsgType());
            }
        } catch (Exception e) {
            log.error("[{}] Failed to process relation update msg [{}]", tenantId, relationUpdateMsg, e);
            return Futures.immediateFailedFuture(e);
        }
        return Futures.immediateFuture(null);
    }

}
