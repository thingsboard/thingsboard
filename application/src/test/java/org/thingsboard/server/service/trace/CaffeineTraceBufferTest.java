package org.thingsboard.server.service.trace;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.BufferedSpan;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class CaffeineTraceBufferTest extends AbstractTraceBufferTest {
    private static final long UNLIMITED_SPANS = 1_000_000L;

    @Override
    protected TraceBuffer createBuffer(int maxSpansPerTrace, long traceTtlMs, StatsFactory statsFactory) {
        return new CaffeineTraceBuffer(maxSpansPerTrace, traceTtlMs, UNLIMITED_SPANS, statsFactory);
    }

    @Override
    protected boolean expectedShared() {
        return false;
    }

    @Test
    void concurrentAppendsToOneTraceLoseNoSpan() throws InterruptedException {
        int threads = 8;
        int spansPerThread = 200;
        int totalSpans = threads * spansPerThread;
        TraceBuffer buffer = createBuffer(totalSpans + 1, TTL_MS);
        UUID tenantId = UUID.randomUUID();
        String traceId = randomTraceId();

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        try {
            for (int t = 0; t < threads; t++) {
                int threadIndex = t;
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < spansPerThread; i++) {
                            buffer.append(List.of(span(tenantId, traceId, "span-" + threadIndex + "-" + i, null, "n")));
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        BufferedTrace trace = onlyTrace(buffer);
        assertThat(trace.truncated()).isFalse();
        assertThat(spanIds(trace)).hasSize(totalSpans);
    }

    @Test
    void concurrentAppendsRespectTheSpanCap() throws InterruptedException {
        int threads = 8;
        int cap = 50;
        TraceBuffer buffer = createBuffer(cap, TTL_MS);
        UUID tenantId = UUID.randomUUID();
        String traceId = randomTraceId();

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch done = new CountDownLatch(threads);
        try {
            for (int t = 0; t < threads; t++) {
                int threadIndex = t;
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < 100; i++) {
                            buffer.append(List.of(span(tenantId, traceId, "span-" + threadIndex + "-" + i, null, "n")));
                        }
                    } finally {
                        done.countDown();
                    }
                });
            }
            assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        BufferedTrace trace = onlyTrace(buffer);
        assertThat(trace.spans()).hasSize(cap);
        assertThat(trace.truncated()).isTrue();
    }

    @Test
    void reachingTheSpanBoundRejectsNewSpansAndKeepsEverythingAlreadyBuffered() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        int maxBufferedSpans = 100;
        TraceBuffer buffer = new CaffeineTraceBuffer(10, TTL_MS, maxBufferedSpans, statsFactory(registry));

        List<BufferedSpan> spans = IntStream.range(0, 600)
                .mapToObj(i -> span(UUID.randomUUID(), randomTraceId(), "span-" + i, null, "n" + i))
                .collect(Collectors.toList());
        spans.forEach(span -> buffer.append(List.of(span)));

        assertThat(counterValue(registry, "trace.buffer.rejected", "cause", "full")).isEqualTo(500);

        List<BufferedSpan> buffered = buffer.load(buffer.findExpiredTraceKeys(farFuture())).stream()
                .flatMap(loaded -> loaded.trace().spans().stream())
                .collect(Collectors.toList());
        assertThat(buffered).hasSize(maxBufferedSpans);
        assertThat(buffered).extracting(BufferedSpan::getSpanId)
                .containsExactlyInAnyOrderElementsOf(spans.subList(0, maxBufferedSpans).stream()
                        .map(BufferedSpan::getSpanId).collect(Collectors.toList()));
    }

    @Test
    void drainingAFullBufferMakesRoomForNewSpansAgain() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TraceBuffer buffer = new CaffeineTraceBuffer(10, TTL_MS, 2L, statsFactory(registry));
        buffer.append(List.of(span(UUID.randomUUID(), randomTraceId(), "span-0", null, "n0")));
        buffer.append(List.of(span(UUID.randomUUID(), randomTraceId(), "span-1", null, "n1")));

        buffer.append(List.of(span(UUID.randomUUID(), randomTraceId(), "rejected", null, "n2")));
        assertThat(counterValue(registry, "trace.buffer.rejected", "cause", "full")).isEqualTo(1);

        buffer.remove(buffer.findExpiredTraceKeys(farFuture()));

        UUID tenantId = UUID.randomUUID();
        String traceId = randomTraceId();
        buffer.append(List.of(span(tenantId, traceId, "fresh", null, "fresh")));
        assertThat(buffer.load(buffer.findExpiredTraceKeys(farFuture())).stream()
                .flatMap(loaded -> loaded.trace().spans().stream())
                .map(BufferedSpan::getSpanId)).containsExactly("fresh");
        assertThat(counterValue(registry, "trace.buffer.rejected", "cause", "full")).isEqualTo(1);
    }

    @Test
    void aRejectedAppendDoesNotPushTheTraceDeadlineOut() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TraceBuffer buffer = new CaffeineTraceBuffer(10, 1L, 1L, statsFactory(registry));
        UUID tenantId = UUID.randomUUID();
        String traceId = randomTraceId();
        buffer.append(List.of(span(tenantId, traceId, "span-0", null, "n0")));

        await().pollDelay(50, TimeUnit.MILLISECONDS).timeout(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(buffer.findExpiredTraceKeys(System.currentTimeMillis())).hasSize(1));

        buffer.append(List.of(span(tenantId, traceId, "span-1", null, "n1")));

        assertThat(counterValue(registry, "trace.buffer.rejected", "cause", "full")).isEqualTo(1);
        assertThat(buffer.findExpiredTraceKeys(System.currentTimeMillis())).hasSize(1);
    }

    @Test
    void aTraceRejectedOutrightLeavesNoEmptyEntryBehind() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TraceBuffer buffer = new CaffeineTraceBuffer(10, TTL_MS, 1L, statsFactory(registry));
        buffer.append(List.of(span(UUID.randomUUID(), randomTraceId(), "span-0", null, "n0")));

        buffer.append(List.of(span(UUID.randomUUID(), randomTraceId(), "never-admitted", null, "n1")));

        assertThat(buffer.findExpiredTraceKeys(farFuture())).hasSize(1);
        assertThat(onlyTrace(buffer).spans()).extracting(BufferedSpan::getSpanId).containsExactly("span-0");
    }

    @Test
    void traceNothingDrainsIsKeptUntilItIsDrained() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();

        TraceBuffer buffer = new CaffeineTraceBuffer(10, 1L, UNLIMITED_SPANS, statsFactory(registry));
        buffer.append(List.of(span(UUID.randomUUID(), randomTraceId(), "span-1", null, "one")));

        await().pollDelay(200, TimeUnit.MILLISECONDS).timeout(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(buffer.findExpiredTraceKeys(farFuture())).hasSize(1));

        List<String> keys = buffer.findExpiredTraceKeys(farFuture());
        assertThat(buffer.load(keys)).hasSize(1);
        assertThat(counterValue(registry, "trace.buffer.rejected", "cause", "full")).isZero();
    }

    @Test
    void repeatedAppendsToOneTraceNeverDropIt() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TraceBuffer buffer = new CaffeineTraceBuffer(10, TTL_MS, UNLIMITED_SPANS, statsFactory(registry));
        UUID tenantId = UUID.randomUUID();
        String traceId = randomTraceId();

        for (int i = 0; i < 5; i++) {
            buffer.append(List.of(span(tenantId, traceId, "span-" + i, null, "n" + i)));
        }

        assertThat(onlyTrace(buffer).spans()).hasSize(5);
        assertThat(counterValue(registry, "trace.buffer.rejected", "cause", "full")).isZero();
    }

    @Test
    void normalDrainIsNotCountedAsARejection() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        TraceBuffer buffer = new CaffeineTraceBuffer(10, TTL_MS, UNLIMITED_SPANS, statsFactory(registry));
        buffer.append(List.of(span(UUID.randomUUID(), randomTraceId(), "span-1", null, "one")));

        List<String> keys = buffer.findExpiredTraceKeys(farFuture());
        buffer.load(keys);
        buffer.remove(keys);

        assertThat(counterValue(registry, "trace.buffer.rejected", "cause", "full")).isZero();
    }

    @Test
    void appendedSpansAreNotSerialized() {
        TraceBuffer buffer = createBuffer(10, TTL_MS);
        BufferedSpan span = span(UUID.randomUUID(), randomTraceId(), "span-1", null, "one");

        buffer.append(List.of(span));

        assertThat(onlyTrace(buffer).spans().iterator().next()).isSameAs(span);
    }

    @Test
    void appendsRacingTheDrainAreDrainedExactlyOnce() throws Exception {
        int spansToAppend = 200_000;
        // TTL 0: every trace is claimable the moment it's appended, so the drain constantly races the writer.
        CaffeineTraceBuffer buffer = new CaffeineTraceBuffer(Integer.MAX_VALUE, 0, UNLIMITED_SPANS, statsFactory(new SimpleMeterRegistry()));
        UUID tenantId = UUID.randomUUID();
        String traceId = randomTraceId();
        Set<String> drained = ConcurrentHashMap.newKeySet();
        AtomicInteger duplicates = new AtomicInteger();
        AtomicBoolean writing = new AtomicBoolean(true);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> drainer = executor.submit(() -> {
                while (writing.get()) {
                    collect(buffer.drainExpired(System.currentTimeMillis(), Integer.MAX_VALUE), drained, duplicates);
                }
            });
            Future<?> writer = executor.submit(() -> {
                for (int i = 0; i < spansToAppend; i++) {
                    buffer.append(List.of(span(tenantId, traceId, "span-" + i, null, "n")));
                }
            });
            writer.get(60, TimeUnit.SECONDS);
            writing.set(false);
            drainer.get(60, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
        collect(buffer.drainExpired(Long.MAX_VALUE, Integer.MAX_VALUE), drained, duplicates);

        assertThat(duplicates.get()).isZero();
        assertThat(drained).hasSize(spansToAppend);
        assertThat(((AtomicLong) ReflectionTestUtils.getField(buffer, "bufferedSpans")).get()).isZero();
    }

    private static void collect(List<LoadedTrace> loaded, Set<String> drained, AtomicInteger duplicates) {
        for (LoadedTrace trace : loaded) {
            for (BufferedSpan span : trace.trace().spans()) {
                if (!drained.add(span.getSpanId())) {
                    duplicates.incrementAndGet();
                }
            }
        }
    }
}
