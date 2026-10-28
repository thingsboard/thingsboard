// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache.ota;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.stats.StatsFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class OtaPackageBlockCacheTest {

    private static final int BLOCK_SIZE = 8;
    private static final String KEY_A = "a";
    private static final String KEY_B = "b";

    private final Map<String, byte[]> store = new ConcurrentHashMap<>();
    private final AtomicInteger fetchCount = new AtomicInteger();
    private final Map<String, AtomicInteger> fetchCountByKey = new ConcurrentHashMap<>();
    private final List<Integer> fetchedBlocks = Collections.synchronizedList(new ArrayList<>());
    private volatile long fetchDelayMs = 0;

    private byte[] data20;

    @BeforeEach
    void setUp() {
        data20 = sequence(20);
        store.put(KEY_A, data20);
        store.put(KEY_B, sequence(20));
    }

    private static byte[] sequence(int n) {
        byte[] b = new byte[n];
        for (int i = 0; i < n; i++) {
            b[i] = (byte) (i + 1);
        }
        return b;
    }

    private OtaPackageBlockCache newCache(long maxBytes) {
        return new OtaPackageBlockCache(maxBytes, Duration.ofMinutes(10), BLOCK_SIZE, this::fetchBlock,
                key -> store.getOrDefault(key, new byte[0]).length, mock(StatsFactory.class));
    }

    private byte[] fetchBlock(String key, int blockIndex) {
        fetchCount.incrementAndGet();
        fetchCountByKey.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
        fetchedBlocks.add(blockIndex);
        if (fetchDelayMs > 0) {
            try {
                Thread.sleep(fetchDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        byte[] data = store.get(key);
        if (data == null) {
            return null;
        }
        int from = blockIndex * BLOCK_SIZE;
        if (from >= data.length) {
            return new byte[0];
        }
        return Arrays.copyOfRange(data, from, Math.min(from + BLOCK_SIZE, data.length));
    }

    @Test
    void chunkWithinSingleBlock_returnsExactBytes() {
        OtaPackageBlockCache cache = newCache(1024);
        assertThat(cache.getChunk(KEY_A, 4, 1)).containsExactly(Arrays.copyOfRange(data20, 4, 8));
        assertThat(fetchCount.get()).isEqualTo(1);
        assertThat(fetchedBlocks).containsExactly(0);
    }

    @Test
    void chunkCrossingBlockBoundary_returnsExactBytes() {
        OtaPackageBlockCache cache = newCache(1024);
        assertThat(cache.getChunk(KEY_A, 6, 1)).containsExactly(Arrays.copyOfRange(data20, 6, 12));
        assertThat(fetchedBlocks).containsExactlyInAnyOrder(0, 1);
    }

    @Test
    void lastPartialChunk_returnsShortArray() {
        OtaPackageBlockCache cache = newCache(1024);
        assertThat(cache.getChunk(KEY_A, 8, 2)).containsExactly(Arrays.copyOfRange(data20, 16, 20));
    }

    @Test
    void chunkPastEof_returnsEmptyArray() {
        OtaPackageBlockCache cache = newCache(1024);
        assertThat(cache.getChunk(KEY_A, 8, 10)).isEmpty();
    }

    @Test
    void chunkSizeZero_delegatesToGetAll() {
        OtaPackageBlockCache cache = newCache(1024);
        assertThat(cache.getChunk(KEY_A, 0, 0)).containsExactly(data20);
    }

    @Test
    void chunkSizeZero_absentPackage_returnsNull() {
        OtaPackageBlockCache cache = newCache(1024);
        assertThat(cache.getChunk("missing", 0, 0)).isNull();
    }

    @Test
    void constructor_rejectsNonPositiveBlockSize() {
        assertThatThrownBy(() -> new OtaPackageBlockCache(1024, Duration.ofMinutes(10), 0, this::fetchBlock,
                key -> 0, mock(StatsFactory.class))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsZeroExpireAfterAccess() {
        assertThatThrownBy(() -> new OtaPackageBlockCache(1024, Duration.ZERO, BLOCK_SIZE, this::fetchBlock,
                key -> 0, mock(StatsFactory.class))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_rejectsNegativeExpireAfterAccess() {
        assertThatThrownBy(() -> new OtaPackageBlockCache(1024, Duration.ofMinutes(-1), BLOCK_SIZE, this::fetchBlock,
                key -> 0, mock(StatsFactory.class))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getAll_missingKey_returnsNull() {
        OtaPackageBlockCache cache = newCache(1024);
        assertThat(cache.getAll("missing")).isNull();
        assertThat(fetchCount.get()).isZero();
    }

    @Test
    void getAll_assemblesAllBlocksInOrder() {
        OtaPackageBlockCache cache = newCache(1024);
        assertThat(cache.getAll(KEY_A)).containsExactly(data20);
        assertThat(fetchCount.get()).isEqualTo(3);
    }

    @Test
    void missingPackage_isNotCachedAsEmpty() {
        OtaPackageBlockCache cache = newCache(1024);
        String key = "late";
        assertThat(cache.getChunk(key, 8, 0)).isEmpty();
        assertThat(cache.getChunk(key, 8, 0)).isEmpty();
        assertThat(fetchCountByKey.get(key).get()).isEqualTo(2);

        byte[] data = sequence(10);
        store.put(key, data);
        assertThat(cache.getChunk(key, 8, 0)).containsExactly(Arrays.copyOfRange(data, 0, 8));
    }

    @Test
    void repeatedReads_hitLocalCache() {
        OtaPackageBlockCache cache = newCache(1024);
        cache.getChunk(KEY_A, 4, 0);
        cache.getChunk(KEY_A, 4, 1);
        assertThat(fetchCount.get()).isEqualTo(1);
    }

    @Test
    void concurrentReads_coalesceToSingleFetch() throws Exception {
        OtaPackageBlockCache cache = newCache(1024);
        fetchDelayMs = 200;
        int n = 16;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(n);
        try {
            List<Future<byte[]>> futures = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return cache.getChunk(KEY_A, 4, 0);
                }));
            }
            start.countDown();
            for (Future<byte[]> f : futures) {
                assertThat(f.get(10, TimeUnit.SECONDS)).containsExactly(Arrays.copyOfRange(data20, 0, 4));
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(fetchCount.get()).isEqualTo(1);
    }

    @Test
    void evict_removesOnlyThatKeysBlocks() {
        OtaPackageBlockCache cache = newCache(1024);
        cache.getChunk(KEY_A, 4, 0);
        cache.getChunk(KEY_B, 4, 0);
        cache.evict(KEY_A);
        cache.getChunk(KEY_A, 4, 0);
        cache.getChunk(KEY_B, 4, 0);
        assertThat(fetchCountByKey.get(KEY_A).get()).isEqualTo(2);
        assertThat(fetchCountByKey.get(KEY_B).get()).isEqualTo(1);
    }

    @Test
    void hugeDeviceSuppliedChunkSize_doesNotPreallocateAndReturnsPackage() {
        OtaPackageBlockCache cache = newCache(1024);
        assertThat(cache.getChunk(KEY_A, Integer.MAX_VALUE, 0)).containsExactly(data20);
    }

    @Test
    void overflowingChunkStart_returnsEmptyWithoutFetching() {
        OtaPackageBlockCache cache = newCache(1024);
        assertThat(cache.getChunk(KEY_A, Integer.MAX_VALUE, Integer.MAX_VALUE)).isEmpty();
        assertThat(fetchCount.get()).isZero();
    }

    @Test
    void negativeChunkIndex_returnsEmptyWithoutFetching() {
        OtaPackageBlockCache cache = newCache(1024);
        assertThat(cache.getChunk(KEY_A, 8, -1)).isEmpty();
        assertThat(fetchCount.get()).isZero();
    }

    @Test
    void budgetEviction_staysWithinMaxBytes() {
        OtaPackageBlockCache cache = newCache(2L * BLOCK_SIZE);
        store.put("big", sequence(4 * BLOCK_SIZE));
        for (int block = 0; block < 4; block++) {
            cache.getChunk("big", BLOCK_SIZE, block);
        }
        cache.cleanUp();
        assertThat(cache.estimatedSize()).isLessThanOrEqualTo(2);
    }

    @Test
    void oversizedPackage_logsWarnOncePerKey() {
        OtaPackageBlockCache cache = newCache(16);
        cache.getChunk(KEY_A, BLOCK_SIZE, 2);
        cache.getChunk(KEY_A, BLOCK_SIZE, 2);
        assertThat(cache.warnedKeys()).containsExactly(KEY_A);
        cache.evict(KEY_A);
        assertThat(cache.warnedKeys()).isEmpty();
    }

}
