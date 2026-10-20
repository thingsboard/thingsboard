// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.migrator.utils;

import com.datastax.oss.driver.api.core.cql.ResultSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.cql.ArgumentPreparedStatementBinder;
import org.springframework.data.cassandra.core.cql.CachedPreparedStatementCreator;
import org.springframework.data.cassandra.core.cql.CqlOperations;
import org.springframework.data.cassandra.core.cql.ResultSetExtractor;
import org.springframework.data.cassandra.core.cql.RowMapperResultSetExtractor;
import org.springframework.data.cassandra.core.cql.SingleColumnRowMapper;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Lazy
@Slf4j
public class CassandraService {

    private final CqlOperations cqlOperations;

    @Value("${cassandra.retry.max_retries:10}")
    private int maxRetries;
    @Value("${cassandra.retry.initial_delay:100}")
    private long initialDelay;
    @Value("${cassandra.retry.multiplier:2.0}")
    private double multiplier;

    private final AtomicInteger activeErrors = new AtomicInteger(0);
    private final AtomicLong lastRateLimitLogTime = new AtomicLong(System.nanoTime());

    private static final long RATE_LIMIT_LOG_INTERVAL_NANOS = TimeUnit.SECONDS.toNanos(5);

    public CassandraService(CassandraTemplate cassandraTemplate) {
        this.cqlOperations = cassandraTemplate.getCqlOperations();
    }

    public void execute(String query, Object... args) {
        query(query, args);
    }

    public ResultSet query(String query, Object... args) {
        return query(query, rs -> rs, args);
    }

    public <T> List<T> query(String query, Class<T> type, Object... args) {
        return query(query, new RowMapperResultSetExtractor<>(SingleColumnRowMapper.newInstance(type)), args);
    }

    @SuppressWarnings("deprecation")
    private <T> T query(String query, ResultSetExtractor<T> resultSetExtractor, Object... args) {
        int attempt = 0;
        while (true) {
            try {
                if (activeErrors.get() > 0) {
                    rateLimit();
                }
                boolean success = false;
                try {
                    T result = cqlOperations.query(new CachedPreparedStatementCreator(query), new ArgumentPreparedStatementBinder(args), resultSetExtractor);
                    success = true;
                    return result;
                } finally {
                    if (success && attempt > 0) {
                        activeErrors.decrementAndGet();
                    }
                }
            } catch (Exception e) {
                attempt++;
                if (attempt == 1) {
                    activeErrors.incrementAndGet();
                }
                if (attempt > maxRetries) {
                    String errorMessage = "Failed to execute Cassandra query after " + maxRetries + " retries: " + query + " with args " + Arrays.toString(args);
                    try {
                        log.error(errorMessage, e);
                        throw new RuntimeException(errorMessage, e);
                    } finally {
                        activeErrors.decrementAndGet();
                    }
                }
                long delay = (long) (initialDelay * Math.pow(multiplier, attempt - 1));
                log.warn("Failed attempt to execute Cassandra query (attempt {}/{}): {}. Retrying in {}ms...", attempt, maxRetries, e.getMessage(), delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    activeErrors.decrementAndGet();
                    throw new RuntimeException("Interrupted during Cassandra query retry", ie);
                }
            }
        }
    }

    private void rateLimit() {
        int errors = activeErrors.get();
        if (errors > 0) {
            // Reduce effective parallelism by sleeping briefly or using a small delay
            // The more errors, the longer we wait before proceeding
            long backpressureDelay = Math.min(1000, errors * 50L);
            long now = System.nanoTime();
            long lastLogTime = lastRateLimitLogTime.get();
            if (now - lastLogTime > RATE_LIMIT_LOG_INTERVAL_NANOS) {
                if (lastRateLimitLogTime.compareAndSet(lastLogTime, now)) {
                    log.info("Rate limit active: {} errors, delaying for {}ms", errors, backpressureDelay);
                }
            } else if (log.isDebugEnabled()) {
                log.debug("Rate limit active: {} errors, delaying for {}ms", errors, backpressureDelay);
            }
            try {
                Thread.sleep(backpressureDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

}
