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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.RelationUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
@TbCoreComponent
public class RelationEdgeProcessor extends BaseRelationProcessor {

    public DownlinkMsg convertRelationEventToDownlink(EdgeEvent edgeEvent) {
        EntityRelation entityRelation = JacksonUtil.OBJECT_MAPPER.convertValue(edgeEvent.getBody(), EntityRelation.class);
        UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
        RelationUpdateMsg relationUpdateMsg = relationMsgConstructor.constructRelationUpdatedMsg(msgType, entityRelation);
        return DownlinkMsg.newBuilder()
                .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                .addRelationUpdateMsg(relationUpdateMsg)
                .build();
    }

    public ListenableFuture<Void> processRelationNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) throws JsonProcessingException {
        EntityRelation relation = JacksonUtil.OBJECT_MAPPER.readValue(edgeNotificationMsg.getBody(), EntityRelation.class);
        if (relation.getFrom().getEntityType().equals(EntityType.EDGE) ||
                relation.getTo().getEntityType().equals(EntityType.EDGE)) {
            return Futures.immediateFuture(null);
        }

        Set<EdgeId> uniqueEdgeIds = new HashSet<>();
        uniqueEdgeIds.addAll(edgeService.findAllRelatedEdgeIds(tenantId, relation.getTo()));
        uniqueEdgeIds.addAll(edgeService.findAllRelatedEdgeIds(tenantId, relation.getFrom()));
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
                    JacksonUtil.OBJECT_MAPPER.valueToTree(relation)));
        }
        return Futures.transform(Futures.allAsList(futures), voids -> null, dbCallbackExecutorService);
    }
}
