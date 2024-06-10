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
package org.thingsboard.server.dao.attributes;

import com.google.protobuf.InvalidProtocolBufferException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;
import org.thingsboard.server.cache.CacheSpecsMap;
import org.thingsboard.server.cache.RedisTbTransactionalCache;
import org.thingsboard.server.cache.TBRedisCacheConfiguration;
import org.thingsboard.server.cache.TbRedisSerializer;
import org.thingsboard.server.common.data.CacheConstants;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.dao.dictionary.KeyDictionaryDao;

import java.nio.charset.StandardCharsets;

@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("AttributeCache")
public class AttributeRedisCache extends RedisTbTransactionalCache<AttributeCacheKey, AttributeKvEntry> {

    public AttributeRedisCache(TBRedisCacheConfiguration configuration, CacheSpecsMap cacheSpecsMap, RedisConnectionFactory connectionFactory, KeyDictionaryDao keyDictionaryDao) {
        super(CacheConstants.ATTRIBUTES_CACHE, cacheSpecsMap, connectionFactory, configuration, new TbRedisSerializer<>() {

            static final int TS_LENGTH = 13;

            @Override
            public byte[] serialize(AttributeKvEntry attributeKvEntry) throws SerializationException {
                //Timestamp, DataType, HasV, StringValue
                StringBuilder sb = new StringBuilder(toFixedLength(attributeKvEntry.getLastUpdateTs()));
                sb
                        .append(":")
                        .append(attributeKvEntry.getDataType().getProtoNumber())
                        .append(":");
                switch (attributeKvEntry.getDataType()) {
                    case BOOLEAN:
                        if (attributeKvEntry.getBooleanValue().isPresent()) {
                            sb.append(1);
                            sb.append(":");
                            sb.append(attributeKvEntry.getBooleanValue().get());
                        } else {
                            sb.append(0);
                        }
                        break;
                    case STRING:
                        if (attributeKvEntry.getStrValue().isPresent()) {
                            sb.append(1);
                            sb.append(":");
                            sb.append(attributeKvEntry.getStrValue().get());
                        } else {
                            sb.append(0);
                        }
                    case DOUBLE:
                        if (attributeKvEntry.getDoubleValue().isPresent()) {
                            sb.append(1);
                            sb.append(":");
                            sb.append(attributeKvEntry.getDoubleValue().get());
                        } else {
                            sb.append(0);
                        }
                        break;
                    case LONG:
                        if (attributeKvEntry.getLongValue().isPresent()) {
                            sb.append(1);
                            sb.append(":");
                            sb.append(attributeKvEntry.getLongValue().get());
                        } else {
                            sb.append(0);
                        }
                        break;
                    case JSON:
                        if (attributeKvEntry.getJsonValue().isPresent()) {
                            sb.append(1);
                            sb.append(":");
                            sb.append(attributeKvEntry.getJsonValue().get());
                        } else {
                            sb.append(0);
                        }
                        break;

                }
                return sb.toString().getBytes(StandardCharsets.UTF_8);
            }

            private String toFixedLength(long timestamp) {
                if (timestamp < 0) {
                    throw new RuntimeException("timestamp serialization failed because it is negative " + timestamp);
                }
                String timestampString = Long.toString(timestamp);
                int originalLength = timestampString.length();
                if (TS_LENGTH == originalLength) {
                    return timestampString;
                } else if (TS_LENGTH > originalLength) {
                    return timestampString + " ".repeat(TS_LENGTH - originalLength);
                } else {
                    throw new RuntimeException("timestamp serialization broken " + timestamp + " " + timestampString + " " + TS_LENGTH);
                }
            }

            @Override
            public AttributeKvEntry deserialize(AttributeCacheKey key, byte[] bytes) throws SerializationException {
                try {
                    String proto = new String(bytes, StandardCharsets.UTF_8);
                    long ts = Long.parseLong(proto.substring(0, TS_LENGTH).trim());
                    DataType type = DataType.ofChar(proto.charAt(TS_LENGTH + 1));
                    boolean hasValue = proto.charAt(TS_LENGTH + 1 + 2) == '1';
                    String payload = proto.length() > TS_LENGTH + 5 ? proto.substring(TS_LENGTH + 5) : "";
                    String keyName = keyDictionaryDao.getKey(key.getKeyId());
                    KvEntry entry = switch (type) {
                        case BOOLEAN ->
                                new BooleanDataEntry(keyName, hasValue ? Boolean.parseBoolean(payload) : null);
                        case LONG -> new LongDataEntry(keyName, hasValue ? Long.parseLong(payload) : null);
                        case DOUBLE -> new DoubleDataEntry(keyName, hasValue ? Double.parseDouble(payload) : null);
                        case STRING -> new StringDataEntry(keyName, hasValue ? payload : null);
                        case JSON -> new JsonDataEntry(keyName, hasValue ? payload : null);
                        default -> throw new InvalidProtocolBufferException("Unrecognized type: " + type + " !");
                    };
                    return new BaseAttributeKvEntry(ts, entry);
                } catch (InvalidProtocolBufferException e) {
                    throw new SerializationException(e.getMessage());
                }
            }
        });
    }

}
