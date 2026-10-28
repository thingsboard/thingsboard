// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache.ota;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.stats.StatsFactory;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Transport-local cache of OTA package data, stored in fixed-size blocks with a total byte budget.
 * Concurrent reads of the same block are coalesced into a single fetch. Absent blocks are never cached.
 */
@Slf4j
public class OtaPackageBlockCache {

    @FunctionalInterface
    public interface BlockFetcher {
        /** @return block bytes; null or empty when absent */
        byte[] fetch(String key, int blockIndex);
    }

    @FunctionalInterface
    public interface LengthFetcher {
        /** @return total package length; 0 when absent */
        long length(String key);
    }

    private record BlockKey(String key, int block) {
    }

    private final long maxBytes;
    private final int blockSize;
    private final LengthFetcher lengthFetcher;
    private final LoadingCache<BlockKey, byte[]> cache;
    private final Set<String> warnedKeys = ConcurrentHashMap.newKeySet();

    public OtaPackageBlockCache(long maxBytes, Duration expireAfterAccess, int blockSize,
                                BlockFetcher blockFetcher, LengthFetcher lengthFetcher,
                                StatsFactory statsFactory) {
        if (maxBytes > 0) {
            if (blockSize <= 0) {
                throw new IllegalArgumentException("OTA block cache block size must be positive, got " + blockSize);
            }
            if (expireAfterAccess == null || expireAfterAccess.isZero() || expireAfterAccess.isNegative()) {
                throw new IllegalArgumentException("OTA block cache expire-after-access must be positive, got " + expireAfterAccess);
            }
        }
        this.maxBytes = maxBytes;
        this.blockSize = blockSize;
        this.lengthFetcher = lengthFetcher;
        this.cache = Caffeine.newBuilder()
                .maximumWeight(maxBytes)
                .weigher((BlockKey k, byte[] v) -> v.length)
                .expireAfterAccess(expireAfterAccess)
                .recordStats()
                .build(k -> {
                    byte[] block = blockFetcher.fetch(k.key(), k.block());
                    return block == null || block.length == 0 ? null : block; // null: nothing is stored
                });
        statsFactory.createGauge("otaBlockCache", "hitRate", cache, c -> c.stats().hitRate());
        statsFactory.createGauge("otaBlockCache", "hitCount", cache, c -> c.stats().hitCount());
        statsFactory.createGauge("otaBlockCache", "missCount", cache, c -> c.stats().missCount());
        statsFactory.createGauge("otaBlockCache", "estimatedWeightBytes", cache,
                c -> c.policy().eviction().map(e -> (double) e.weightedSize().orElse(0)).orElse(0.0));
    }

    /**
     * @return the requested chunk (possibly shorter than chunkSize, or empty past EOF); when chunkSize <= 0
     * delegates to {@link #getAll(String)}, which returns null when the package is absent
     */
    public byte[] getChunk(String key, int chunkSize, int chunk) {
        if (chunkSize <= 0) {
            return getAll(key);
        }
        if (chunk < 0) {
            return new byte[0]; // a negative block index would make Redis GETRANGE read from the end of the value
        }
        long start = (long) chunkSize * chunk;
        long end = start + chunkSize - 1;
        long firstBlock = start / blockSize;
        long lastBlock = end / blockSize;
        if (firstBlock > Integer.MAX_VALUE) {
            return new byte[0]; // far past any package Redis can hold; the int cast below would overflow
        }
        // chunkSize is device-supplied and must not size allocations; the buffer grows to the bytes actually read
        ByteArrayOutputStream out = new ByteArrayOutputStream(Math.min(chunkSize, blockSize));
        for (long b = firstBlock; b <= lastBlock; b++) {
            byte[] block = loadBlock(key, (int) b);
            if (block == null) {
                break; // absent or past EOF
            }
            long blockStart = b * blockSize;
            int from = (int) Math.max(start - blockStart, 0);
            int to = (int) Math.min(end - blockStart + 1, block.length);
            if (from < to) {
                out.write(block, from, to - from);
            }
            if (block.length < blockSize) {
                break; // last partial block
            }
        }
        return out.toByteArray();
    }

    public byte[] getAll(String key) {
        long length = lengthFetcher.length(key);
        if (length <= 0) {
            return null;
        }
        if (length > Integer.MAX_VALUE) {
            throw new IllegalStateException("OTA package " + key + " is too large to read as a single array: " + length + " bytes");
        }
        byte[] result = new byte[(int) length];
        int blocks = (int) ((length + blockSize - 1) / blockSize);
        int pos = 0;
        for (int b = 0; b < blocks; b++) {
            byte[] block = loadBlock(key, b);
            if (block == null) {
                return null; // package vanished or changed mid-read
            }
            int n = Math.min(block.length, result.length - pos);
            System.arraycopy(block, 0, result, pos, n);
            pos += n;
        }
        return result;
    }

    public void evict(String key) {
        cache.asMap().keySet().removeIf(k -> k.key().equals(key));
        warnedKeys.remove(key);
    }

    private byte[] loadBlock(String key, int blockIndex) {
        if ((long) (blockIndex + 1) * blockSize > maxBytes && warnedKeys.add(key)) {
            log.warn("OTA package [{}] exceeds the transport block cache budget, blocks will be evicted before reuse. " +
                    "Increase CACHE_OTA_L1_MAX_BYTES.", key);
        }
        return cache.get(new BlockKey(key, blockIndex));
    }

    Set<String> warnedKeys() {
        return warnedKeys;
    }

    long estimatedSize() {
        return cache.estimatedSize();
    }

    void cleanUp() {
        cache.cleanUp();
    }

}
