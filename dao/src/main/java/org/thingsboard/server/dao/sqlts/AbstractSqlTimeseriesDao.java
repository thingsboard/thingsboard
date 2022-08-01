/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.sqlts;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.ReadTsKvQuery;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.attributes.AttributesDao;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.sql.ScheduledLogExecutorComponent;
import org.thingsboard.server.dao.tenant.TenantDao;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public abstract class AbstractSqlTimeseriesDao extends BaseAbstractSqlTimeseriesDao implements AggregationTimeseriesDao {

    protected static final long SECONDS_IN_DAY = TimeUnit.DAYS.toSeconds(1);
    public static final String TTL = "TTL";
    public static final int PAGE_SIZE = 10_000;

    @Autowired
    protected ScheduledLogExecutorComponent logExecutor;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private AttributesDao attributesDao;

    @Value("${sql.ts.batch_size:1000}")
    protected int tsBatchSize;

    @Value("${sql.ts.batch_max_delay:100}")
    protected long tsMaxDelay;

    @Value("${sql.ts.stats_print_interval_ms:1000}")
    protected long tsStatsPrintIntervalMs;

    @Value("${sql.ts.batch_threads:4}")
    protected int tsBatchThreads;

    @Value("${sql.timescale.batch_threads:4}")
    protected int timescaleBatchThreads;

    @Value("${sql.batch_sort:false}")
    protected boolean batchSortEnabled;

    @Value("${sql.ttl.ts.ts_key_value_ttl:0}")
    private long systemTtl;

    public void cleanup(long systemTtl) {
        log.info("Going to cleanup old timeseries data using ttl: {}s", systemTtl);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement("call cleanup_timeseries_by_ttl(?,?,?)")) {
            stmt.setObject(1, ModelConstants.NULL_UUID);
            stmt.setLong(2, systemTtl);
            stmt.setLong(3, 0);
            stmt.setQueryTimeout((int) TimeUnit.HOURS.toSeconds(1));
            stmt.execute();
            printWarnings(stmt);
            try (ResultSet resultSet = stmt.getResultSet()) {
                resultSet.next();
                log.info("Total telemetry removed stats by TTL for entities: [{}]", resultSet.getLong(1));
            }
        } catch (SQLException e) {
            log.error("SQLException occurred during timeseries TTL task execution ", e);
        }
    }

    public void cleanup(long systemTtl, List<String> excludedKeys) {
        PageLink tenantsBatchRequest = new PageLink(PAGE_SIZE, 0);
        PageData<TenantId> tenantsIds;
        do {
            tenantsIds = tenantDao.findTenantsIds(tenantsBatchRequest);

            for (TenantId tenantId : tenantsIds.getData()) {
                long tenantTtl = getTtl(systemTtl, tenantId, tenantId);
                cleanup(tenantTtl, excludedKeys, tenantId, new CustomerId(CustomerId.NULL_UUID));

                PageLink customersBatchRequest = new PageLink(PAGE_SIZE, 0);
                PageData<Customer> customersIds;
                do {
                    customersIds = customerDao.findCustomersByTenantId(tenantId.getId(), customersBatchRequest);

                    for (Customer customer : customersIds.getData()) {
                        long customerTtl = getTtl(tenantTtl, tenantId, customer.getId());
                        cleanup(customerTtl, excludedKeys, tenantId, customer.getId());
                    }
                    customersBatchRequest = customersBatchRequest.nextPageLink();

                } while (customersIds.hasNext());
            }

            tenantsBatchRequest = tenantsBatchRequest.nextPageLink();
        } while (tenantsIds.hasNext());
    }

    private long getTtl(long standardTtl, TenantId tenantId, EntityId entityId) {
        Optional<AttributeKvEntry> ttl;
        try {
            ttl = attributesDao.find(tenantId, entityId, DataConstants.SERVER_SCOPE, TTL).get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot get server scope attribute TTL for entity[" + entityId + "], error: " + e);
        }

        return ttl != null && ttl.isPresent() && ttl.get().getLongValue().isPresent() ? ttl.get().getLongValue().get() : standardTtl;
    }

    private void cleanup(long ttl, List<String> excludedKeys, TenantId tenantId, CustomerId customerId) {
        List<Integer> keyIds = excludedKeys.stream().map(this::getOrSaveKeyId).collect(Collectors.toList());
        EntityId entityId = customerId.getId().equals(CustomerId.NULL_UUID) ? tenantId : customerId;
        log.info("Going to cleanup old timeseries data for entityId {} using ttl: {}s without keys {}", entityId, ttl, excludedKeys);
        try {
            long totalRemoved = doCleanup(getExpirationTime(ttl), keyIds, tenantId, customerId);
            log.info("Total telemetry removed stats by TTL without keys {} for entities: [{}]", excludedKeys, totalRemoved);
        } catch (Exception e) {
            log.info("Failed to execute cleanup using ttl: {} without keys {} due to: [{}]", ttl, excludedKeys, e.getMessage());
        }
    }

    private long getExpirationTime(long ttl) {
        return System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(ttl);
    }

    public abstract long doCleanup(long expirationTime, List<Integer> keyIds, TenantId entityId, CustomerId customerId);

    protected ListenableFuture<List<TsKvEntry>> processFindAllAsync(TenantId tenantId, EntityId entityId, List<ReadTsKvQuery> queries) {
        List<ListenableFuture<List<TsKvEntry>>> futures = queries
                .stream()
                .map(query -> findAllAsync(tenantId, entityId, query))
                .collect(Collectors.toList());
        return Futures.transform(Futures.allAsList(futures), new Function<>() {
            @Nullable
            @Override
            public List<TsKvEntry> apply(@Nullable List<List<TsKvEntry>> results) {
                if (results == null || results.isEmpty()) {
                    return null;
                }
                return results.stream()
                        .filter(Objects::nonNull)
                        .flatMap(List::stream)
                        .collect(Collectors.toList());
            }
        }, service);
    }

    protected long computeTtl(long ttl) {
        if (systemTtl > 0) {
            if (ttl == 0) {
                ttl = systemTtl;
            } else {
                ttl = Math.min(systemTtl, ttl);
            }
        }
        return ttl;
    }

    protected int getDataPointDays(TsKvEntry tsKvEntry, long ttl) {
        return tsKvEntry.getDataPoints() * Math.max(1, (int) (ttl / SECONDS_IN_DAY));
    }
}
