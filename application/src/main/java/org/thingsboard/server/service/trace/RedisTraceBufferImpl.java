package org.thingsboard.server.service.trace;

import com.google.common.hash.Hashing;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.BufferedSpan;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@ConditionalOnExpression(TraceConditions.SHARED_CACHE)
public class RedisTraceBufferImpl implements TraceBuffer {
    // Cluster hash tag shared by a partition's deadlines ZSET and its trace hashes, so scripts touch one slot.
    private static final String PARTITION_TAG = "trace-p";

    private static final int TRACE_PARTITION_COUNT = 64;
    private static final int TRACE_SCRIPT_NUM_KEYS = 1;

    private static final byte[] TRACE_LUA_SCRIPT = raw("""
            local deadlinesKey = KEYS[1]

            local maxSpans = tonumber(ARGV[1])
            local deadline = tonumber(ARGV[2])
            local tracePrefix = ARGV[3]
            local keyTtlMs = tonumber(ARGV[4])

            local truncatedTraces = 0
            local i = 5

            while i <= #ARGV do
                local member = ARGV[i]
                local spanPairs = tonumber(ARGV[i + 1])
                local traceKey = tracePrefix .. member

                local firstSpan = i + 2
                local lastSpan = firstSpan + spanPairs * 2 - 1
                i = lastSpan + 1

                if redis.call('HGET', traceKey, '_truncated') == 'true' then
                    truncatedTraces = truncatedTraces + 1
                else
                    local storedCount = redis.call('HGET', traceKey, '_span_count')
                    local isNewTrace = not storedCount
                    local spanCount = tonumber(storedCount or '0')

                    local maxReached = false

                    for j = firstSpan, lastSpan, 2 do
                        if spanCount >= maxSpans then
                            maxReached = true
                            break
                        end

                        spanCount = spanCount + redis.call(
                            'HSET',
                            traceKey,
                            ARGV[j],
                            ARGV[j + 1]
                        )
                    end

                    if spanCount >= maxSpans then
                        maxReached = true
                    end

                    redis.call(
                        'HSET',
                        traceKey,
                        '_span_count',
                        spanCount,
                        '_truncated',
                        maxReached and 'true' or 'false'
                    )

                    if isNewTrace and keyTtlMs > 0 then
                        redis.call('PEXPIRE', traceKey, keyTtlMs)
                    end

                    redis.call(
                        'ZADD',
                        deadlinesKey,
                        deadline,
                        member
                    )

                    if maxReached then
                        truncatedTraces = truncatedTraces + 1
                    end
                end
            end

            return truncatedTraces
            """);

    private static final byte[] TRACE_SCRIPT_SHA = raw(Hashing.sha1().hashBytes(TRACE_LUA_SCRIPT).toString());

    private static final byte[] DRAIN_LUA_SCRIPT = raw("""
            local deadlinesKey = KEYS[1]

            local now = tonumber(ARGV[1])
            local budget = tonumber(ARGV[2])
            local tracePrefix = ARGV[3]
            local scanLimit = tonumber(ARGV[4])

            local members = redis.call(
                'ZRANGEBYSCORE',
                deadlinesKey,
                '-inf',
                now,
                'LIMIT',
                0,
                scanLimit
            )

            if #members == 0 then
                return {}
            end

            local out = {}
            local claimed = {}
            local spans = 0

            for i = 1, #members do
                local member = members[i]
                local hash = redis.call('HGETALL', tracePrefix .. member)
                redis.call('DEL', tracePrefix .. member)

                claimed[#claimed + 1] = member
                out[#out + 1] = member
                out[#out + 1] = tostring(#hash)
                for j = 1, #hash do
                    out[#out + 1] = hash[j]
                end

                local count = 0
                for j = 1, #hash, 2 do
                    if hash[j] == '_span_count' then
                        count = tonumber(hash[j + 1]) or 0
                    end
                end
                if count == 0 then
                    count = #hash / 2
                end

                spans = spans + count
                if spans >= budget then
                    break
                end
            end

            redis.call('ZREM', deadlinesKey, unpack(claimed))

            return out
            """);

    private static final byte[] DRAIN_SCRIPT_SHA = raw(Hashing.sha1().hashBytes(DRAIN_LUA_SCRIPT).toString());
    private static final int DRAIN_SCRIPT_NUM_KEYS = 1;

    private static final int DRAIN_SCAN_LIMIT = 512;

    private final RedisConnectionFactory redisConnectionFactory;
    private final int maxSpansPerTrace;
    private final long traceTtlMs;
    private final long keyTtlMs;

    private final AtomicInteger drainCursor = new AtomicInteger();

    private final Timer redisExecTimer;
    private final DefaultCounter failedSpansCounter;
    private final DefaultCounter limitReachedCounter;

    public RedisTraceBufferImpl(RedisConnectionFactory redisConnectionFactory,
                                @Value("${otel.processor.trace.buffer.max-spans-per-trace:1000}") int maxSpansPerTrace,
                                @Value("${otel.processor.trace.buffer.trace-ttl-ms:60000}") long traceTtlMs,
                                @Value("${otel.processor.trace.buffer.redis-key-ttl-ms:1800000}") long keyTtlMs,
                                StatsFactory statsFactory) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.maxSpansPerTrace = maxSpansPerTrace;
        this.traceTtlMs = traceTtlMs;
        this.keyTtlMs = keyTtlMs;
        this.redisExecTimer = statsFactory.createTimer("trace.export.redis.duration");
        this.failedSpansCounter = statsFactory.createDefaultCounter("trace.export.spans", "result", "failed");
        this.limitReachedCounter = statsFactory.createDefaultCounter("trace.export.limit.reached");
    }

    @Override
    public void append(Collection<BufferedSpan> spans) {
        if (CollectionUtils.isEmpty(spans)) {
            return;
        }

        Map<Integer, Map<String, List<BufferedSpan>>> spansByPartition = new LinkedHashMap<>();
        for (BufferedSpan span : spans) {
            String traceIdentity = traceIdentity(span);
            spansByPartition
                    .computeIfAbsent(partitionOf(traceIdentity), partition -> new LinkedHashMap<>())
                    .computeIfAbsent(traceIdentity, identity -> new ArrayList<>())
                    .add(span);
        }
        long deadline = System.currentTimeMillis() + traceTtlMs;
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            spansByPartition.forEach((partition, tracesInPartition) -> {
                try {
                    byte[][] keysAndArgs = buildTraceScriptArgs(partition, deadline, tracesInPartition);
                    Long truncated = redisExecTimer.record(() -> executeTraceScript(connection, keysAndArgs));
                    if (truncated != null && truncated > 0L) {
                        limitReachedCounter.add(truncated.intValue());
                        log.debug("{} of {} traces in partition [{}] reached the span limit of {} and are truncated",
                                truncated, tracesInPartition.size(), partition, maxSpansPerTrace);
                    }
                } catch (Exception e) {
                    int lostSpans = tracesInPartition.values().stream().mapToInt(List::size).sum();
                    failedSpansCounter.add(lostSpans);
                    log.warn("Failed to persist {} traces ({} spans) in partition [{}]",
                            tracesInPartition.size(), lostSpans, partition, e);
                }
            });
        } catch (Exception e) {
            failedSpansCounter.add(spans.size());
            log.warn("Failed to create Redis trace hashes for span batch of size {}", spans.size(), e);
        }
    }

    @Override
    public List<LoadedTrace> drainExpired(long now, int maxSpans) {
        int budget = Math.max(1, maxSpans);
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            for (int attempt = 0; attempt < TRACE_PARTITION_COUNT; attempt++) {
                int partition = Math.floorMod(drainCursor.getAndIncrement(), TRACE_PARTITION_COUNT);
                List<LoadedTrace> drained = drainPartition(connection, partition, now, budget);
                if (!drained.isEmpty()) {
                    return drained;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to drain expired traces", e);
        }
        return List.of();
    }

    private List<LoadedTrace> drainPartition(RedisConnection connection, int partition, long now, int budget) {
        byte[][] keysAndArgs = {
                raw(deadlinesKey(partition)),
                raw(Long.toString(now)),
                raw(Integer.toString(budget)),
                raw(traceKeyPrefix(partition)),
                raw(Integer.toString(DRAIN_SCAN_LIMIT))
        };
        List<Object> reply;
        try {
            reply = redisExecTimer.record(() -> executeScript(connection, DRAIN_SCRIPT_SHA, DRAIN_LUA_SCRIPT,
                    ReturnType.MULTI, DRAIN_SCRIPT_NUM_KEYS, keysAndArgs));
        } catch (Exception e) {
            log.warn("Failed to drain expired traces from partition [{}]", partition, e);
            return List.of();
        }
        if (CollectionUtils.isEmpty(reply)) {
            return List.of();
        }
        return parseDrainReply(reply, traceKeyPrefix(partition));
    }

    private List<LoadedTrace> parseDrainReply(List<Object> reply, String traceKeyPrefix) {
        List<LoadedTrace> drained = new ArrayList<>();
        int i = 0;
        while (i + 1 < reply.size()) {
            String member = str(reply.get(i++));
            int fieldElements = Integer.parseInt(str(reply.get(i++)));
            int end = Math.min(i + fieldElements, reply.size());
            String traceKey = traceKeyPrefix + member;
            try {
                Map<String, String> fields = new HashMap<>();
                for (int f = i; f + 1 < end; f += 2) {
                    fields.put(str(reply.get(f)), str(reply.get(f + 1)));
                }
                if (!fields.isEmpty()) {
                    drained.add(toLoadedTrace(traceKey, fields));
                }
            } catch (Exception e) {
                log.warn("Failed to decode drained trace [{}]", traceKey, e);
            }
            i = end;
        }
        return drained;
    }

    @Override
    public boolean isShared() {
        return true;
    }

    private LoadedTrace toLoadedTrace(String traceKey, Map<String, String> fields) {
        RedisTraceKey parsedKey = parseTraceKey(traceKey);
        boolean truncated = false;
        Set<BufferedSpan> spans = new HashSet<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String field = entry.getKey();
            if ("_truncated".equals(field)) {
                truncated = Boolean.parseBoolean(entry.getValue());
                continue;
            }
            if ("_span_count".equals(field)) {
                continue;
            }
            BufferedSpan span = JacksonUtil.fromString(entry.getValue(), BufferedSpan.class);
            if (span == null) {
                continue;
            }

            span.setTenantId(parsedKey.tenantId().toString());
            span.setTraceId(parsedKey.traceId());
            spans.add(span);
        }
        return new LoadedTrace(traceKey,
                new BufferedTrace(parsedKey.tenantId(), parsedKey.traceId(), truncated, spans));
    }

    private static String str(Object value) {
        return value instanceof byte[] bytes ? new String(bytes, StandardCharsets.UTF_8) : String.valueOf(value);
    }

    private Long executeTraceScript(RedisConnection connection, byte[][] keysAndArgs) {
        return executeScript(connection, TRACE_SCRIPT_SHA, TRACE_LUA_SCRIPT, ReturnType.INTEGER,
                TRACE_SCRIPT_NUM_KEYS, keysAndArgs);
    }

    private <T> T executeScript(RedisConnection connection, byte[] sha, byte[] script, ReturnType returnType,
                                int numKeys, byte[][] keysAndArgs) {
        try {
            return connection.scriptingCommands().evalSha(sha, returnType, numKeys, keysAndArgs);
        } catch (InvalidDataAccessApiUsageException noScript) {
            String actualSha = connection.scriptingCommands().scriptLoad(script);
            if (!Arrays.equals(sha, raw(String.valueOf(actualSha)))) {
                throw new IllegalStateException(String.format(
                        "SHA for trace LUA script wrong! Expected [%s], but actual [%s]",
                        new String(sha, StandardCharsets.UTF_8), actualSha));
            }
            try {
                return connection.scriptingCommands().evalSha(sha, returnType, numKeys, keysAndArgs);
            } catch (InvalidDataAccessApiUsageException stillMissing) {
                log.warn("Slowly executing eval instead of fast evalSha", stillMissing);
                return connection.scriptingCommands().eval(script, returnType, numKeys, keysAndArgs);
            }
        }
    }

    private byte[][] buildTraceScriptArgs(int partition, long deadline, Map<String, List<BufferedSpan>> traces) {
        int spanCount = traces.values().stream().mapToInt(List::size).sum();
        List<byte[]> args = new ArrayList<>(5 + traces.size() * 2 + spanCount * 2);
        args.add(raw(deadlinesKey(partition)));
        args.add(raw(Integer.toString(maxSpansPerTrace)));
        args.add(raw(Long.toString(deadline)));
        args.add(raw(traceKeyPrefix(partition)));
        args.add(raw(Long.toString(keyTtlMs)));
        traces.forEach((traceIdentity, traceSpans) -> {
            args.add(raw(traceIdentity));
            args.add(raw(Integer.toString(traceSpans.size())));
            for (BufferedSpan span : traceSpans) {
                args.add(raw(span.getSpanId()));
                args.add(raw(JacksonUtil.toString(span)));
            }
        });
        return args.toArray(new byte[0][]);
    }

    private static String traceIdentity(BufferedSpan span) {
        return span.getTenantId() + ":" + span.getTraceId();
    }

    private static int partitionOf(String traceIdentity) {
        return Math.floorMod(traceIdentity.hashCode(), TRACE_PARTITION_COUNT);
    }

    private static String deadlinesKey(int partition) {
        return "trace:deadlines:{" + PARTITION_TAG + partition + "}";
    }

    private static String traceKeyPrefix(int partition) {
        return "trace:{" + PARTITION_TAG + partition + "}:";
    }

    private static RedisTraceKey parseTraceKey(String traceKey) {
        String identity = traceKey.substring(traceKey.indexOf("}:") + 2);
        int sep = identity.indexOf(':');
        UUID tenantId = UUID.fromString(identity.substring(0, sep));
        String traceId = identity.substring(sep + 1);
        return new RedisTraceKey(tenantId, traceId);
    }

    private static byte[] raw(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private record RedisTraceKey(UUID tenantId, String traceId) {
    }
}
