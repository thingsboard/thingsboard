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

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.domain.DomainInfo;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.OAuth2ClientUpdateMsg;
import org.thingsboard.server.gen.edge.v1.OAuth2DomainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.utils.EdgeVersionUtils;

@Slf4j
@Component
@TbCoreComponent
public class OAuth2EdgeProcessor extends BaseEdgeProcessor {

    public DownlinkMsg convertOAuth2DomainEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        if (EdgeVersionUtils.isEdgeVersionOlderThan(edgeVersion, EdgeVersion.V_3_8_0)) {
            return null;
        }
        DomainId domainId = new DomainId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;

        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                DomainInfo domainInfo = domainService.findDomainInfoById(edgeEvent.getTenantId(), domainId);
                if (domainInfo != null && domainInfo.isPropagateToEdge()) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    OAuth2DomainUpdateMsg oAuth2DomainUpdateMsg = oAuth2MsgConstructor.constructOAuth2DomainUpdateMsg(msgType, domainInfo);
                    DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addOAuth2DomainUpdateMsg(oAuth2DomainUpdateMsg);
                    domainInfo.getOauth2ClientInfos().forEach(clientInfo -> {
                        OAuth2Client oauth2Client = oAuth2ClientService.findOAuth2ClientById(edgeEvent.getTenantId(), clientInfo.getId());
                        OAuth2ClientUpdateMsg oAuth2ClientUpdateMsg = oAuth2MsgConstructor.constructOAuth2ClientUpdateMsg(msgType, oauth2Client);
                        builder.addOAuth2ClientUpdateMsg(oAuth2ClientUpdateMsg);
                    });
                    downlinkMsg = builder.build();
                }
            }
            case DELETED -> {
                OAuth2DomainUpdateMsg oAuth2DomainUpdateMsg = oAuth2MsgConstructor.constructOAuth2DomainDeleteMsg(domainId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addOAuth2DomainUpdateMsg(oAuth2DomainUpdateMsg)
                        .build();
            }
        }
        return downlinkMsg;
    }

    public DownlinkMsg convertOAuth2ClientEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        if (EdgeVersionUtils.isEdgeVersionOlderThan(edgeVersion, EdgeVersion.V_3_8_0)) {
            return null;
        }
        OAuth2ClientId oAuth2ClientId = new OAuth2ClientId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;

        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                boolean isPropagateToEdge = oAuth2ClientService.isPropagateOAuth2ClientToEdge(edgeEvent.getTenantId(), oAuth2ClientId);
                if (!isPropagateToEdge) {
                    return null;
                }
                OAuth2Client oAuth2Client = oAuth2ClientService.findOAuth2ClientById(edgeEvent.getTenantId(), oAuth2ClientId);
                if (oAuth2Client != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    OAuth2ClientUpdateMsg oAuth2ClientUpdateMsg = oAuth2MsgConstructor.constructOAuth2ClientUpdateMsg(msgType, oAuth2Client);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addOAuth2ClientUpdateMsg(oAuth2ClientUpdateMsg)
                            .build();
                }
            }
            case DELETED -> {
                OAuth2ClientUpdateMsg oAuth2ClientDeleteMsg = oAuth2MsgConstructor.constructOAuth2ClientDeleteMsg(oAuth2ClientId);
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addOAuth2ClientUpdateMsg(oAuth2ClientDeleteMsg)
                        .build();
            }
        }
        return downlinkMsg;
    }

}
