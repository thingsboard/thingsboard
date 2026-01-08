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
package org.thingsboard.server.service.edge.rpc.processor.oauth2;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.domain.DomainInfo;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.dao.domain.DomainService;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;
import org.thingsboard.server.gen.edge.v1.OAuth2ClientUpdateMsg;
import org.thingsboard.server.gen.edge.v1.OAuth2DomainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.EdgeMsgConstructorUtils;
import org.thingsboard.server.service.edge.rpc.processor.BaseEdgeProcessor;

@Slf4j
@Component
@TbCoreComponent
public class DomainEdgeProcessor extends BaseEdgeProcessor {

    @Autowired
    private DomainService domainService;

    @Override
    public DownlinkMsg convertEdgeEventToDownlink(EdgeEvent edgeEvent, EdgeVersion edgeVersion) {
        DomainId domainId = new DomainId(edgeEvent.getEntityId());
        switch (edgeEvent.getAction()) {
            case ADDED, UPDATED -> {
                DomainInfo domainInfo = domainService.findDomainInfoById(edgeEvent.getTenantId(), domainId);
                if (domainInfo != null && domainInfo.isPropagateToEdge()) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    OAuth2DomainUpdateMsg oAuth2DomainUpdateMsg = EdgeMsgConstructorUtils.constructOAuth2DomainUpdateMsg(msgType, domainInfo);
                    DownlinkMsg.Builder builder = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addOAuth2DomainUpdateMsg(oAuth2DomainUpdateMsg);
                    domainInfo.getOauth2ClientInfos().forEach(clientInfo -> {
                        OAuth2Client oauth2Client = edgeCtx.getOAuth2ClientService().findOAuth2ClientById(edgeEvent.getTenantId(), clientInfo.getId());
                        OAuth2ClientUpdateMsg oAuth2ClientUpdateMsg = EdgeMsgConstructorUtils.constructOAuth2ClientUpdateMsg(msgType, oauth2Client);
                        builder.addOAuth2ClientUpdateMsg(oAuth2ClientUpdateMsg);
                    });
                    return builder.build();
                }
            }
            case DELETED -> {
                OAuth2DomainUpdateMsg oAuth2DomainUpdateMsg = EdgeMsgConstructorUtils.constructOAuth2DomainDeleteMsg(domainId);
                return DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addOAuth2DomainUpdateMsg(oAuth2DomainUpdateMsg)
                        .build();
            }
        }
        return null;
    }

    @Override
    public EdgeEventType getEdgeEventType() {
        return EdgeEventType.DOMAIN;
    }

}
