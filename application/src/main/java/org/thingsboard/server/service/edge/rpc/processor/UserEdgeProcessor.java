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
package org.thingsboard.server.service.edge.rpc.processor;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Component
@Slf4j
@TbCoreComponent
public class UserEdgeProcessor extends BaseEdgeProcessor {

    public DownlinkMsg convertUserEventToDownlink(EdgeEvent edgeEvent) {
        UserId userId = new UserId(edgeEvent.getEntityId());
        DownlinkMsg downlinkMsg = null;
        switch (edgeEvent.getAction()) {
            case ADDED:
            case UPDATED:
                User user = userService.findUserById(edgeEvent.getTenantId(), userId);
                if (user != null) {
                    UpdateMsgType msgType = getUpdateMsgType(edgeEvent.getAction());
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addUserUpdateMsg(userMsgConstructor.constructUserUpdatedMsg(msgType, user))
                            .build();
                }
                break;
            case DELETED:
                downlinkMsg = DownlinkMsg.newBuilder()
                        .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                        .addUserUpdateMsg(userMsgConstructor.constructUserDeleteMsg(userId))
                        .build();
                break;
            case CREDENTIALS_UPDATED:
                UserCredentials userCredentialsByUserId = userService.findUserCredentialsByUserId(edgeEvent.getTenantId(), userId);
                if (userCredentialsByUserId != null && userCredentialsByUserId.isEnabled()) {
                    UserCredentialsUpdateMsg userCredentialsUpdateMsg =
                            userMsgConstructor.constructUserCredentialsUpdatedMsg(userCredentialsByUserId);
                    downlinkMsg = DownlinkMsg.newBuilder()
                            .setDownlinkMsgId(EdgeUtils.nextPositiveInt())
                            .addUserCredentialsUpdateMsg(userCredentialsUpdateMsg)
                            .build();
                }
        }
        return downlinkMsg;
    }

    public ListenableFuture<Void> processUserNotification(TenantId tenantId, TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg) {
        return processEntityNotificationForAllEdges(tenantId, edgeNotificationMsg);
    }
}
