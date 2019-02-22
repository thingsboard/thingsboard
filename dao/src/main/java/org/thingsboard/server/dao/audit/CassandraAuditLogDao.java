/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.server.dao.audit;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.nosql.AuditLogEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTimeDao;
import org.thingsboard.server.dao.timeseries.TsPartitionDate;
import org.thingsboard.server.dao.util.NoSqlDao;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_BY_CUSTOMER_ID_CF;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_BY_ENTITY_ID_CF;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_BY_TENANT_ID_CF;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_BY_USER_ID_CF;
import static org.thingsboard.server.dao.model.ModelConstants.AUDIT_LOG_COLUMN_FAMILY_NAME;

@Component
@Slf4j
@NoSqlDao
public class CassandraAuditLogDao extends CassandraAbstractSearchTimeDao<AuditLogEntity, AuditLog> implements AuditLogDao {

    private static final String INSERT_INTO = "INSERT INTO ";

    @Autowired
    private Environment environment;

    @Override
    protected Class<AuditLogEntity> getColumnFamilyClass() {
        return AuditLogEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return AUDIT_LOG_COLUMN_FAMILY_NAME;
    }

    protected ExecutorService readResultsProcessingExecutor;

    @Value("${audit-log.by_tenant_partitioning}")
    private String partitioning;
    private TsPartitionDate tsFormat;

    @Value("${audit-log.default_query_period}")
    private Integer defaultQueryPeriodInDays;

    private PreparedStatement partitionInsertStmt;
    private PreparedStatement saveByTenantStmt;
    private PreparedStatement saveByTenantIdAndUserIdStmt;
    private PreparedStatement saveByTenantIdAndEntityIdStmt;
    private PreparedStatement saveByTenantIdAndCustomerIdStmt;

    private boolean isInstall() {
        return environment.acceptsProfiles("install");
    }

    @PostConstruct
    public void init() {
        if (!isInstall()) {
            Optional<TsPartitionDate> partition = TsPartitionDate.parse(partitioning);
            if (partition.isPresent()) {
                tsFormat = partition.get();
            } else {
                log.warn("Incorrect configuration of partitioning {}", partitioning);
                throw new RuntimeException("Failed to parse partitioning property: " + partitioning + "!");
            }
        }
        readResultsProcessingExecutor = Executors.newCachedThreadPool();
    }

    @PreDestroy
    public void stopExecutor() {
        if (readResultsProcessingExecutor != null) {
            readResultsProcessingExecutor.shutdownNow();
        }
    }

    private <T> ListenableFuture<T> getFuture(ResultSetFuture future, java.util.function.Function<ResultSet, T> transformer) {
        return Futures.transform(future, new Function<ResultSet, T>() {
            @Nullable
            @Override
            public T apply(@Nullable ResultSet input) {
                return transformer.apply(input);
            }
        }, readResultsProcessingExecutor);
    }

    @Override
    public ListenableFuture<Void> saveByTenantId(AuditLog auditLog) {
        log.debug("Save saveByTenantId [{}] ", auditLog);

        long partition = toPartitionTs(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli());
        BoundStatement stmt = getSaveByTenantStmt().bind();
        stmt = setSaveStmtVariables(stmt, auditLog, partition);
        return getFuture(executeAsyncWrite(auditLog.getTenantId(), stmt), rs -> null);
    }

    @Override
    public ListenableFuture<Void> saveByTenantIdAndEntityId(AuditLog auditLog) {
        log.debug("Save saveByTenantIdAndEntityId [{}] ", auditLog);

        BoundStatement stmt = getSaveByTenantIdAndEntityIdStmt().bind();
        stmt = setSaveStmtVariables(stmt, auditLog, -1);
        return getFuture(executeAsyncWrite(auditLog.getTenantId(), stmt), rs -> null);
    }

    @Override
    public ListenableFuture<Void> saveByTenantIdAndCustomerId(AuditLog auditLog) {
        log.debug("Save saveByTenantIdAndCustomerId [{}] ", auditLog);

        BoundStatement stmt = getSaveByTenantIdAndCustomerIdStmt().bind();
        stmt = setSaveStmtVariables(stmt, auditLog, -1);
        return getFuture(executeAsyncWrite(auditLog.getTenantId(), stmt), rs -> null);
    }

    @Override
    public ListenableFuture<Void> saveByTenantIdAndUserId(AuditLog auditLog) {
        log.debug("Save saveByTenantIdAndUserId [{}] ", auditLog);

        BoundStatement stmt = getSaveByTenantIdAndUserIdStmt().bind();
        stmt = setSaveStmtVariables(stmt, auditLog, -1);
        return getFuture(executeAsyncWrite(auditLog.getTenantId(), stmt), rs -> null);
    }

    private BoundStatement setSaveStmtVariables(BoundStatement stmt, AuditLog auditLog, long partition) {
        stmt.setUUID(0, auditLog.getId().getId())
                .setUUID(1, auditLog.getTenantId().getId())
                .setUUID(2, auditLog.getCustomerId().getId())
                .setUUID(3, auditLog.getEntityId().getId())
                .setString(4, auditLog.getEntityId().getEntityType().name())
                .setString(5, auditLog.getEntityName())
                .setUUID(6, auditLog.getUserId().getId())
                .setString(7, auditLog.getUserName())
                .setString(8, auditLog.getActionType().name())
                .setString(9, auditLog.getActionData() != null ? auditLog.getActionData().toString() : null)
                .setString(10, auditLog.getActionStatus().name())
                .setString(11, auditLog.getActionFailureDetails());
        if (partition > -1) {
            stmt.setLong(12, partition);
        }
        return stmt;
    }

    @Override
    public ListenableFuture<Void> savePartitionsByTenantId(AuditLog auditLog) {
        log.debug("Save savePartitionsByTenantId [{}] ", auditLog);

        long partition = toPartitionTs(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli());

        BoundStatement stmt = getPartitionInsertStmt().bind();
        stmt = stmt.setUUID(0, auditLog.getTenantId().getId())
                .setLong(1, partition);
        return getFuture(executeAsyncWrite(auditLog.getTenantId(), stmt), rs -> null);
    }

    private PreparedStatement getSaveByTenantStmt() {
        if (saveByTenantStmt == null) {
            saveByTenantStmt = getSaveByTenantIdAndCFName(ModelConstants.AUDIT_LOG_BY_TENANT_ID_CF, true);
        }
        return saveByTenantStmt;
    }

    private PreparedStatement getSaveByTenantIdAndEntityIdStmt() {
        if (saveByTenantIdAndEntityIdStmt == null) {
            saveByTenantIdAndEntityIdStmt = getSaveByTenantIdAndCFName(ModelConstants.AUDIT_LOG_BY_ENTITY_ID_CF, false);
        }
        return saveByTenantIdAndEntityIdStmt;
    }

    private PreparedStatement getSaveByTenantIdAndCustomerIdStmt() {
        if (saveByTenantIdAndCustomerIdStmt == null) {
            saveByTenantIdAndCustomerIdStmt = getSaveByTenantIdAndCFName(ModelConstants.AUDIT_LOG_BY_CUSTOMER_ID_CF, false);
        }
        return saveByTenantIdAndCustomerIdStmt;
    }

    private PreparedStatement getSaveByTenantIdAndUserIdStmt() {
        if (saveByTenantIdAndUserIdStmt == null) {
            saveByTenantIdAndUserIdStmt = getSaveByTenantIdAndCFName(ModelConstants.AUDIT_LOG_BY_USER_ID_CF, false);
        }
        return saveByTenantIdAndUserIdStmt;
    }

    private PreparedStatement getSaveByTenantIdAndCFName(String cfName, boolean hasPartition) {
        List columnsList = new ArrayList();
        columnsList.add(ModelConstants.AUDIT_LOG_ID_PROPERTY);
        columnsList.add(ModelConstants.AUDIT_LOG_TENANT_ID_PROPERTY);
        columnsList.add(ModelConstants.AUDIT_LOG_CUSTOMER_ID_PROPERTY);
        columnsList.add(ModelConstants.AUDIT_LOG_ENTITY_ID_PROPERTY);
        columnsList.add(ModelConstants.AUDIT_LOG_ENTITY_TYPE_PROPERTY);
        columnsList.add(ModelConstants.AUDIT_LOG_ENTITY_NAME_PROPERTY);
        columnsList.add(ModelConstants.AUDIT_LOG_USER_ID_PROPERTY);
        columnsList.add(ModelConstants.AUDIT_LOG_USER_NAME_PROPERTY);
        columnsList.add(ModelConstants.AUDIT_LOG_ACTION_TYPE_PROPERTY);
        columnsList.add(ModelConstants.AUDIT_LOG_ACTION_DATA_PROPERTY);
        columnsList.add(ModelConstants.AUDIT_LOG_ACTION_STATUS_PROPERTY);
        columnsList.add(ModelConstants.AUDIT_LOG_ACTION_FAILURE_DETAILS_PROPERTY);
        if (hasPartition) {
            columnsList.add(ModelConstants.AUDIT_LOG_PARTITION_PROPERTY);
        }
        StringJoiner values = new StringJoiner(",");
        for (int i = 0; i < columnsList.size(); i++) {
            values.add("?");
        }
        String statementString = INSERT_INTO + cfName + " (" + String.join(",", columnsList) + ") VALUES (" + values.toString() + ")";
        return prepare(statementString);
    }

    private PreparedStatement getPartitionInsertStmt() {
        if (partitionInsertStmt == null) {
            partitionInsertStmt = prepare(INSERT_INTO + ModelConstants.AUDIT_LOG_BY_TENANT_ID_PARTITIONS_CF +
                    "(" + ModelConstants.AUDIT_LOG_TENANT_ID_PROPERTY +
                    "," + ModelConstants.AUDIT_LOG_PARTITION_PROPERTY + ")" +
                    " VALUES(?, ?)");
        }
        return partitionInsertStmt;
    }

    private long toPartitionTs(long ts) {
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC);
        return tsFormat.truncatedTo(time).toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    @Override
    public List<AuditLog> findAuditLogsByTenantIdAndEntityId(UUID tenantId, EntityId entityId, TimePageLink pageLink) {
        log.trace("Try to find audit logs by tenant [{}], entity [{}] and pageLink [{}]", tenantId, entityId, pageLink);
        List<AuditLogEntity> entities = findPageWithTimeSearch(new TenantId(tenantId), AUDIT_LOG_BY_ENTITY_ID_CF,
                Arrays.asList(eq(ModelConstants.AUDIT_LOG_TENANT_ID_PROPERTY, tenantId),
                        eq(ModelConstants.AUDIT_LOG_ENTITY_TYPE_PROPERTY, entityId.getEntityType()),
                        eq(ModelConstants.AUDIT_LOG_ENTITY_ID_PROPERTY, entityId.getId())),
                pageLink);
        log.trace("Found audit logs by tenant [{}], entity [{}] and pageLink [{}]", tenantId, entityId, pageLink);
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public List<AuditLog> findAuditLogsByTenantIdAndCustomerId(UUID tenantId, CustomerId customerId, TimePageLink pageLink) {
        log.trace("Try to find audit logs by tenant [{}], customer [{}] and pageLink [{}]", tenantId, customerId, pageLink);
        List<AuditLogEntity> entities = findPageWithTimeSearch(new TenantId(tenantId), AUDIT_LOG_BY_CUSTOMER_ID_CF,
                Arrays.asList(eq(ModelConstants.AUDIT_LOG_TENANT_ID_PROPERTY, tenantId),
                        eq(ModelConstants.AUDIT_LOG_CUSTOMER_ID_PROPERTY, customerId.getId())),
                pageLink);
        log.trace("Found audit logs by tenant [{}], customer [{}] and pageLink [{}]", tenantId, customerId, pageLink);
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public List<AuditLog> findAuditLogsByTenantIdAndUserId(UUID tenantId, UserId userId, TimePageLink pageLink) {
        log.trace("Try to find audit logs by tenant [{}], user [{}] and pageLink [{}]", tenantId, userId, pageLink);
        List<AuditLogEntity> entities = findPageWithTimeSearch(new TenantId(tenantId), AUDIT_LOG_BY_USER_ID_CF,
                Arrays.asList(eq(ModelConstants.AUDIT_LOG_TENANT_ID_PROPERTY, tenantId),
                        eq(ModelConstants.AUDIT_LOG_USER_ID_PROPERTY, userId.getId())),
                pageLink);
        log.trace("Found audit logs by tenant [{}], user [{}] and pageLink [{}]", tenantId, userId, pageLink);
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public List<AuditLog> findAuditLogsByTenantId(UUID tenantId, TimePageLink pageLink) {
        log.trace("Try to find audit logs by tenant [{}] and pageLink [{}]", tenantId, pageLink);

        long minPartition;
        if (pageLink.getStartTime() != null && pageLink.getStartTime() != 0) {
            minPartition = toPartitionTs(pageLink.getStartTime());
        } else {
            minPartition = toPartitionTs(LocalDate.now().minusDays(defaultQueryPeriodInDays).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli());
        }

        long maxPartition;
        if (pageLink.getEndTime() != null && pageLink.getEndTime() != 0) {
            maxPartition = toPartitionTs(pageLink.getEndTime());
        } else {
            maxPartition = toPartitionTs(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli());
        }

        List<Long> partitions = fetchPartitions(tenantId, minPartition, maxPartition)
                .all()
                .stream()
                .map(row -> row.getLong(ModelConstants.PARTITION_COLUMN))
                .collect(Collectors.toList());

        AuditLogQueryCursor cursor = new AuditLogQueryCursor(tenantId, pageLink, partitions);
        List<AuditLogEntity> entities = fetchSequentiallyWithLimit(cursor);
        log.trace("Found audit logs by tenant [{}] and pageLink [{}]", tenantId, pageLink);
        return DaoUtil.convertDataList(entities);
    }

    private List<AuditLogEntity> fetchSequentiallyWithLimit(AuditLogQueryCursor cursor) {
        if (cursor.isFull() || !cursor.hasNextPartition()) {
            return cursor.getData();
        } else {
            cursor.addData(findPageWithTimeSearch(new TenantId(cursor.getTenantId()), AUDIT_LOG_BY_TENANT_ID_CF,
                    Arrays.asList(eq(ModelConstants.AUDIT_LOG_TENANT_ID_PROPERTY, cursor.getTenantId()),
                            eq(ModelConstants.AUDIT_LOG_PARTITION_PROPERTY, cursor.getNextPartition())),
                    cursor.getPageLink()));
            return fetchSequentiallyWithLimit(cursor);
        }
    }

    private ResultSet fetchPartitions(UUID tenantId, long minPartition, long maxPartition) {
        Select.Where select = QueryBuilder.select(ModelConstants.AUDIT_LOG_PARTITION_PROPERTY).from(ModelConstants.AUDIT_LOG_BY_TENANT_ID_PARTITIONS_CF)
                .where(eq(ModelConstants.AUDIT_LOG_TENANT_ID_PROPERTY, tenantId));
        select.and(QueryBuilder.gte(ModelConstants.PARTITION_COLUMN, minPartition));
        select.and(QueryBuilder.lte(ModelConstants.PARTITION_COLUMN, maxPartition));
        return executeRead(new TenantId(tenantId), select);
    }

}
