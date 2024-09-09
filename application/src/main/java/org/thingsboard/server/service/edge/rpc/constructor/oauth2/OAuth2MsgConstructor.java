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
package org.thingsboard.server.service.edge.rpc.constructor.oauth2;

import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.domain.DomainInfo;
import org.thingsboard.server.common.data.id.DomainId;
import org.thingsboard.server.common.data.id.OAuth2ClientId;
import org.thingsboard.server.common.data.oauth2.OAuth2Client;
import org.thingsboard.server.gen.edge.v1.OAuth2ClientUpdateMsg;
import org.thingsboard.server.gen.edge.v1.OAuth2DomainUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Component
@TbCoreComponent
public class OAuth2MsgConstructor {

    public OAuth2ClientUpdateMsg constructOAuth2ClientUpdateMsg(UpdateMsgType msgType, OAuth2Client oAuth2Client) {
        return OAuth2ClientUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(oAuth2Client))
                .setIdMSB(oAuth2Client.getId().getId().getMostSignificantBits())
                .setIdLSB(oAuth2Client.getId().getId().getLeastSignificantBits()).build();
    }

    public OAuth2ClientUpdateMsg constructOAuth2ClientDeleteMsg(OAuth2ClientId oAuth2ClientId) {
        return OAuth2ClientUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(oAuth2ClientId.getId().getMostSignificantBits())
                .setIdLSB(oAuth2ClientId.getId().getLeastSignificantBits()).build();
    }

    public OAuth2DomainUpdateMsg constructOAuth2DomainUpdateMsg(UpdateMsgType msgType, DomainInfo domainInfo) {
        return OAuth2DomainUpdateMsg.newBuilder().setMsgType(msgType).setEntity(JacksonUtil.toString(domainInfo))
                .setIdMSB(domainInfo.getId().getId().getMostSignificantBits())
                .setIdLSB(domainInfo.getId().getId().getLeastSignificantBits()).build();
    }

    public OAuth2DomainUpdateMsg constructOAuth2DomainDeleteMsg(DomainId domainId) {
        return OAuth2DomainUpdateMsg.newBuilder()
                .setMsgType(UpdateMsgType.ENTITY_DELETED_RPC_MESSAGE)
                .setIdMSB(domainId.getId().getMostSignificantBits())
                .setIdLSB(domainId.getId().getLeastSignificantBits())
                .build();
    }

}
