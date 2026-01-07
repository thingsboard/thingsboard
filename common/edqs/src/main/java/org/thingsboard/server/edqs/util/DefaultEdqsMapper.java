/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.TbStringPool;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.edqs.AttributeKv;
import org.thingsboard.server.common.data.edqs.DataPoint;
import org.thingsboard.server.common.data.edqs.EdqsObject;
import org.thingsboard.server.common.data.edqs.EdqsObjectKey;
import org.thingsboard.server.common.data.edqs.Entity;
import org.thingsboard.server.common.data.edqs.LatestTsKv;
import org.thingsboard.server.common.data.edqs.fields.FieldsUtil;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.stats.EdqsStatsService;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.edqs.data.dp.BoolDataPoint;
import org.thingsboard.server.edqs.data.dp.CompressedJsonDataPoint;
import org.thingsboard.server.edqs.data.dp.CompressedStringDataPoint;
import org.thingsboard.server.edqs.data.dp.DoubleDataPoint;
import org.thingsboard.server.edqs.data.dp.JsonDataPoint;
import org.thingsboard.server.edqs.data.dp.LongDataPoint;
import org.thingsboard.server.edqs.data.dp.StringDataPoint;
import org.thingsboard.server.edqs.repo.KeyDictionary;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.DataPointProto;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultEdqsMapper implements EdqsMapper {

    private final EdqsStatsService edqsStatsService;

    @Value("${queue.edqs.string_compression_length_threshold:512}")
    private int stringCompressionLengthThreshold;

    private final Map<ObjectType, Mapper<? extends EdqsObject>> mappers = new HashMap<>();
    private final Mapper<Entity> defaultMapper = new JsonMapper<>(Entity.class) {
        @Override
        public EdqsObjectKey getKey(Entity entity) {
            return new Entity.Key(entity.getFields().getId());
        }
    };

    {
        mappers.put(ObjectType.RELATION, new JsonMapper<>(EntityRelation.class) {
            @Override
            public EdqsObjectKey getKey(EntityRelation relation) {
                return new EntityRelation.Key(relation.getFrom().getId(), relation.getTo().getId(), relation.getTypeGroup(), relation.getType());
            }
        });
        mappers.put(ObjectType.ATTRIBUTE_KV, new Mapper<AttributeKv>() {
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
            public AttributeKv deserialize(ObjectType type, byte[] bytes, boolean onlyKey) throws Exception {
                TransportProtos.AttributeKvProto proto = TransportProtos.AttributeKvProto.parseFrom(bytes);
                EntityId entityId = EntityIdFactory.getByTypeAndUuid(ProtoUtils.fromProto(proto.getEntityType()),
                        new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));
                AttributeScope scope = AttributeScope.values()[proto.getScope().getNumber()];
                DataPoint dataPoint = onlyKey || !proto.hasDataPoint() ? null : fromDataPointProto(proto.getDataPoint());
                return AttributeKv.builder()
                        .entityId(entityId)
                        .scope(scope)
                        .key(proto.getKey())
                        .version(proto.getVersion())
                        .dataPoint(dataPoint)
                        .build();
            }

            @Override
            public EdqsObjectKey getKey(AttributeKv attributeKv) {
                return new AttributeKv.Key(attributeKv.getEntityId().getId(), attributeKv.getScope(), KeyDictionary.get(attributeKv.getKey()));
            }
        });
        mappers.put(ObjectType.LATEST_TS_KV, new Mapper<LatestTsKv>() {
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
            public LatestTsKv deserialize(ObjectType type, byte[] bytes, boolean onlyKey) throws Exception {
                TransportProtos.LatestTsKvProto proto = TransportProtos.LatestTsKvProto.parseFrom(bytes);
                EntityId entityId = EntityIdFactory.getByTypeAndUuid(ProtoUtils.fromProto(proto.getEntityType()),
                        new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));
                DataPoint dataPoint = onlyKey || !proto.hasDataPoint() ? null : fromDataPointProto(proto.getDataPoint());
                return LatestTsKv.builder()
                        .entityId(entityId)
                        .key(proto.getKey())
                        .version(proto.getVersion())
                        .dataPoint(dataPoint)
                        .build();
            }

            @Override
            public EdqsObjectKey getKey(LatestTsKv latestTsKv) {
                return new LatestTsKv.Key(latestTsKv.getEntityId().getId(), KeyDictionary.get(latestTsKv.getKey()));
            }
        });
    }

    public DataPointProto toDataPointProto(long ts, KvEntry kvEntry) {
        DataPointProto.Builder proto = DataPointProto.newBuilder();
        proto.setTs(ts);
        switch (kvEntry.getDataType()) {
            case BOOLEAN -> proto.setBoolV(kvEntry.getBooleanValue().get());
            case LONG -> proto.setLongV(kvEntry.getLongValue().get());
            case DOUBLE -> proto.setDoubleV(kvEntry.getDoubleValue().get());
            case STRING -> {
                String strValue = kvEntry.getStrValue().get();
                if (strValue.length() < stringCompressionLengthThreshold) {
                    proto.setStringV(strValue);
                } else {
                    proto.setCompressedStringV(ByteString.copyFrom(compress(strValue)));
                }
            }
            case JSON -> {
                String jsonValue = kvEntry.getJsonValue().get();
                if (jsonValue.length() < stringCompressionLengthThreshold) {
                    proto.setJsonV(jsonValue);
                } else {
                    proto.setCompressedJsonV(ByteString.copyFrom(compress(jsonValue)));
                }
            }
        }
        return proto.build();
    }

    public DataPoint fromDataPointProto(DataPointProto proto) {
        long ts = proto.getTs();
        if (proto.hasBoolV()) {
            return new BoolDataPoint(ts, proto.getBoolV());
        } else if (proto.hasLongV()) {
            return new LongDataPoint(ts, proto.getLongV());
        } else if (proto.hasDoubleV()) {
            return new DoubleDataPoint(ts, proto.getDoubleV());
        } else if (proto.hasStringV()) {
            String stringV = proto.getStringV();
            if (stringV.length() < stringCompressionLengthThreshold) {
                return new StringDataPoint(ts, stringV);
            } else {
                return new CompressedStringDataPoint(ts, compress(stringV), this::uncompress);
            }
        } else if (proto.hasCompressedStringV()) {
            return new CompressedStringDataPoint(ts, proto.getCompressedStringV().toByteArray(), this::uncompress);
        } else if (proto.hasJsonV()) {
            String jsonV = proto.getJsonV();
            if (jsonV.length() < stringCompressionLengthThreshold) {
                return new JsonDataPoint(ts, jsonV);
            } else {
                return new CompressedJsonDataPoint(ts, compress(jsonV), this::uncompress);
            }
        } else if (proto.hasCompressedJsonV()) {
            return new CompressedJsonDataPoint(ts, proto.getCompressedJsonV().toByteArray(), this::uncompress);
        } else {
            throw new IllegalArgumentException("Unsupported data point proto: " + proto);
        }
    }

    @SneakyThrows
    private byte[] compress(String value) {
        byte[] compressed = Snappy.compress(value, StandardCharsets.UTF_8);
        log.debug("Compressed {} chars to {} bytes", value.length(), compressed.length);
        edqsStatsService.reportStringCompressed();
        return compressed;
    }

    @SneakyThrows
    private String uncompress(byte[] compressed) {
        String value = Snappy.uncompressString(compressed, StandardCharsets.UTF_8);
        log.debug("Uncompressed {} bytes to {} chars", compressed.length, value.length());
        edqsStatsService.reportStringUncompressed();
        return value;
    }

    public static Entity toEntity(EntityType entityType, Object entity) {
        Entity edqsEntity = new Entity();
        edqsEntity.setType(entityType);
        edqsEntity.setFields(FieldsUtil.toFields(entity));
        return edqsEntity;
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public <T extends EdqsObject> byte[] serialize(T value) {
        ObjectType type = value.type();
        Mapper<T> mapper = (Mapper<T>) mappers.getOrDefault(type, defaultMapper);
        return mapper.serialize(type, value);
    }

    @SneakyThrows
    public EdqsObject deserialize(ObjectType type, byte[] bytes, boolean onlyKey) {
        Mapper<? extends EdqsObject> mapper = mappers.getOrDefault(type, defaultMapper);
        return mapper.deserialize(type, bytes, onlyKey);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public <T extends EdqsObject> EdqsObjectKey getKey(T object) {
        Mapper<T> mapper = (Mapper<T>) mappers.getOrDefault(object.type(), defaultMapper);
        return mapper.getKey(object);
    }

    @RequiredArgsConstructor
    private static abstract class JsonMapper<T> implements Mapper<T> {

        private static final SimpleModule module = new SimpleModule();
        private static final ObjectMapper mapper = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .visibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                .visibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
                .visibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE)
                .build();

        static {
            module.addDeserializer(String.class, new InterningStringDeserializer());
            mapper.registerModule(module);
        }

        private final Class<T> type;

        @SneakyThrows
        @Override
        public byte[] serialize(ObjectType objectType, T value) {
            return mapper.writeValueAsBytes(value);
        }

        @SneakyThrows
        @Override
        public T deserialize(ObjectType objectType, byte[] bytes, boolean onlyKey) {
            return mapper.readValue(bytes, this.type);
        }

    }

    private interface Mapper<T> {

        byte[] serialize(ObjectType type, T value) throws Exception;

        T deserialize(ObjectType type, byte[] bytes, boolean onlyKey) throws Exception;

        EdqsObjectKey getKey(T object);

    }

    public static class InterningStringDeserializer extends StdDeserializer<String> {

        public InterningStringDeserializer() {
            super(String.class);
        }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            return TbStringPool.intern(p.getText());
        }

    }

}
