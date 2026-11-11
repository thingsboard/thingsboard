// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.migrator;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.migrator.utils.Storage;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public abstract class MigrationService {

    public final Logger log = LoggerFactory.getLogger(getClass());

    protected ThreadPoolExecutor executor;
    @Autowired
    protected Storage storage;

    @Value("${stats_print_interval}")
    private int statsPrintInterval;
    @Value("${parallelism_level}")
    private int parallelismLevel;

    protected final ConcurrentMap<String, Long> total = new ConcurrentHashMap<>();
    protected final ConcurrentMap<String, AtomicLong> processed = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {
        int width = Math.max(2, String.valueOf(parallelismLevel).length());
        String format = "migrator%0" + width + "d";
        AtomicInteger counter = new AtomicInteger();
        ThreadFactory threadFactory = r -> {
            var t = new Thread(r, String.format(format, counter.incrementAndGet()));
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        };
        executor = new ThreadPoolExecutor(parallelismLevel, parallelismLevel,
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(5000),
                threadFactory, new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public final void run() throws Exception {
        // TODO: more logs on tenant
        log.info("Starting...");
        start();

        executor.shutdown();
        executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.SECONDS);
        afterFinished();
        log.info("Finished successfully!");
    }

    protected abstract void start() throws Exception;

    protected void afterFinished() throws Exception {}

    protected void reportTotal(String key, long total) {
        this.total.put(key, total);
    }

    // Report a single processed item
    protected void reportProcessed(String key, Object data) {
        reportProcessed(key, 1, data);
    }

    // Report multiple processed items at once (e.g., when skipping lines)
    protected void reportProcessed(String key, long count, Object data) {
        long n = processed.computeIfAbsent(key, k -> new AtomicLong()).addAndGet(count);
        if (n % statsPrintInterval == 0) {
            printStats(key, n, data);
        }
    }

    protected void finishedProcessing(String key) {
        long n = Optional.ofNullable(processed.remove(key)).map(AtomicLong::get).orElse(0L);
        printStats(key, n, null);
    }

    protected void printStats(String key, long n, Object lastData) {
        String stats = String.valueOf(n);
        Long total = this.total.get(key);
        if (total != null && total > 0) {
            stats += "/" + total + " (" + (n * 100 / total) + "%)";
        }
        if (lastData != null) {
            stats += ". Last: " + StringUtils.abbreviate(lastData.toString(), 300);
        }

        log.info("[{}] Processed: {}", key, stats);
    }

}
