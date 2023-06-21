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

import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.gen.edge.v1.TenantProfileUpdateMsg;
import org.thingsboard.server.gen.edge.v1.TenantUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UpdateMsgType;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Component
@TbCoreComponent
public class TenantMsgConstructor {

    public TenantUpdateMsg constructTenantUpdateMsg(UpdateMsgType msgType, Tenant tenant, TenantProfileUpdateMsg tenantProfileUpdateMsg) {
        TenantUpdateMsg.Builder builder = TenantUpdateMsg.newBuilder()
                .setMsgType(msgType)
                .setIdMSB(tenant.getId().getId().getMostSignificantBits())
                .setIdLSB(tenant.getId().getId().getLeastSignificantBits())
                .setTitle(tenant.getTitle())
                .setProfileIdMSB(tenant.getTenantProfileId().getId().getMostSignificantBits())
                .setProfileIdLSB(tenant.getTenantProfileId().getId().getLeastSignificantBits())
                .setRegion(tenant.getRegion())
                .setTenantProfileUpdateMsg(tenantProfileUpdateMsg);
        if (tenant.getCountry() != null) {
            builder.setCountry(tenant.getCountry());
        }
        if (tenant.getState() != null) {
            builder.setState(tenant.getState());
        }
        if (tenant.getCity() != null) {
            builder.setCity(tenant.getCity());
        }
        if (tenant.getAddress() != null) {
            builder.setAddress(tenant.getAddress());
        }
        if (tenant.getAddress2() != null) {
            builder.setAddress2(tenant.getAddress2());
        }
        if (tenant.getZip() != null) {
            builder.setZip(tenant.getZip());
        }
        if (tenant.getPhone() != null) {
            builder.setPhone(tenant.getPhone());
        }
        if (tenant.getEmail() != null) {
            builder.setEmail(tenant.getEmail());
        }
        if (tenant.getAdditionalInfo() != null) {
            builder.setAdditionalInfo(JacksonUtil.toString(tenant.getAdditionalInfo()));
        }
        return builder.build();
    }
}
