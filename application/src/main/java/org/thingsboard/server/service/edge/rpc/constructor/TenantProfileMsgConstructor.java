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
package org.thingsboard.server.service.edge.rpc.constructor;

import com.google.protobuf.ByteString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.gen.edge.v1.TenantProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.DataDecodingEncodingService;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Component
@TbCoreComponent
public class TenantProfileMsgConstructor {

    @Autowired
    private DataDecodingEncodingService dataDecodingEncodingService;

    public TenantProfileUpdateMsg constructTenantProfileUpdateMsg(UpdateMsgType msgType, TenantProfile tenantProfile) {
        TenantProfileUpdateMsg.Builder builder = TenantProfileUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(tenantProfile.getId().getId().getMostSignificantBits())
                .setIdLSB(tenantProfile.getId().getId().getLeastSignificantBits())
                .setName(tenantProfile.getName())
                .setDefault(tenantProfile.isDefault())
                .setIsolatedRuleChain(tenantProfile.isIsolatedTbRuleEngine())
                .setProfileDataBytes(ByteString.copyFrom(dataDecodingEncodingService.encode(tenantProfile.getProfileData())));
        if (tenantProfile.getDescription() != null) {
            builder.setDescription(tenantProfile.getDescription());
        }
        return builder.build();
    }
}
