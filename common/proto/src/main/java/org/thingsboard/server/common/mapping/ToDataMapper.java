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
import org.mapstruct.CollectionMappingStrategy;
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
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
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

import java.util.UUID;

@Mapper(collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        typeConversionPolicy = ReportingPolicy.ERROR
)
public interface ToDataMapper {

    ToDataMapper INSTANCE = Mappers.getMapper(ToDataMapper.class);

    Device mapDevice(DeviceProto proto);

    DeviceProfile mapDeviceProfile(DeviceProfileProto proto);

    Tenant mapTenant(TenantProto proto);

    TenantProfile mapTenantProfile(TenantProfileProto proto);

    ApiUsageState mapApiUsageState(ApiUsageStateProto proto);

    ComponentLifecycleMsg mapComponentLifecycleMsg(ComponentLifecycleMsgProto proto);

    default DeviceId mapDeviceId(TbUUID id) {
        return new DeviceId(new UUID(id.getMsb(), id.getLsb()));
    }

    default DeviceProfileId mapDeviceProfileId(TbUUID id) {
        return new DeviceProfileId(new UUID(id.getMsb(), id.getLsb()));
    }

    default EntityId mapEntityIdProto(EntityIdProto id) {
        return EntityIdFactory.getByTypeAndUuid(id.getType(), new UUID(id.getMsb(), id.getLsb()));
    }

    default TenantProfileId mapTenantProfileId(TbUUID id) {
        return new TenantProfileId(new UUID(id.getMsb(), id.getLsb()));
    }

    default TenantId mapTenantId(TbUUID id) {
        return TenantId.fromUUID(new UUID(id.getMsb(), id.getLsb()));
    }

    default CustomerId mapCustomerId(TbUUID id) {
        return new CustomerId(new UUID(id.getMsb(), id.getLsb()));
    }

    default OtaPackageId mapOtaPackageId(TbUUID id) {
        return new OtaPackageId(new UUID(id.getMsb(), id.getLsb()));
    }

    default AssetProfileId mapAssetProfileId(TbUUID id) {
        return new AssetProfileId(new UUID(id.getMsb(), id.getLsb()));
    }

    default ApiUsageStateId mapApiUsageStateId(TbUUID id) {
        return new ApiUsageStateId(new UUID(id.getMsb(), id.getLsb()));
    }

    default JsonNode mapJsonNode(String json) {
        return JacksonUtil.toJsonNode(json);
    }

    default DeviceData mapDeviceData(String data) {
        return JacksonUtil.fromString(data, DeviceData.class);
    }

    default DeviceProfileData mapDeviceProfileData(String data) {
        return JacksonUtil.fromString(data, DeviceProfileData.class);
    }

    default TenantProfileData mapTenantProfileData(String data) {
        return JacksonUtil.fromString(data, TenantProfileData.class);
    }
}
