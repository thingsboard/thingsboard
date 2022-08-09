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
package org.thingsboard.server.dao.sqlts.sql;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.model.sqlts.ts.TsKvEntity;
import org.thingsboard.server.dao.sqlts.AbstractChunkedAggregationTimeseriesDao;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.timeseries.SqlPartition;
import org.thingsboard.server.dao.timeseries.SqlTsPartitionDate;
import org.thingsboard.server.dao.util.SqlTsDao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
@SqlTsDao
public class JpaSqlTimeseriesDao extends AbstractChunkedAggregationTimeseriesDao {

    private final Map<Long, SqlPartition> partitions = new ConcurrentHashMap<>();
    private static final ReentrantLock partitionCreationLock = new ReentrantLock();

    @Autowired
    private SqlPartitioningRepository partitioningRepository;
    @Autowired
    private TenantDao tenantDao;
    @Autowired
    private CustomerDao customerDao;

    private SqlTsPartitionDate tsFormat;

    @Value("${sql.postgres.ts_key_value_partitioning:MONTHS}")
    private String partitioning;


    @Override
    protected void init() {
        super.init();
        Optional<SqlTsPartitionDate> partition = SqlTsPartitionDate.parse(partitioning);
        if (partition.isPresent()) {
            tsFormat = partition.get();
        } else {
            log.warn("Incorrect configuration of partitioning {}", partitioning);
            throw new RuntimeException("Failed to parse partitioning property: " + partitioning + "!");
        }
    }

    @Override
    public ListenableFuture<Integer> save(TenantId tenantId, EntityId entityId, TsKvEntry tsKvEntry, long ttl) {
        int dataPointDays = getDataPointDays(tsKvEntry, computeTtl(ttl));
        savePartitionIfNotExist(tsKvEntry.getTs());
        String strKey = tsKvEntry.getKey();
        Integer keyId = getOrSaveKeyId(strKey);
        TsKvEntity entity = new TsKvEntity();
        entity.setEntityId(entityId.getId());
        entity.setTs(tsKvEntry.getTs());
        entity.setKey(keyId);
        entity.setStrValue(tsKvEntry.getStrValue().orElse(null));
        entity.setDoubleValue(tsKvEntry.getDoubleValue().orElse(null));
        entity.setLongValue(tsKvEntry.getLongValue().orElse(null));
        entity.setBooleanValue(tsKvEntry.getBooleanValue().orElse(null));
        entity.setJsonValue(tsKvEntry.getJsonValue().orElse(null));
        log.trace("Saving entity: {}", entity);
        return Futures.transform(tsQueue.add(entity), v -> dataPointDays, MoreExecutors.directExecutor());
    }

    @Override
    public void cleanup(long systemTtl) {
        cleanupPartitions(systemTtl);
        super.cleanup(systemTtl);
    }

    @Override
    public void cleanup(long systemTtl, List<String> excludedKeys) {
        super.cleanup(systemTtl, excludedKeys);
    }

    @Override
    public long doCleanup(long expirationTime, List<Integer> keyIds, TenantId tenantId, CustomerId customerId) {
        return tsKvRepository.cleanUp(expirationTime, keyIds, tenantId.getId(), customerId.getId());
    }

    public int cleanupPartitions(long systemTtl) {
        long maxTtl = getMaxTtl(systemTtl);
        DateTime dateByTtlDate = getPartitionByTtlDate(maxTtl);
        log.info("Date by max ttl {}", dateByTtlDate);
        String partitionByTtlDate = getPartitionByDate(dateByTtlDate);
        log.info("Partition by max ttl {}", partitionByTtlDate);

        return cleanupPartition(dateByTtlDate, partitionByTtlDate);
    }

    private long getMaxTtl(long systemTtl) {
        long maxTtl = Math.max(systemTtl, 0L);
        PageLink tenantsBatchRequest = new PageLink(PAGE_SIZE, 0);
        PageData<TenantId> tenantsIds;
        do {
            tenantsIds = tenantDao.findTenantsIds(tenantsBatchRequest);

            for (TenantId tenantId : tenantsIds.getData()) {
                long tenantTtl = getTtl(systemTtl, tenantId, tenantId);
                maxTtl = Math.max(maxTtl, tenantTtl);

                PageLink customersBatchRequest = new PageLink(PAGE_SIZE, 0);
                PageData<Customer> customersIds;
                do {
                    customersIds = customerDao.findCustomersByTenantId(tenantId.getId(), customersBatchRequest);

                    for (Customer customer : customersIds.getData()) {
                        long customerTtl = getTtl(tenantTtl, tenantId, customer.getId());
                        maxTtl = Math.max(maxTtl, customerTtl);
                    }
                    customersBatchRequest = customersBatchRequest.nextPageLink();

                } while (customersIds.hasNext());
            }

            tenantsBatchRequest = tenantsBatchRequest.nextPageLink();
        } while (tenantsIds.hasNext());
        return maxTtl;
    }

    private DateTime getPartitionByTtlDate(long maxTtl) {
        return new DateTime(System.currentTimeMillis() - maxTtl);
    }

    private String getPartitionByDate(DateTime date) {
        String result = "";
        switch (partitioning) {
            case "DAYS":
                result = "_" + ((date.getDayOfMonth() < 10) ? "0" + date.getDayOfMonth() : date.getDayOfMonth()) + result;
            case "MONTHS":
                result = "_" + ((date.getMonthOfYear() < 10) ? "0" + date.getMonthOfYear() : date.getMonthOfYear()) + result;
            case "YEARS":
                result = date.getYear() + result;
        }
        return "ts_kv_" + result;
    }

    private int cleanupPartition(DateTime dateByTtlDate, String partitionByTtlDate) {
        int deleted = 0;
        try (Connection connection = dataSource.getConnection();
            PreparedStatement stmt = connection.prepareStatement("SELECT tablename " +
                    "FROM pg_tables " +
                    "WHERE schemaname = 'public'" +
                    "AND tablename like 'ts_kv_' || '%' "+
                    "AND tablename != 'ts_kv_latest' " +
                    "AND tablename != 'ts_kv_dictionary' " +
                    "AND tablename != 'ts_kv_indefinite' " +
                    "AND tablename != ?")) {
                stmt.setString(1, partitionByTtlDate);
                stmt.setQueryTimeout((int) TimeUnit.MINUTES.toSeconds(1));
                stmt.execute();
                try (ResultSet resultSet = stmt.getResultSet()) {
                    //todo :: remove log list with table and log with drop table
                    List<String> allTableName = new ArrayList<>();
                    while (resultSet.next()) {
                        allTableName.add(resultSet.getString(1));
                        log.info("table = {}", resultSet.getString(1));
                        String tableName = resultSet.getString(1);
                        //todo :: in tests have failure after remove ts_kv_1970_01
                        if (tableName != null && checkNeedDropTable(dateByTtlDate, tableName) && !tableName.equals("ts_kv_1970_01")) {
                            log.info("start drop {} table", tableName);
                            dropTable(tableName);
                            deleted++;
                        }
                    }
                    log.info("select this table = {}", allTableName);
                    log.info("Cleanup {} partitions", deleted);
                    return deleted;
                }
        } catch (SQLException e) {
            log.error("SQLException occurred during TTL task execution ", e);
        }
        return deleted;
    }

    private boolean checkNeedDropTable(DateTime date, String tableName) {
        List<String> splitTableName = Arrays.asList(tableName.split("_"));
        //zero position is 'ts', first is 'kv' and after years, months, days
        if (splitTableName.size() > 2 && splitTableName.get(0).equals("ts") && splitTableName.get(1).equals("kv")) {
            switch (partitioning) {
                case "YEARS":
                    return (splitTableName.size() == 3 && date.getYear() > Integer.parseInt(splitTableName.get(2)));
                case "MONTHS":
                    return (
                            splitTableName.size() == 4 && (date.getYear() > Integer.parseInt(splitTableName.get(2))
                                    || (date.getYear() == Integer.parseInt(splitTableName.get(2))
                                    && date.getMonthOfYear() > Integer.parseInt(splitTableName.get(3)))
                            ));
                case "DAYS":
                    return (
                            splitTableName.size() == 5 && (date.getYear() > Integer.parseInt(splitTableName.get(2))
                                    || (date.getYear() == Integer.parseInt(splitTableName.get(2))
                                    && date.getMonthOfYear() > Integer.parseInt(splitTableName.get(3)))
                                    || (date.getYear() == Integer.parseInt(splitTableName.get(2))
                                    && date.getMonthOfYear() == Integer.parseInt(splitTableName.get(3))
                                    && date.getDayOfMonth() > Integer.parseInt(splitTableName.get(4)))
                            ));
            }
        }
        return false;
    }

    private void dropTable(String tableName) {
        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(String.format("DROP TABLE IF EXISTS %s", tableName))) {
            statement.execute();
        } catch (SQLException e) {
            log.error("SQLException occurred during TTL task execution ", e);
        }
    }

    private void savePartitionIfNotExist(long ts) {
        if (!tsFormat.equals(SqlTsPartitionDate.INDEFINITE) && ts >= 0) {
            LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC);
            LocalDateTime localDateTimeStart = tsFormat.trancateTo(time);
            long partitionStartTs = toMills(localDateTimeStart);
            if (partitions.get(partitionStartTs) == null) {
                LocalDateTime localDateTimeEnd = tsFormat.plusTo(localDateTimeStart);
                long partitionEndTs = toMills(localDateTimeEnd);
                ZonedDateTime zonedDateTime = localDateTimeStart.atZone(ZoneOffset.UTC);
                String partitionDate = zonedDateTime.format(DateTimeFormatter.ofPattern(tsFormat.getPattern()));
                savePartition(new SqlPartition(partitionStartTs, partitionEndTs, partitionDate));
            }
        }
    }

    private void savePartition(SqlPartition sqlPartition) {
        if (!partitions.containsKey(sqlPartition.getStart())) {
            partitionCreationLock.lock();
            try {
                log.trace("Saving partition: {}", sqlPartition);
                partitioningRepository.save(sqlPartition);
                log.trace("Adding partition to Set: {}", sqlPartition);
                partitions.put(sqlPartition.getStart(), sqlPartition);
            } catch (DataIntegrityViolationException ex) {
                log.trace("Error occurred during partition save:", ex);
                if (ex.getCause() instanceof ConstraintViolationException) {
                    log.warn("Saving partition [{}] rejected. Timeseries data will save to the ts_kv_indefinite (DEFAULT) partition.", sqlPartition.getPartitionDate());
                    partitions.put(sqlPartition.getStart(), sqlPartition);
                } else {
                    throw new RuntimeException(ex);
                }
            } finally {
                partitionCreationLock.unlock();
            }
        }
    }

    private static long toMills(LocalDateTime time) {
        return time.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}
