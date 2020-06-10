/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.service.edge.rpc.constructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.security.UserCredentials;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;
import org.thingsboard.server.gen.edge.UpdateMsgType;
import org.thingsboard.server.gen.edge.UserUpdateMsg;

@Component
@Slf4j
public class UserUpdateMsgConstructor {

    @Autowired
    private UserService userService;

    public UserUpdateMsg constructUserUpdatedMsg(UpdateMsgType msgType, User user) {
        UserUpdateMsg.Builder builder = UserUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(user.getId().getId().getMostSignificantBits())
                .setIdLSB(user.getId().getId().getLeastSignificantBits())
                .setEmail(user.getEmail())
                .setAuthority(user.getAuthority().name())
                .setEnabled(false);
        if (user.getFirstName() != null) {
            builder.setFirstName(user.getFirstName());
        }
        if (user.getLastName() != null) {
            builder.setLastName(user.getLastName());
        }
        if (user.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(user.getAdditionalInfo()));
        }
        if (user.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(user.getAdditionalInfo()));
        }
        if (msgType.equals(UpdateMsgType.ENTITY_CREATED_RPC_MESSAGE) ||
                msgType.equals(UpdateMsgType.ENTITY_UPDATED_RPC_MESSAGE)) {
            UserCredentials userCredentials = userService.findUserCredentialsByUserId(user.getTenantId(), user.getId());
            if (userCredentials != null) {
                builder.setEnabled(userCredentials.isEnabled()).setPassword(userCredentials.getPassword());
            }
        }
        return builder.build();
    }
}
