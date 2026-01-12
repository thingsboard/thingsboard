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
package org.thingsboard.server.service.edge.rpc.processor.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.QueueId;
import org.thingsboard.server.common.data.queue.Queue;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.QueueUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
@Component
@TbCoreComponent
public class QueueEdgeProcessor extends BaseEdgeProcessor {

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        QueueId queueId = new QueueId(edgeEvent.getEntityId());
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                Queue queue = edgeCtx.getQueueService().findQueueById(edgeEvent.getTenantId(), queueId);
                if (queue != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    QueueUpdateMsg queueUpdateMsg = EdgeMsgConstructorUtils.constructQueueUpdatedMsg(msgType, queue);
                    return DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addQueueUpdateMsg(queueUpdateMsg)
                            .build();
                }
            }
            case DELETED -> {
                QueueUpdateMsg queueDeleteMsg = EdgeMsgConstructorUtils.constructQueueDeleteMsg(queueId);
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addQueueUpdateMsg(queueDeleteMsg)
                        .build();
            }
        }
        return null;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.QUEUE;
    }

}
