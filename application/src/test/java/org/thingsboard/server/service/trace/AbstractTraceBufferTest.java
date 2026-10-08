package org.thingsboard.server.service.trace;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.stats.DefaultStatsFactory;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.BufferedSpan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractTraceBufferTest {
    protected static final long TTL_MS = 10_000L;

    protected abstract TraceBuffer createBuffer(int maxSpansPerTrace, long traceTtlMs, StatsFactory statsFactory);

    protected abstract boolean expectedShared();

    protected TraceBuffer createBuffer(int maxSpansPerTrace, long traceTtlMs) {
        return createBuffer(maxSpansPerTrace, traceTtlMs, statsFactory(new SimpleMeterRegistry()));
    }

    @Test
    void isSharedReflectsTheBackend() {
        assertThat(createBuffer(10, TTL_MS).isShared()).isEqualTo(expectedShared());
    }

    @Test
    void appendedSpanIsLoadedBackUnchanged() {
        TraceBuffer buffer = createBuffer(10, TTL_MS);
        UUID tenantId = UUID.randomUUID();
        String traceId = randomTraceId();
        BufferedSpan span = span(tenantId, traceId, "span-1", "parent-1", "Rule Node: Save Timeseries");

        buffer.append(List.of(span));

        BufferedTrace trace = onlyTrace(buffer);
        assertThat(trace.tenantId()).isEqualTo(tenantId);
        assertThat(trace.traceId()).isEqualTo(traceId);
        assertThat(trace.truncated()).isFalse();
        assertThat(trace.spans()).containsExactly(span);
    }

    @Test
    void spansOfDifferentTracesAndTenantsStaySeparate() {
        TraceBuffer buffer = createBuffer(10, TTL_MS);
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        String traceOne = randomTraceId();
        String traceTwo = randomTraceId();

        buffer.append(List.of(
                span(tenantA, traceOne, "a1", null, "one"),
                span(tenantA, traceOne, "a2", "a1", "one-child"),
                span(tenantA, traceTwo, "b1", null, "two"),
                span(tenantB, traceOne, "c1", null, "same-trace-id-other-tenant")));

        List<LoadedTrace> loaded = drain(buffer);
        assertThat(loaded).hasSize(3);
        Map<String, BufferedTrace> byIdentity = loaded.stream().collect(Collectors.toMap(
                t -> t.trace().tenantId() + ":" + t.trace().traceId(), LoadedTrace::trace));
        assertThat(byIdentity.get(tenantA + ":" + traceOne).spans()).hasSize(2);
        assertThat(byIdentity.get(tenantA + ":" + traceTwo).spans()).hasSize(1);
        assertThat(byIdentity.get(tenantB + ":" + traceOne).spans()).hasSize(1);
    }

    @Test
    void reAppendingTheSameSpanIdOverwritesWithoutGrowingTheTrace() {
        TraceBuffer buffer = createBuffer(3, TTL_MS);
        UUID tenantId = UUID.randomUUID();
        String traceId = randomTraceId();

        buffer.append(List.of(span(tenantId, traceId, "span-1", null, "first")));
        buffer.append(List.of(span(tenantId, traceId, "span-1", null, "second")));
        buffer.append(List.of(span(tenantId, traceId, "span-1", null, "third")));

        BufferedTrace trace = onlyTrace(buffer);

        assertThat(trace.truncated()).isFalse();
        assertThat(trace.spans()).hasSize(1);
        assertThat(trace.spans().iterator().next().getName()).isEqualTo("third");
    }

    @Test
    void traceIsTruncatedAtTheSpanCapAndRejectsFurtherSpans() {
        int cap = 5;
        TraceBuffer buffer = createBuffer(cap, TTL_MS);
        UUID tenantId = UUID.randomUUID();
        String traceId = randomTraceId();

        buffer.append(IntStream.range(0, cap + 3)
                .mapToObj(i -> span(tenantId, traceId, "span-" + i, null, "n" + i))
                .collect(Collectors.toList()));

        BufferedTrace trace = onlyTrace(buffer);
        assertThat(trace.spans()).hasSize(cap);
        assertThat(trace.truncated()).isTrue();

        buffer.append(List.of(span(tenantId, traceId, "late-span", null, "late")));

        BufferedTrace afterLateAppend = onlyTrace(buffer);
        assertThat(afterLateAppend.spans()).hasSize(cap);
        assertThat(afterLateAppend.spans().stream().map(BufferedSpan::getSpanId)).doesNotContain("late-span");
        assertThat(afterLateAppend.truncated()).isTrue();
    }

    @Test
    void traceIsTruncatedExactlyAtTheCapNotBefore() {
        int cap = 4;
        TraceBuffer buffer = createBuffer(cap, TTL_MS);
        UUID tenantId = UUID.randomUUID();
        String traceId = randomTraceId();

        buffer.append(IntStream.range(0, cap - 1)
                .mapToObj(i -> span(tenantId, traceId, "span-" + i, null, "n" + i))
                .collect(Collectors.toList()));
        assertThat(onlyTrace(buffer).truncated()).isFalse();

        buffer.append(List.of(span(tenantId, traceId, "span-last", null, "last")));
        BufferedTrace atCap = onlyTrace(buffer);
        assertThat(atCap.spans()).hasSize(cap);
        assertThat(atCap.truncated()).isTrue();
    }

    @Test
    void traceIsNotExpiredBeforeItsDeadline() {
        TraceBuffer buffer = createBuffer(10, TTL_MS);
        long beforeAppend = System.currentTimeMillis();
        buffer.append(List.of(span(UUID.randomUUID(), randomTraceId(), "span-1", null, "one")));

        assertThat(buffer.findExpiredTraceKeys(beforeAppend)).isEmpty();
        assertThat(buffer.findExpiredTraceKeys(System.currentTimeMillis() + TTL_MS * 2)).hasSize(1);
    }

    @Test
    void deadlineSlidesToTheLastAppendedSpan() throws InterruptedException {
        long ttlMs = 1_000L;
        TraceBuffer buffer = createBuffer(10, ttlMs);
        UUID tenantId = UUID.randomUUID();
        String traceId = randomTraceId();

        buffer.append(List.of(span(tenantId, traceId, "span-1", null, "one")));
        long deadlineAfterFirstSpan = System.currentTimeMillis() + ttlMs;

        Thread.sleep(50);
        buffer.append(List.of(span(tenantId, traceId, "span-2", "span-1", "two")));

        assertThat(buffer.findExpiredTraceKeys(deadlineAfterFirstSpan)).isEmpty();
        assertThat(buffer.findExpiredTraceKeys(System.currentTimeMillis() + ttlMs * 2)).hasSize(1);
    }

    @Test
    void removedTraceIsNeitherLoadableNorExpired() {
        TraceBuffer buffer = createBuffer(10, TTL_MS);
        buffer.append(List.of(span(UUID.randomUUID(), randomTraceId(), "span-1", null, "one")));
        List<String> keys = buffer.findExpiredTraceKeys(farFuture());
        assertThat(keys).hasSize(1);

        buffer.remove(keys);

        assertThat(buffer.load(keys)).isEmpty();
        assertThat(buffer.findExpiredTraceKeys(farFuture())).isEmpty();
    }

    @Test
    void loadSkipsVanishedKeysAndStillReturnsTheRest() {
        TraceBuffer buffer = createBuffer(10, TTL_MS);
        buffer.append(List.of(
                span(UUID.randomUUID(), randomTraceId(), "span-1", null, "one"),
                span(UUID.randomUUID(), randomTraceId(), "span-2", null, "two")));
        List<String> keys = buffer.findExpiredTraceKeys(farFuture());
        assertThat(keys).hasSize(2);

        buffer.remove(List.of(keys.get(0)));

        List<LoadedTrace> loaded = buffer.load(keys);
        assertThat(loaded).hasSize(1);
        assertThat(loaded.get(0).bufferKey()).isEqualTo(keys.get(1));
    }

    @Test
    void drainExpiredRemovesWhatItReturnsAndYieldsEveryTraceExactlyOnce() {
        int maxSpansPerTrace = 10;
        int budget = 4;
        int traceCount = 12;
        TraceBuffer buffer = createBuffer(maxSpansPerTrace, TTL_MS);

        List<BufferedSpan> appended = new ArrayList<>();
        for (int i = 0; i < traceCount; i++) {
            UUID tenantId = UUID.randomUUID();
            String traceId = randomTraceId();
            appended.add(span(tenantId, traceId, "span-1", null, "one"));
            appended.add(span(tenantId, traceId, "span-2", "span-1", "two"));
        }
        buffer.append(appended);

        List<LoadedTrace> drained = new ArrayList<>();

        for (int call = 0; call < traceCount * 8; call++) {
            List<LoadedTrace> batch = buffer.drainExpired(farFuture(), budget);
            if (batch.isEmpty()) {
                break;
            }

            assertThat(batch.stream().mapToInt(t -> t.trace().spans().size()).sum())
                    .isLessThanOrEqualTo(budget + maxSpansPerTrace);
            drained.addAll(batch);
        }

        assertThat(drained).hasSize(traceCount);
        assertThat(drained.stream().map(LoadedTrace::bufferKey)).doesNotHaveDuplicates();
        assertThat(drained).allSatisfy(t -> assertThat(spanIds(t.trace())).containsExactlyInAnyOrder("span-1", "span-2"));

        assertThat(buffer.drainExpired(farFuture(), 1000)).isEmpty();
        assertThat(buffer.findExpiredTraceKeys(farFuture())).isEmpty();
    }

    @Test
    void drainExpiredLeavesTracesThatHaveNotReachedTheirDeadline() {
        TraceBuffer buffer = createBuffer(10, TTL_MS);
        long beforeAppend = System.currentTimeMillis();
        buffer.append(List.of(span(UUID.randomUUID(), randomTraceId(), "span-1", null, "one")));

        assertThat(buffer.drainExpired(beforeAppend, 1000)).isEmpty();

        assertThat(buffer.findExpiredTraceKeys(farFuture())).hasSize(1);
        assertThat(buffer.drainExpired(farFuture(), 1000)).hasSize(1);
    }

    @Test
    void drainExpiredOnAnEmptyBufferReturnsEmpty() {
        assertThat(createBuffer(10, TTL_MS).drainExpired(farFuture(), 1000)).isEmpty();
    }

    @Test
    void oneAppendCarryingManyTracesOfUnequalSizeStoresEveryOneOfThem() {
        int cap = 4;

        int traceCount = 200;
        TraceBuffer buffer = createBuffer(cap, TTL_MS);

        UUID fullTenantId = UUID.randomUUID();
        String fullTraceId = randomTraceId();
        buffer.append(IntStream.range(0, cap)
                .mapToObj(i -> span(fullTenantId, fullTraceId, "full-" + i, null, "full"))
                .collect(Collectors.toList()));

        List<BufferedSpan> batch = new ArrayList<>();
        batch.add(span(fullTenantId, fullTraceId, "rejected", null, "late"));
        Map<String, Integer> expectedSpanCounts = new HashMap<>();
        for (int i = 0; i < traceCount; i++) {
            UUID tenantId = UUID.randomUUID();
            String traceId = randomTraceId();
            int spanCount = 1 + i % cap;
            for (int j = 0; j < spanCount; j++) {
                batch.add(span(tenantId, traceId, "span-" + j, null, "n" + j));
            }
            expectedSpanCounts.put(tenantId + ":" + traceId, spanCount);
        }

        buffer.append(batch);

        Map<String, BufferedTrace> loaded = drain(buffer).stream().collect(Collectors.toMap(
                t -> t.trace().tenantId() + ":" + t.trace().traceId(), LoadedTrace::trace));
        assertThat(loaded).hasSize(traceCount + 1);
        expectedSpanCounts.forEach((identity, spanCount) -> assertThat(loaded.get(identity))
                .as("trace [%s]", identity)
                .isNotNull()
                .satisfies(trace -> assertThat(trace.spans()).hasSize(spanCount)));

        BufferedTrace full = loaded.get(fullTenantId + ":" + fullTraceId);
        assertThat(full.truncated()).isTrue();
        assertThat(full.spans()).hasSize(cap);
        assertThat(spanIds(full)).doesNotContain("rejected");
    }

    @Test
    void emptyAndUnknownInputsAreNoOps() {
        TraceBuffer buffer = createBuffer(10, TTL_MS);

        buffer.append(List.of());
        buffer.remove(List.of());

        assertThat(buffer.load(List.of())).isEmpty();
        assertThat(buffer.findExpiredTraceKeys(farFuture())).isEmpty();
    }

    protected static long farFuture() {
        return System.currentTimeMillis() + TTL_MS * 10;
    }

    protected static List<LoadedTrace> drain(TraceBuffer buffer) {
        return buffer.load(buffer.findExpiredTraceKeys(farFuture()));
    }

    protected static BufferedTrace onlyTrace(TraceBuffer buffer) {
        List<LoadedTrace> loaded = drain(buffer);
        assertThat(loaded).hasSize(1);
        return loaded.get(0).trace();
    }

    protected static BufferedSpan span(UUID tenantId, String traceId, String spanId, String parentSpanId, String name) {
        long now = System.currentTimeMillis();
        return new BufferedSpan(
                tenantId.toString(),
                traceId,
                spanId,
                parentSpanId,
                name,
                "tb-core-0",
                JacksonUtil.toJsonNode("{\"tenant.id\":\"" + tenantId + "\",\"queue.id\":\"" + UUID.randomUUID() + "\"}"),
                JacksonUtil.toJsonNode("[{\"name\":\"exception\",\"attributes\":{\"exception.stacktrace\":\"boom\"}}]"),
                now,
                now + 7,
                "OK",
                null,
                now);
    }

    protected static String randomTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    protected static StatsFactory statsFactory(MeterRegistry meterRegistry) {
        DefaultStatsFactory statsFactory = new DefaultStatsFactory();
        ReflectionTestUtils.setField(statsFactory, "meterRegistry", meterRegistry);
        ReflectionTestUtils.setField(statsFactory, "metricsEnabled", true);
        ReflectionTestUtils.setField(statsFactory, "timerPercentilesStr", "0.5");
        statsFactory.init();
        return statsFactory;
    }

    protected static double counterValue(MeterRegistry registry, String name, String tagKey, String tagValue) {
        return registry.find(name).tag(tagKey, tagValue).counters().stream()
                .mapToDouble(io.micrometer.core.instrument.Counter::count)
                .sum();
    }

    protected static Set<String> spanIds(BufferedTrace trace) {
        return trace.spans().stream().map(BufferedSpan::getSpanId).collect(Collectors.toSet());
    }
}
