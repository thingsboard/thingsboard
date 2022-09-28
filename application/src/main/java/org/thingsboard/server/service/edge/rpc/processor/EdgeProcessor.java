/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.processor;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeConfiguration;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Component
@Slf4j
@TbCoreComponent
public class EdgeProcessor extends BaseEdgeProcessor {

    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent) {
        EdgeId edgeId = new EdgeId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ASSIGNED_TO_CUSTOMER:
            case UNASSIGNED_FROM_CUSTOMER:
                Edge edge = edgeService.findEdgeById(edgeEvent.getTenantId(), edgeId);
                if (edge != null) {
                    EdgeConfiguration edgeConfigMsg =
                            edgeMsgConstructor.constructEdgeConfiguration(edge);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .setEdgeConfiguration(edgeConfigMsg)
                            .build();
                }
                break;
        }
        return downlinkMsg;
    }

    public ListenableFuture<Void> processEdgeNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        return processEntityNotification(tenantId, edgeNotificationMsg);
    }
}
