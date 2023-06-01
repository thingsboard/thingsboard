package org.thingsboard.server.queue.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.protobuf.ByteString;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.factory.Mappers;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.gen.data.DeviceProto;

import java.nio.charset.StandardCharsets;

@Mapper(collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface DeviceMapper {

    DeviceMapper INSTANCE = Mappers.getMapper(DeviceMapper.class);

    DeviceProto map(Device device);

    default String map(UUIDBased id) {
        return id.getId().toString();
    }

    default ByteString map(JsonNode additionalInfo) {
        return ByteString.copyFrom(JacksonUtil.toString(additionalInfo), StandardCharsets.UTF_8);
    }

//     Device map(DeviceProto proto);

}
