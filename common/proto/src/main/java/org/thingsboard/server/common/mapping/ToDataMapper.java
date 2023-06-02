package org.thingsboard.server.common.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.ByteString;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.data.DeviceProto;
import org.thingsboard.server.gen.data.TbUUID;

import java.util.UUID;

@Mapper(collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ToDataMapper {

    ToDataMapper INSTANCE = Mappers.getMapper(ToDataMapper.class);

    Device map(DeviceProto device);

    default DeviceId mapDeviceId(TbUUID id) {
        return new DeviceId(new UUID(id.getMsb(), id.getLsb()));
    }

    default DeviceProfileId mapDeviceProfileId(TbUUID id) {
        return new DeviceProfileId(new UUID(id.getMsb(), id.getLsb()));
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

    default JsonNode map(ByteString additionalInfo) {
        return JacksonUtil.fromBytes(additionalInfo.toByteArray());
    }

}
