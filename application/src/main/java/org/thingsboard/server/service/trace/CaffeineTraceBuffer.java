package org.thingsboard.server.service.trace;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.BufferedSpan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Component
@ConditionalOnExpression(TraceConditions.LOCAL_CACHE)
public class CaffeineTraceBuffer implements TraceBuffer {
    private final int maxSpansPerTrace;
    private final long traceTtlMs;
    private final long maxBufferedSpans;

    private final Cache<String, BufferedTraceEntry> traces;

    private final AtomicLong bufferedSpans = new AtomicLong();

    private final DefaultCounter failedSpansCounter;
    private final DefaultCounter limitReachedCounter;
    private final DefaultCounter rejectedSpansCounter;

    public CaffeineTraceBuffer(@Value("${otel.processor.trace.buffer.max-spans-per-trace:1000}") int maxSpansPerTrace,
                               @Value("${otel.processor.trace.buffer.trace-ttl-ms:60000}") long traceTtlMs,
                               @Value("${otel.processor.trace.buffer.max-buffered-spans:1000000}") long maxBufferedSpans,
                               StatsFactory statsFactory) {
        this.maxSpansPerTrace = maxSpansPerTrace;
        this.traceTtlMs = traceTtlMs;
        this.maxBufferedSpans = maxBufferedSpans;
        this.failedSpansCounter = statsFactory.createDefaultCounter("trace.export.spans", "result", "failed");
        this.limitReachedCounter = statsFactory.createDefaultCounter("trace.export.limit.reached");
        this.rejectedSpansCounter = statsFactory.createDefaultCounter("trace.buffer.rejected", "cause", "full");
        this.traces = Caffeine.newBuilder()

                .executor(Runnable::run)
                .removalListener((String key, BufferedTraceEntry entry, RemovalCause cause) -> onRemoval(entry, cause))
                .build();
        log.info("Using local trace buffer: maxSpansPerTrace = [{}], traceTtlMs = [{}], maxBufferedSpans = [{}]",
                maxSpansPerTrace, traceTtlMs, maxBufferedSpans);
    }

    @Override
    public void append(Collection<BufferedSpan> spans) {
        if (CollectionUtils.isEmpty(spans)) {
            return;
        }
        Map<String, List<BufferedSpan>> spansByTrace = spans.stream()
                .collect(Collectors.groupingBy(CaffeineTraceBuffer::traceIdentity));
        long deadline = System.currentTimeMillis() + traceTtlMs;
        spansByTrace.forEach((traceIdentity, traceSpans) -> {
            try {
                AppendResult result = appendToTrace(traceIdentity, traceSpans, deadline);
                if (result.rejected() > 0) {
                    rejectedSpansCounter.add(result.rejected());
                    log.debug("Rejected {} spans of trace [{}]: buffer is at its max-buffered-spans limit of {}. " +
                              "Is the aggregation job keeping up?", result.rejected(), traceIdentity, maxBufferedSpans);
                }
                if (result.truncated()) {
                    limitReachedCounter.increment();
                    log.debug("Trace [{}] reached the span limit of {} and is truncated", traceIdentity, maxSpansPerTrace);
                }
            } catch (Exception e) {
                failedSpansCounter.add(traceSpans.size());
                log.warn("Failed to buffer trace [{}] of {} spans", traceIdentity, traceSpans.size(), e);
            }
        });
    }

    @Override
    public List<LoadedTrace> drainExpired(long now, int maxSpans) {
        List<LoadedTrace> loaded = new ArrayList<>();
        int spans = 0;
        for (Map.Entry<String, BufferedTraceEntry> candidate : traces.asMap().entrySet()) {
            if (candidate.getValue().deadline() > now) {
                continue;
            }
            // Re-check and remove under the key's lock, which append also takes: a span appended after the scan
            // either moved the deadline (the trace stays) or is part of what is drained, never dropped in between.
            // removal still fires onRemoval (EXPLICIT), which releases exactly the spans the entry held.
            BufferedTraceEntry[] claimed = new BufferedTraceEntry[1];
            traces.asMap().computeIfPresent(candidate.getKey(), (key, entry) -> {
                if (entry.deadline() > now) {
                    return entry;
                }
                claimed[0] = entry;
                return null;
            });
            if (claimed[0] == null) {
                continue;
            }
            TraceSnapshot snapshot = claimed[0].snapshot();
            TraceIdentity identity = parseTraceIdentity(candidate.getKey());
            loaded.add(new LoadedTrace(candidate.getKey(), new BufferedTrace(identity.tenantId(), identity.traceId(),
                    snapshot.truncated(), snapshot.spans())));
            spans += snapshot.spans().size();
            if (spans >= maxSpans) {
                break;
            }
        }
        return loaded;
    }

    @Override
    public List<String> findExpiredTraceKeys(long now) {
        List<String> expired = new ArrayList<>();
        traces.asMap().forEach((key, entry) -> {
            if (entry.deadline() <= now) {
                expired.add(key);
            }
        });
        return expired;
    }

    @Override
    public List<LoadedTrace> load(Collection<String> traceKeys) {
        List<LoadedTrace> loaded = new ArrayList<>();
        if (CollectionUtils.isEmpty(traceKeys)) {
            return loaded;
        }
        for (String traceKey : traceKeys) {
            try {
                BufferedTraceEntry entry = traces.getIfPresent(traceKey);
                if (entry == null) {
                    continue;
                }

                TraceSnapshot snapshot = entry.snapshot();
                TraceIdentity identity = parseTraceIdentity(traceKey);
                loaded.add(new LoadedTrace(traceKey, new BufferedTrace(identity.tenantId(), identity.traceId(),
                        snapshot.truncated(), snapshot.spans())));
            } catch (Exception e) {
                log.warn("Failed to read buffered trace [{}]", traceKey, e);
            }
        }
        return loaded;
    }

    @Override
    public void remove(Collection<String> traceKeys) {
        if (CollectionUtils.isEmpty(traceKeys)) {
            return;
        }
        traces.invalidateAll(traceKeys);
    }

    @Override
    public boolean isShared() {
        return false;
    }

    private AppendResult appendToTrace(String traceIdentity, List<BufferedSpan> traceSpans, long deadline) {
        AppendResult[] result = new AppendResult[1];
        traces.asMap().compute(traceIdentity, (key, entry) -> {
            BufferedTraceEntry target = entry != null ? entry : new BufferedTraceEntry();

            long headroom = maxBufferedSpans - bufferedSpans.get();
            result[0] = target.append(traceSpans, maxSpansPerTrace, deadline, headroom);
            if (result[0].added() > 0) {
                bufferedSpans.addAndGet(result[0].added());
            }

            return entry == null && target.spanCount() == 0 ? null : target;
        });
        return result[0];
    }

    private void onRemoval(BufferedTraceEntry entry, RemovalCause cause) {
        if (entry != null && cause != RemovalCause.REPLACED) {
            bufferedSpans.addAndGet(-entry.spanCount());
        }
    }

    private static String traceIdentity(BufferedSpan span) {
        return span.getTenantId() + ":" + span.getTraceId();
    }

    private static TraceIdentity parseTraceIdentity(String traceKey) {
        int sep = traceKey.indexOf(':');
        return new TraceIdentity(UUID.fromString(traceKey.substring(0, sep)), traceKey.substring(sep + 1));
    }

    private record TraceIdentity(UUID tenantId, String traceId) {
    }

    private record TraceSnapshot(Set<BufferedSpan> spans, boolean truncated) {
    }

    private record AppendResult(int added, int rejected, boolean truncated) {
    }

    private static final class BufferedTraceEntry {
        private final Map<String, BufferedSpan> spans = new LinkedHashMap<>();
        private boolean truncated;
        private long deadline;

        private synchronized AppendResult append(List<BufferedSpan> incoming, int maxSpansPerTrace,
                                                 long newDeadline, long headroom) {
            if (truncated) {
                return new AppendResult(0, 0, true);
            }
            int added = 0;
            int rejected = 0;
            for (int i = 0; i < incoming.size(); i++) {
                if (spans.size() >= maxSpansPerTrace) {
                    truncated = true;
                    break;
                }
                BufferedSpan span = incoming.get(i);

                if (!spans.containsKey(span.getSpanId()) && added >= headroom) {
                    rejected = incoming.size() - i;
                    break;
                }
                if (spans.put(span.getSpanId(), span) == null) {
                    added++;
                }
            }
            if (spans.size() >= maxSpansPerTrace) {
                truncated = true;
            }
            if (added > 0) {
                deadline = newDeadline;
            }
            return new AppendResult(added, rejected, truncated);
        }

        private synchronized TraceSnapshot snapshot() {
            return new TraceSnapshot(new HashSet<>(spans.values()), truncated);
        }

        private synchronized long deadline() {
            return deadline;
        }

        private synchronized int spanCount() {
            return spans.size();
        }
    }
}
