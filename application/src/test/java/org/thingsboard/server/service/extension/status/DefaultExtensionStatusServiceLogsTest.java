// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.extension.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionState;
import org.thingsboard.server.common.data.id.ExtensionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.dao.extension.ExtensionService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.service.extension.spi.ExtensionReconciler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the guards on {@link DefaultExtensionStatusService#tailLogs}: the short-lived cache, the
 * single-flight collapse of simultaneous readers, and the bounded executor that keeps a slow
 * orchestrator off the caller's thread pool. These are load properties, not display logic — every
 * assertion here is about how many times the substrate is actually called.
 */
class DefaultExtensionStatusServiceLogsTest {

    private static final int MAX_ENTRY_CHARS = 1024;

    private final ExtensionReconciler reconciler = mock(ExtensionReconciler.class);
    private DefaultExtensionStatusService service;

    @AfterEach
    void tearDown() {
        if (service != null) {
            service.destroy();
        }
    }

    private DefaultExtensionStatusService service(long cacheTtlMs, int maxConcurrentReads, long readTimeoutMs) {
        // @PostConstruct is not invoked for a plain `new`, so no status poll loop runs here; the log
        // executor is built by the constructor and is therefore live.
        service = new DefaultExtensionStatusService(
                reconciler,
                mock(ExtensionService.class),
                mock(PartitionService.class),
                "/api/extension/route",
                10_000L,
                2,
                cacheTtlMs,
                MAX_ENTRY_CHARS,
                8L * 1024 * 1024,
                maxConcurrentReads,
                readTimeoutMs);
        return service;
    }

    private Extension extension() {
        Extension ext = new Extension(new ExtensionId(UUID.randomUUID()));
        ext.setTenantId(TenantId.fromUUID(UUID.randomUUID()));
        ext.setSlug("acme-billing-9f3a");
        ext.setName("Acme Billing");
        ext.setState(ExtensionState.ACTIVE);
        return ext;
    }

    @Test
    void repeatReadWithinTtlIsServedFromCache() {
        Extension ext = extension();
        when(reconciler.tailLogs(any(), anyInt())).thenReturn("hello from the pod");
        DefaultExtensionStatusService svc = service(60_000L, 4, 20_000L);

        assertThat(svc.tailLogs(ext.getTenantId(), ext, 200)).isEqualTo("hello from the pod");
        assertThat(svc.tailLogs(ext.getTenantId(), ext, 200)).isEqualTo("hello from the pod");

        verify(reconciler, times(1)).tailLogs(any(), anyInt());
    }

    @Test
    void differentLineCountsDoNotShareACacheEntry() {
        Extension ext = extension();
        when(reconciler.tailLogs(any(), anyInt())).thenReturn("hello from the pod");
        DefaultExtensionStatusService svc = service(60_000L, 4, 20_000L);

        svc.tailLogs(ext.getTenantId(), ext, 200);
        svc.tailLogs(ext.getTenantId(), ext, 2000);

        // A larger tail is a different response; serving the 200-line snapshot for it would silently
        // ignore what the caller asked for.
        verify(reconciler, times(2)).tailLogs(any(), anyInt());
    }

    @Test
    void oversizedResponseIsServedButNotCached() {
        Extension ext = extension();
        String big = "x".repeat(MAX_ENTRY_CHARS + 1);
        when(reconciler.tailLogs(any(), anyInt())).thenReturn(big);
        DefaultExtensionStatusService svc = service(60_000L, 4, 20_000L);

        assertThat(svc.tailLogs(ext.getTenantId(), ext, 200)).isEqualTo(big);
        assertThat(svc.tailLogs(ext.getTenantId(), ext, 200)).isEqualTo(big);

        // Holding megabyte entries is what the per-entry ceiling exists to prevent, so the second read
        // must go back to the substrate rather than be served from a cached copy.
        verify(reconciler, times(2)).tailLogs(any(), anyInt());
    }

    @Test
    void cacheCanBeDisabled() {
        Extension ext = extension();
        when(reconciler.tailLogs(any(), anyInt())).thenReturn("hello from the pod");
        DefaultExtensionStatusService svc = service(0L, 4, 20_000L);

        svc.tailLogs(ext.getTenantId(), ext, 200);
        svc.tailLogs(ext.getTenantId(), ext, 200);

        verify(reconciler, times(2)).tailLogs(any(), anyInt());
    }

    @Test
    void simultaneousReadersOfOneExtensionCostOneSubstrateCall() throws Exception {
        Extension ext = extension();
        int readers = 8;
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch started = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        when(reconciler.tailLogs(any(), anyInt())).thenAnswer(invocation -> {
            calls.incrementAndGet();
            started.countDown();
            release.await(10, TimeUnit.SECONDS); // hold the read open while the other callers arrive
            return "hello from the pod";
        });
        // One executor thread on purpose: without single-flight the other 7 reads would queue behind
        // this one rather than being collapsed into it.
        DefaultExtensionStatusService svc = service(60_000L, 1, 20_000L);

        ExecutorService pool = Executors.newFixedThreadPool(readers);
        try {
            CountDownLatch done = new CountDownLatch(readers);
            List<String> results = Collections.synchronizedList(new ArrayList<>());
            // Let the first read register itself as in-flight, so the rest are guaranteed to join it
            // rather than race the registration.
            pool.submit(() -> {
                try {
                    results.add(svc.tailLogs(ext.getTenantId(), ext, 200));
                } finally {
                    done.countDown();
                }
            });
            assertThat(started.await(10, TimeUnit.SECONDS)).isTrue();
            for (int i = 1; i < readers; i++) {
                pool.submit(() -> {
                    try {
                        results.add(svc.tailLogs(ext.getTenantId(), ext, 200));
                    } finally {
                        done.countDown();
                    }
                });
            }
            release.countDown();
            assertThat(done.await(20, TimeUnit.SECONDS)).isTrue();
            assertThat(results).hasSize(readers).allMatch("hello from the pod"::equals);
        } finally {
            pool.shutdownNow();
        }

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void slowReadReturnsAPlaceholderInsteadOfHoldingTheCaller() throws Exception {
        Extension ext = extension();
        CountDownLatch release = new CountDownLatch(1);
        when(reconciler.tailLogs(any(), anyInt())).thenAnswer(invocation -> {
            release.await(10, TimeUnit.SECONDS);
            return "hello from the pod";
        });
        DefaultExtensionStatusService svc = service(60_000L, 4, 150L);

        long start = System.currentTimeMillis();
        String answer = svc.tailLogs(ext.getTenantId(), ext, 200);
        long elapsed = System.currentTimeMillis() - start;

        // The caller is released on the read timeout, not when the orchestrator finally answers.
        assertThat(answer).contains("Still reading logs");
        assertThat(elapsed).isLessThan(5_000L);
        release.countDown();
    }

    @Test
    void saturatedExecutorIsRejectedAsARateLimit() throws Exception {
        int distinctExtensions = 400; // past the 256-entry queue plus the single worker
        CountDownLatch release = new CountDownLatch(1);
        when(reconciler.tailLogs(any(), anyInt())).thenAnswer(invocation -> {
            release.await(10, TimeUnit.SECONDS);
            return "hello from the pod";
        });
        // Distinct extensions cannot be collapsed by single-flight, so they are what actually fills the
        // queue. Once it is full the caller must be turned away rather than queued without limit. The
        // read timeout is 1 ms so each iteration returns its placeholder immediately and the loop is not
        // gated on the (deliberately stuck) substrate.
        DefaultExtensionStatusService svc = service(60_000L, 1, 1L);

        try {
            assertThatThrownBy(() -> {
                for (int i = 0; i < distinctExtensions; i++) {
                    svc.tailLogs(extension().getTenantId(), extension(), 200);
                }
            }).isInstanceOf(TbRateLimitsException.class);
        } finally {
            release.countDown();
        }
    }

}
