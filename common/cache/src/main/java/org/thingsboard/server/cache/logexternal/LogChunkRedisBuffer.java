// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.cache.logexternal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.RedisClusterConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.logexternal.LogChunk;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Slf4j
@ConditionalOnProperty(prefix = "cache", value = "type", havingValue = "redis")
@Service("LogChunkBuffer")
public class LogChunkRedisBuffer implements LogChunkBuffer {

    private static final String KEY_PREFIX = "logChunks";
    private static final String SEQ_KEY_PREFIX = "logChunksSeq";

    private final RedisConnectionFactory connectionFactory;
    private final long ttlSeconds;
    private final int maxChunksPerUnit;

    public LogChunkRedisBuffer(RedisConnectionFactory connectionFactory,
                               @Value("${agents.logStream.bufferTtlSeconds:900}") long ttlSeconds,
                               @Value("${agents.logStream.bufferMaxChunksPerUnit:10}") int maxChunksPerUnit) {
        this.connectionFactory = connectionFactory;
        this.ttlSeconds = ttlSeconds;
        this.maxChunksPerUnit = maxChunksPerUnit;
    }

    /**
     * Best-effort append: only the incr is awaited (its reply is the assigned seq), the zAdd, trim and expires go out
     * in a single pipeline where the connection supports one - a Jedis cluster connection does not, so there they stay
     * sequential. Nothing is wrapped in a transaction, so a mid-sequence connection failure may leave the buffer
     * partially updated (e.g. seq advanced without a matching member).
     */
    @Override
    public long append(TenantId tenantId, EntityId unitId, LogChunk chunk) {
        byte[] key = unitKey(KEY_PREFIX, tenantId, unitId);
        byte[] seqKey = unitKey(SEQ_KEY_PREFIX, tenantId, unitId);
        try (RedisConnection connection = connectionFactory.getConnection()) {
            Long incremented = connection.stringCommands().incr(seqKey);
            long seq = incremented != null ? incremented : 0L;
            chunk.setSeq(seq);
            byte[] member = encodeMember(seq, chunk);
            if (connection instanceof RedisClusterConnection) {
                writeChunk(connection, key, seqKey, seq, member);
            } else {
                connection.openPipeline();
                try {
                    writeChunk(connection, key, seqKey, seq, member);
                } finally {
                    connection.closePipeline();
                }
            }
            return seq;
        }
    }

    private void writeChunk(RedisConnection connection, byte[] key, byte[] seqKey, long seq, byte[] member) {
        connection.zSetCommands().zAdd(key, seq, member);
        connection.zSetCommands().zRemRange(key, 0, -(maxChunksPerUnit + 1L));
        connection.keyCommands().expire(key, ttlSeconds);
        connection.keyCommands().expire(seqKey, ttlSeconds);
    }

    @Override
    public List<LogChunk> range(TenantId tenantId, EntityId unitId, long fromSeqInclusive, long toSeqInclusive) {
        if (toSeqInclusive < fromSeqInclusive) {
            return List.of();
        }
        byte[] key = unitKey(KEY_PREFIX, tenantId, unitId);
        try (RedisConnection connection = connectionFactory.getConnection()) {
            return readRange(connection, key, fromSeqInclusive, toSeqInclusive);
        }
    }

    @Override
    public long latestTailSeq(TenantId tenantId, EntityId unitId) {
        byte[] key = unitKey(KEY_PREFIX, tenantId, unitId);
        try (RedisConnection connection = connectionFactory.getConnection()) {
            Set<byte[]> last = connection.zSetCommands().zRevRange(key, 0, 0);
            if (last == null || last.isEmpty()) {
                return 0;
            }
            byte[] member = last.iterator().next();
            return decodeSeq(member);
        }
    }

    @Override
    public long latestTailLineTs(TenantId tenantId, EntityId unitId) {
        byte[] key = unitKey(KEY_PREFIX, tenantId, unitId);
        try (RedisConnection connection = connectionFactory.getConnection()) {
            Set<byte[]> last = connection.zSetCommands().zRevRange(key, 0, 0);
            if (last == null || last.isEmpty()) {
                return 0;
            }
            LogChunk chunk = decodeMember(last.iterator().next());
            return chunk == null ? 0 : chunk.getLastLineTs();
        }
    }

    @Override
    public void deleteUnit(TenantId tenantId, EntityId unitId) {
        byte[] key = unitKey(KEY_PREFIX, tenantId, unitId);
        byte[] seqKey = unitKey(SEQ_KEY_PREFIX, tenantId, unitId);
        try (RedisConnection connection = connectionFactory.getConnection()) {
            connection.keyCommands().del(key, seqKey);
        }
    }

    private static List<LogChunk> readRange(RedisConnection connection, byte[] key, double minScore, double maxScore) {
        Set<byte[]> members = connection.zSetCommands().zRangeByScore(key, minScore, maxScore);
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        List<LogChunk> result = new ArrayList<>(members.size());
        for (byte[] m : members) {
            LogChunk decoded = decodeMember(m);
            if (decoded != null) {
                result.add(decoded);
            }
        }
        return result;
    }

    private static long decodeSeq(byte[] member) {
        if (member == null || member.length < Long.BYTES) {
            return 0;
        }
        return ByteBuffer.wrap(member, 0, Long.BYTES).getLong();
    }

    private static byte[] unitKey(String prefix, TenantId tenantId, EntityId unitId) {
        return (prefix + ":tenant:" + tenantId.getId() + ":unit:" + unitId.getId())
                .getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] encodeMember(long seq, LogChunk chunk) {
        byte[] payload = JacksonUtil.writeValueAsBytes(chunk);
        return ByteBuffer.allocate(Long.BYTES + payload.length).putLong(seq).put(payload).array();
    }

    private static LogChunk decodeMember(byte[] member) {
        if (member == null || member.length <= Long.BYTES) {
            return null;
        }
        byte[] payload = Arrays.copyOfRange(member, Long.BYTES, member.length);
        try {
            LogChunk chunk = JacksonUtil.IGNORE_UNKNOWN_PROPERTIES_JSON_MAPPER.readValue(payload, LogChunk.class);
            if (chunk != null) {
                chunk.setSeq(decodeSeq(member));
            }
            return chunk;
        } catch (Exception e) {
            log.warn("Failed to deserialize log chunk", e);
            return null;
        }
    }

}
