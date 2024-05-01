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
package org.thingsboard.server.service.edge.rpc.processor.relation;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.service.edge.rpc.constructor.relation.RelationMsgConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public abstract class RelationEdgeProcessor extends BaseRelationProcessor implements RelationProcessor {

    @Override
    public ListenableFuture<Void> processRelationMsgFromEdge(TenantId tenantId, Edge edge, RelationUpdateMsg relationUpdateMsg) {
        log.trace("[{}] executing processRelationMsgFromEdge [{}] from edge [{}]", tenantId, relationUpdateMsg, edge.getId());
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());
            return processRelationMsg(tenantId, relationUpdateMsg);
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
    }

    @Override
    public DownlinkMsg convertRelationEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        EntityRelation entityRelation = JacksonUtil.convertValue(edgeEvent.getBody(), EntityRelation.class);
        UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
        RelationUpdateMsg relationUpdateMsg = ((RelationMsgConstructor) relationMsgConstructorFactory.getMsgConstructorByEdgeVersion(edgeVersion))
                .constructRelationUpdatedMsg(msgType, entityRelation);
        return DownlinkMsg.newBuilder()
                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                .addRelationUpdateMsg(relationUpdateMsg)
                .build();
    }

    public ListenableFuture<Void> processRelationNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        EntityRelation relation = JacksonUtil.fromString(edgeNotificationMsg.getBody(), EntityRelation.class);
        if (relation == null || (relation.getFrom().getEntityType().equals(EntityType.EDGE) || relation.getTo().getEntityType().equals(EntityType.EDGE))) {
            return Futures.immediateFuture(null);
        }
        EdgeId originatorEdgeId = safeGetEdgeId(edgeNotificationMsg.getOriginatorEdgeIdMSB(), edgeNotificationMsg.getOriginatorEdgeIdLSB());

        Set<EdgeId> uniqueEdgeIds = new HashSet<>();
        uniqueEdgeIds.addAll(edgeService.findAllRelatedEdgeIds(tenantId, relation.getTo()));
        uniqueEdgeIds.addAll(edgeService.findAllRelatedEdgeIds(tenantId, relation.getFrom()));
        uniqueEdgeIds.remove(originatorEdgeId);
        if (uniqueEdgeIds.isEmpty()) {
            return Futures.immediateFuture(null);
        }
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        for (EdgeId edgeId : uniqueEdgeIds) {
            futures.add(saveEdgeEvent(tenantId,
                    edgeId,
                    EdgeEventType.RELATION,
                    EdgeEventActionType.valueOf(edgeNotificationMsg.getAction()),
                    null,
                    JacksonUtil.valueToTree(relation)));
        }
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }

}
