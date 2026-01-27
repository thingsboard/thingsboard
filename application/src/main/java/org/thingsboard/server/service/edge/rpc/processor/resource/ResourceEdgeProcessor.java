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
package org.thingsboard.server.service.edge.rpc.processor.resource;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.TbResourceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.ResourceUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;

import java.util.UUID;

@Slf4j
@Component
@TbCoreComponent
public class ResourceEdgeProcessor extends BaseResourceProcessor implements ResourceProcessor {

    @Override
    public ListenableFuture<Void> processResourceMsgFromEdge(TenantId tenantId, Edge edge, ResourceUpdateMsg resourceUpdateMsg) {
        TbResourceId tbResourceId = new TbResourceId(new UUID(resourceUpdateMsg.getIdMSB(), resourceUpdateMsg.getIdLSB()));
        try {
            edgeSynchronizationManager.getEdgeId().set(edge.getId());

            switch (resourceUpdateMsg.getMsgType()) {
                case ENTITY_CREATED_RPC_MESSAGE:
                case ENTITY_UPDATED_RPC_MESSAGE:
                    boolean resourceKeyUpdated = super.saveOrUpdateTbResource(tenantId, tbResourceId, resourceUpdateMsg);
                    if (resourceKeyUpdated) {
                        saveEdgeEvent(tenantId, edge.getId(), EdgeEventType.TB_RESOURCE, EdgeEventActionType.UPDATED, tbResourceId, null);
                    }
                    break;
                case ENTITY_DELETED_RPC_MESSAGE:
                case UNRECOGNIZED:
                    return handleUnsupportedMsgType(resourceUpdateMsg.getMsgType());
            }
        } catch (DataValidationException e) {
            if (e.getMessage().contains("exceeds the maximum")) {
                log.warn("[{}] Resource data size has been exhausted {}", tenantId, resourceUpdateMsg, e);
                return Futures.immediateFuture(null);
            } else {
                return Futures.immediateFailedFuture(e);
            }
        } finally {
            edgeSynchronizationManager.getEdgeId().remove();
        }
        return Futures.immediateFuture(null);
    }

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        TbResourceId tbResourceId = new TbResourceId(edgeEvent.getEntityId());
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                TbResource tbResource = edgeCtx.getResourceService().findResourceById(edgeEvent.getTenantId(), tbResourceId);
                if (tbResource != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    ResourceUpdateMsg resourceUpdateMsg = EdgeMsgConstructorUtils.constructResourceUpdatedMsg(msgType, tbResource);
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addResourceUpdateMsg(resourceUpdateMsg)
                            .build();
                }
            }
            case DELETED -> {
                ResourceUpdateMsg resourceUpdateMsg = EdgeMsgConstructorUtils.constructResourceDeleteMsg(tbResourceId);
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addResourceUpdateMsg(resourceUpdateMsg)
                        .build();
            }
        }
        return null;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.TB_RESOURCE;
    }

}
