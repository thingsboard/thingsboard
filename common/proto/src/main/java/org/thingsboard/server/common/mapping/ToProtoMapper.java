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
package org.thingsboard.server.common.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.gen.data.ApiUsageStateProto;
import org.thingsboard.server.gen.data.ComponentLifecycleMsgProto;
import org.thingsboard.server.gen.data.DeviceProfileProto;
import org.thingsboard.server.gen.data.DeviceProto;
import org.thingsboard.server.gen.data.EntityIdProto;
import org.thingsboard.server.gen.data.TbUUID;
import org.thingsboard.server.gen.data.TenantProfileProto;
import org.thingsboard.server.gen.data.TenantProto;

@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        unmappedSourcePolicy = ReportingPolicy.ERROR,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        typeConversionPolicy = ReportingPolicy.ERROR)
public interface ToProtoMapper {

    ToProtoMapper INSTANCE = Mappers.getMapper(ToProtoMapper.class);

    @BeanMapping(ignoreUnmappedSourceProperties = {"uuidId"})
    DeviceProto mapDevice(Device msg);

    @BeanMapping(ignoreUnmappedSourceProperties = {"uuidId"})
    DeviceProfileProto mapDeviceProfile(DeviceProfile msg);

    @BeanMapping(ignoreUnmappedSourceProperties = {"uuidId", "tenantId", "name"})
    TenantProto mapTenant(Tenant msg);

    @BeanMapping(ignoreUnmappedSourceProperties = {"uuidId", "profileConfiguration", "defaultProfileConfiguration"})
    TenantProfileProto mapTenantProfile(TenantProfile msg);

    @BeanMapping(ignoreUnmappedSourceProperties = {"uuidId", "transportEnabled", "reExecEnabled", "dbStorageEnabled", "jsExecEnabled", "emailSendEnabled", "smsSendEnabled", "alarmCreationEnabled"})
    ApiUsageStateProto mapApiUsageState(ApiUsageState msg);

    @BeanMapping(ignoreUnmappedSourceProperties = {"ruleChainId", "msgType"})
    ComponentLifecycleMsgProto mapComponentLifecycleMsg(ComponentLifecycleMsg msg);

    default TbUUID mapUuid(UUIDBased id) {
        return TbUUID.newBuilder().setMsb(id.getId().getMostSignificantBits()).setLsb(id.getId().getLeastSignificantBits()).build();
    }

    default EntityIdProto mapEntityId(EntityId id) {
        return EntityIdProto.newBuilder().setType(id.getEntityType().name()).setMsb(id.getId().getMostSignificantBits()).setLsb(id.getId().getLeastSignificantBits()).build();
    }

    default String map(JsonNode additionalInfo) {
        return JacksonUtil.toString(additionalInfo);
    }

    default String map(DeviceData deviceData) {
        return JacksonUtil.toString(deviceData);
    }

    default String map(DeviceProfileData profileData) {
        return JacksonUtil.toString(profileData);
    }

    default String map(TenantProfileData profileData) {
        return JacksonUtil.toString(profileData);
    }

}
