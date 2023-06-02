package org.thingsboard.server.common.mapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.ByteString;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.gen.data.DeviceProto;
import org.thingsboard.server.gen.data.TbUUID;

import java.nio.charset.StandardCharsets;

@Mapper(nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        unmappedSourcePolicy = ReportingPolicy.ERROR,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ToProtoMapper {

    ToProtoMapper INSTANCE = Mappers.getMapper(ToProtoMapper.class);

    @BeanMapping(ignoreUnmappedSourceProperties = {"uuidId", "deviceData"})
    DeviceProto map(Device device);

    default TbUUID map(UUIDBased id) {
        return TbUUID.newBuilder().setMsb(id.getId().getMostSignificantBits()).setLsb(id.getId().getLeastSignificantBits()).build();
    }

    default ByteString map(JsonNode additionalInfo) {
        return ByteString.copyFrom(JacksonUtil.toString(additionalInfo), StandardCharsets.UTF_8);
    }

}
