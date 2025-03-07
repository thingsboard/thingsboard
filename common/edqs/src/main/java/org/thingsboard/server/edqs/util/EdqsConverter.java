/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.edqs.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.edqs.AttributeKv;
import org.thingsboard.server.common.data.edqs.DataPoint;
import org.thingsboard.server.common.data.edqs.EdqsObject;
import org.thingsboard.server.common.data.edqs.Entity;
import org.thingsboard.server.common.data.edqs.LatestTsKv;
import org.thingsboard.server.common.data.edqs.fields.FieldsUtil;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.edqs.data.dp.BoolDataPoint;
import org.thingsboard.server.edqs.data.dp.CompressedJsonDataPoint;
import org.thingsboard.server.edqs.data.dp.CompressedStringDataPoint;
import org.thingsboard.server.edqs.data.dp.DoubleDataPoint;
import org.thingsboard.server.edqs.data.dp.JsonDataPoint;
import org.thingsboard.server.edqs.data.dp.LongDataPoint;
import org.thingsboard.server.edqs.data.dp.StringDataPoint;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.DataPointProto;
import org.xerial.snappy.Snappy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class EdqsConverter {

    private final Map<ObjectType, Converter<? extends EdqsObject>> converters = new HashMap<>();
    private final Converter<Entity> defaultConverter = new JsonConverter<>(Entity.class);

    {
        converters.put(ObjectType.RELATION, new JsonConverter<>(EntityRelation.class));
        converters.put(ObjectType.ATTRIBUTE_KV, new Converter<AttributeKv>() {
            @Override
            public byte[] serialize(ObjectType type, AttributeKv attributeKv) {
                var proto = TransportProtos.AttributeKvProto.newBuilder()
                        .setEntityIdMSB(attributeKv.getEntityId().getId().getMostSignificantBits())
                        .setEntityIdLSB(attributeKv.getEntityId().getId().getLeastSignificantBits())
                        .setEntityType(ProtoUtils.toProto(attributeKv.getEntityId().getEntityType()))
                        .setScope(TransportProtos.AttributeScopeProto.forNumber(attributeKv.getScope().ordinal()))
                        .setKey(attributeKv.getKey())
                        .setVersion(attributeKv.getVersion());
                if (attributeKv.getLastUpdateTs() != null && attributeKv.getValue() != null) {
                    proto.setDataPoint(toDataPointProto(attributeKv.getLastUpdateTs(), attributeKv.getValue()));
                }
                return proto.build().toByteArray();
            }

            @Override
            public AttributeKv deserialize(ObjectType type, byte[] bytes) throws Exception {
                TransportProtos.AttributeKvProto proto = TransportProtos.AttributeKvProto.parseFrom(bytes);
                EntityId entityId = EntityIdFactory.getByTypeAndUuid(ProtoUtils.fromProto(proto.getEntityType()),
                        new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));
                AttributeScope scope = AttributeScope.values()[proto.getScope().getNumber()];
                DataPoint dataPoint = proto.hasDataPoint() ? fromDataPointProto(proto.getDataPoint()) : null;
                return AttributeKv.builder()
                        .entityId(entityId)
                        .scope(scope)
                        .key(proto.getKey())
                        .version(proto.getVersion())
                        .dataPoint(dataPoint)
                        .build();
            }
        });
        converters.put(ObjectType.LATEST_TS_KV, new Converter<LatestTsKv>() {
            @Override
            public byte[] serialize(ObjectType type, LatestTsKv latestTsKv) {
                var proto = TransportProtos.LatestTsKvProto.newBuilder()
                        .setEntityIdMSB(latestTsKv.getEntityId().getId().getMostSignificantBits())
                        .setEntityIdLSB(latestTsKv.getEntityId().getId().getLeastSignificantBits())
                        .setEntityType(ProtoUtils.toProto(latestTsKv.getEntityId().getEntityType()))
                        .setKey(latestTsKv.getKey())
                        .setVersion(latestTsKv.getVersion());
                if (latestTsKv.getTs() != null && latestTsKv.getValue() != null) {
                    proto.setDataPoint(toDataPointProto(latestTsKv.getTs(), latestTsKv.getValue()));
                }
                return proto.build().toByteArray();
            }

            @Override
            public LatestTsKv deserialize(ObjectType type, byte[] bytes) throws Exception {
                TransportProtos.LatestTsKvProto proto = TransportProtos.LatestTsKvProto.parseFrom(bytes);
                EntityId entityId = EntityIdFactory.getByTypeAndUuid(ProtoUtils.fromProto(proto.getEntityType()),
                        new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));
                DataPoint dataPoint = proto.hasDataPoint() ? fromDataPointProto(proto.getDataPoint()) : null;
                return LatestTsKv.builder()
                        .entityId(entityId)
                        .key(proto.getKey())
                        .version(proto.getVersion())
                        .dataPoint(dataPoint)
                        .build();
            }
        });
    }

    public static DataPointProto toDataPointProto(long ts, KvEntry kvEntry) {
        DataPointProto.Builder proto = DataPointProto.newBuilder();
        proto.setTs(ts);
        switch (kvEntry.getDataType()) {
            case BOOLEAN -> proto.setBoolV(kvEntry.getBooleanValue().get());
            case LONG -> proto.setLongV(kvEntry.getLongValue().get());
            case DOUBLE -> proto.setDoubleV(kvEntry.getDoubleValue().get());
            case STRING -> {
                String strValue = kvEntry.getStrValue().get();
                if (strValue.length() < CompressedStringDataPoint.MIN_STR_SIZE_TO_COMPRESS) {
                    proto.setStringV(strValue);
                } else {
                    proto.setCompressedStringV(ByteString.copyFrom(compress(strValue)));
                }
            }
            case JSON -> {
                String jsonValue = kvEntry.getJsonValue().get();
                if (jsonValue.length() < CompressedStringDataPoint.MIN_STR_SIZE_TO_COMPRESS) {
                    proto.setJsonV(jsonValue);
                } else {
                    proto.setCompressedJsonV(ByteString.copyFrom(compress(jsonValue)));
                }
            }
        }
        return proto.build();
    }

    public static DataPoint fromDataPointProto(DataPointProto proto) {
        long ts = proto.getTs();
        if (proto.hasBoolV()) {
            return new BoolDataPoint(ts, proto.getBoolV());
        } else if (proto.hasLongV()) {
            return new LongDataPoint(ts, proto.getLongV());
        } else if (proto.hasDoubleV()) {
            return new DoubleDataPoint(ts, proto.getDoubleV());
        } else if (proto.hasStringV()) {
            return new StringDataPoint(ts, proto.getStringV());
        } else if (proto.hasCompressedStringV()) {
            return new CompressedStringDataPoint(ts, proto.getCompressedStringV().toByteArray());
        } else if (proto.hasJsonV()) {
            return new JsonDataPoint(ts, proto.getJsonV());
        } else if (proto.hasCompressedJsonV()) {
            return new CompressedJsonDataPoint(ts, proto.getCompressedJsonV().toByteArray());
        } else {
            throw new IllegalArgumentException("Unsupported data point proto: " + proto);
        }
    }

    @SneakyThrows
    private static byte[] compress(String value) {
        byte[] compressed = Snappy.compress(value);
        // TODO: limit the size
        log.debug("Compressed {} bytes to {} bytes", value.length(), compressed.length);
        return compressed;
    }

    public static Entity toEntity(EntityType entityType, Object entity) {
        Entity edqsEntity = new Entity();
        edqsEntity.setType(entityType);
        edqsEntity.setFields(FieldsUtil.toFields(entity));
        return edqsEntity;
    }

    public EdqsObject check(ObjectType type, Object object) {
        if (object instanceof EdqsObject edqsObject) {
            return edqsObject;
        } else {
            return toEntity(type.toEntityType(), object);
        }
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public <T extends EdqsObject> byte[] serialize(ObjectType type, T value) {
        Converter<T> converter = (Converter<T>) converters.get(type);
        if (converter != null) {
            return converter.serialize(type, value);
        } else {
            return defaultConverter.serialize(type, (Entity) value);
        }
    }

    @SneakyThrows
    public EdqsObject deserialize(ObjectType type, byte[] bytes) {
        Converter<? extends EdqsObject> converter = converters.get(type);
        if (converter != null) {
            return converter.deserialize(type, bytes);
        } else {
            return defaultConverter.deserialize(type, bytes);
        }
    }

    @RequiredArgsConstructor
    private static class JsonConverter<T> implements Converter<T> {

        private static final ObjectMapper mapper = JsonMapper.builder()
                .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .visibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
                .visibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE)
                .build();

        private final Class<T> type;

        @SneakyThrows
        @Override
        public byte[] serialize(ObjectType objectType, T value) {
            return mapper.writeValueAsBytes(value);
        }

        @SneakyThrows
        @Override
        public T deserialize(ObjectType objectType, byte[] bytes) {
            return mapper.readValue(bytes, this.type);
        }

    }

    private interface Converter<T> {

        byte[] serialize(ObjectType type, T value) throws Exception;

        T deserialize(ObjectType type, byte[] bytes) throws Exception;

    }

}
