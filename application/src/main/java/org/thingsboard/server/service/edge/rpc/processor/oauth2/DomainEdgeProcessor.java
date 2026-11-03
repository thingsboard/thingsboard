// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
