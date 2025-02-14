/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.edqs.processor;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.edqs.AttributeKv;
import org.thingsboard.server.common.data.edqs.EdqsObject;
import org.thingsboard.server.common.data.edqs.Entity;
import org.thingsboard.server.common.data.edqs.LatestTsKv;
import org.thingsboard.server.common.data.edqs.fields.FieldsUtil;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.util.KvProtoUtil;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class EdqsConverter {

    private final Map<ObjectType, Converter<? extends EdqsObject>> converters = new HashMap<>();
    private final Converter<Entity> defaultConverter = new JsonConverter<>(Entity.class);

    {
        converters.put(ObjectType.RELATION, new JsonConverter<>(EntityRelation.class));
        converters.put(ObjectType.ATTRIBUTE_KV, new Converter<AttributeKv>() {
            @Override
            public byte[] serialize(ObjectType type, AttributeKv attributeKv) {
                // TODO: some attributes may not fit into kafka
                var proto = TransportProtos.AttributeKvProto.newBuilder()
                        .setEntityIdMSB(attributeKv.getEntityId().getId().getMostSignificantBits())
                        .setEntityIdLSB(attributeKv.getEntityId().getId().getLeastSignificantBits())
                        .setEntityType(ProtoUtils.toProto(attributeKv.getEntityId().getEntityType()))
                        .setScope(TransportProtos.AttributeScopeProto.forNumber(attributeKv.getScope().ordinal()))
                        .setKey(attributeKv.getKey())
                        .setVersion(attributeKv.getVersion());
                if (attributeKv.getLastUpdateTs() != null) {
                    proto.setLastUpdateTs(attributeKv.getLastUpdateTs());
                }
                if (attributeKv.getValue() != null) {
                    proto.setValue(KvProtoUtil.toKeyValueTypeProto(attributeKv.getValue()));
                }
                return proto.build().toByteArray();
            }

            @Override
            public AttributeKv deserialize(ObjectType type, byte[] bytes) throws Exception {
                TransportProtos.AttributeKvProto proto = TransportProtos.AttributeKvProto.parseFrom(bytes);
                EntityId entityId = EntityIdFactory.getByTypeAndUuid(ProtoUtils.fromProto(proto.getEntityType()),
                        new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));
                AttributeScope scope = AttributeScope.values()[proto.getScope().getNumber()];
                KvEntry value = proto.hasValue() ? KvProtoUtil.fromTsKvProto(proto.getValue()) : null;
                return AttributeKv.builder()
                        .entityId(entityId)
                        .scope(scope)
                        .key(proto.getKey())
                        .version(proto.getVersion())
                        .lastUpdateTs(proto.getLastUpdateTs())
                        .value(value)
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
                if (latestTsKv.getTs() != null) {
                    proto.setTs(latestTsKv.getTs());
                }
                if (latestTsKv.getValue() != null) {
                    proto.setValue(KvProtoUtil.toKeyValueTypeProto(latestTsKv.getValue()));
                }
                return proto.build().toByteArray();
            }

            @Override
            public LatestTsKv deserialize(ObjectType type, byte[] bytes) throws Exception {
                TransportProtos.LatestTsKvProto proto = TransportProtos.LatestTsKvProto.parseFrom(bytes);
                EntityId entityId = EntityIdFactory.getByTypeAndUuid(ProtoUtils.fromProto(proto.getEntityType()),
                        new UUID(proto.getEntityIdMSB(), proto.getEntityIdLSB()));
                KvEntry value = proto.hasValue() ? KvProtoUtil.fromTsKvProto(proto.getValue()) : null;
                return LatestTsKv.builder()
                        .entityId(entityId)
                        .key(proto.getKey())
                        .ts(proto.getTs())
                        .version(proto.getVersion())
                        .value(value)
                        .build();
            }
        });
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

        private final Class<T> type;

        @Override
        public byte[] serialize(ObjectType objectType, T value) {
            return JacksonUtil.writeValueAsBytes(value);
        }

        @Override
        public T deserialize(ObjectType objectType, byte[] bytes) {
            return JacksonUtil.fromBytes(bytes, this.type);
        }

    }

    private interface Converter<T> {

        byte[] serialize(ObjectType type, T value) throws Exception;

        T deserialize(ObjectType type, byte[] bytes) throws Exception;

    }

}
