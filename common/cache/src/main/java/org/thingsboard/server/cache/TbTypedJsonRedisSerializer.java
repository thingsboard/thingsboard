package org.thingsboard.server.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.data.redis.serializer.SerializationException;
import org.thingsboard.common.util.JacksonUtil;

public class TbTypedJsonRedisSerializer<K, V> implements TbRedisSerializer<K, V> {

    private final TypeReference<V> valueTypeRef;

    public TbTypedJsonRedisSerializer(TypeReference<V> valueTypeRef) {
        this.valueTypeRef = valueTypeRef;
    }

    @Override
    public byte[] serialize(V v) throws SerializationException {
        return JacksonUtil.writeValueAsBytes(v);
    }

    @Override
    public V deserialize(K key, byte[] bytes) throws SerializationException {
        return JacksonUtil.fromBytes(bytes, valueTypeRef);
    }
}
