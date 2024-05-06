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
package org.thingsboard.server.service.edge.rpc.processor.oauth2;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.oauth2.OAuth2Info;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.OAuth2UpdateMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
@Component
@TbCoreComponent
public class OAuth2EdgeProcessor extends BaseEdgeProcessor {

    public DownlinkMsg convertOAuth2EventToDownlink(EdgeEvent edgeEvent) {
        DownlinkMsg downlinkMsg = null;
        OAuth2Info oAuth2Info = JacksonUtil.convertValue(edgeEvent.getBody(), OAuth2Info.class);
        if (oAuth2Info != null) {
            OAuth2UpdateMsg oAuth2UpdateMsg = oAuth2MsgConstructor.constructOAuth2UpdateMsg(oAuth2Info);
            downlinkMsg = DownlinkMsg.newBuilder()
                    .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                    .addOAuth2UpdateMsg(oAuth2UpdateMsg)
                    .build();
        }
        return downlinkMsg;
    }

    public ListenableFuture<Void> processOAuth2Notification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        OAuth2Info oAuth2Info = JacksonUtil.fromString(edgeNotificationMsg.getBody(), OAuth2Info.class);
        if (oAuth2Info == null) {
            return Futures.immediateFuture(null);
        }
        EdgeEventType type = EdgeEventType.valueOf(edgeNotificationMsg.getType());
        EdgeEventActionType actionType = EdgeEventActionType.valueOf(edgeNotificationMsg.getAction());
        return processActionForAllEdges(tenantId, type, actionType, null, JacksonUtil.toJsonNode(edgeNotificationMsg.getBody()), null);
    }

}
