/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
import com.datastax.driver.core.utils.UUIDs;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.id.AuditLogId;
import org.thingsboard.server.common.data.id.EntityId;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static org.thingsboard.server.dao.model.ModelConstants.*;

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

    @Value("${cassandra.query.ts_key_value_partitioning}")
    private String partitioning;
    private TsPartitionDate tsFormat;

    private PreparedStatement[] saveStmts;

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

        AuditLogId auditLogId = new AuditLogId(UUIDs.timeBased());

        long partition = toPartitionTs(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli());
        BoundStatement stmt = getSaveByTenantStmt().bind();
        stmt.setUUID(0, auditLogId.getId())
                .setUUID(1, auditLog.getTenantId().getId())
                .setUUID(2, auditLog.getEntityId().getId())
                .setString(3, auditLog.getEntityId().getEntityType().name())
                .setString(4, auditLog.getActionType().name())
                .setLong(5, partition);
        return getFuture(executeAsyncWrite(stmt), rs -> null);
    }

    @Override
    public ListenableFuture<Void> saveByTenantIdAndEntityId(AuditLog auditLog) {
        log.debug("Save saveByTenantIdAndEntityId [{}] ", auditLog);

        AuditLogId auditLogId = new AuditLogId(UUIDs.timeBased());

        BoundStatement stmt = getSaveByTenantIdAndEntityIdStmt().bind();
        stmt.setUUID(0, auditLogId.getId())
                .setUUID(1, auditLog.getTenantId().getId())
                .setUUID(2, auditLog.getEntityId().getId())
                .setString(3, auditLog.getEntityId().getEntityType().name())
                .setString(4, auditLog.getActionType().name());
        return getFuture(executeAsyncWrite(stmt), rs -> null);
    }

    @Override
    public ListenableFuture<Void> savePartitionsByTenantId(AuditLog auditLog) {
        log.debug("Save savePartitionsByTenantId [{}] ", auditLog);

        long partition = toPartitionTs(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli());

        BoundStatement stmt = getPartitionInsertStmt().bind();
        stmt = stmt.setUUID(0, auditLog.getTenantId().getId())
                .setLong(1, partition);
        return getFuture(executeAsyncWrite(stmt), rs -> null);
    }

    private PreparedStatement getPartitionInsertStmt() {
        // TODO: ADD CACHE LOGIC
        return getSession().prepare(INSERT_INTO + ModelConstants.AUDIT_LOG_BY_TENANT_ID_PARTITIONS_CF +
                "(" + ModelConstants.AUDIT_LOG_TENANT_ID_PROPERTY +
                "," + ModelConstants.AUDIT_LOG_PARTITION_PROPERTY + ")" +
                " VALUES(?, ?)");
    }

    private PreparedStatement getSaveByTenantIdAndEntityIdStmt() {
        // TODO: ADD CACHE LOGIC
        return getSession().prepare(INSERT_INTO + ModelConstants.AUDIT_LOG_BY_ENTITY_ID_CF +
                "(" + ModelConstants.AUDIT_LOG_ID_PROPERTY +
                "," + ModelConstants.AUDIT_LOG_TENANT_ID_PROPERTY +
                "," + ModelConstants.AUDIT_LOG_ENTITY_ID_PROPERTY +
                "," + ModelConstants.AUDIT_LOG_ENTITY_TYPE_PROPERTY +
                "," + ModelConstants.AUDIT_LOG_ACTION_TYPE_PROPERTY + ")" +
                " VALUES(?, ?, ?, ?, ?)");
    }

    private PreparedStatement getSaveByTenantStmt() {
        // TODO: ADD CACHE LOGIC
        return getSession().prepare(INSERT_INTO + ModelConstants.AUDIT_LOG_BY_TENANT_ID_CF +
                "(" + ModelConstants.AUDIT_LOG_ID_PROPERTY +
                "," + ModelConstants.AUDIT_LOG_TENANT_ID_PROPERTY +
                "," + ModelConstants.AUDIT_LOG_ENTITY_ID_PROPERTY +
                "," + ModelConstants.AUDIT_LOG_ENTITY_TYPE_PROPERTY +
                "," + ModelConstants.AUDIT_LOG_ACTION_TYPE_PROPERTY +
                "," + ModelConstants.AUDIT_LOG_PARTITION_PROPERTY + ")" +
                " VALUES(?, ?, ?, ?, ?, ?)");
    }

    private long toPartitionTs(long ts) {
        LocalDateTime time = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC);
        return tsFormat.truncatedTo(time).toInstant(ZoneOffset.UTC).toEpochMilli();
    }


    @Override
    public List<AuditLog> findAuditLogsByTenantIdAndEntityId(UUID tenantId, EntityId entityId, TimePageLink pageLink) {
        log.trace("Try to find audit logs by tenant [{}], entity [{}] and pageLink [{}]", tenantId, entityId, pageLink);
        List<AuditLogEntity> entities = findPageWithTimeSearch(AUDIT_LOG_BY_ENTITY_ID_CF,
                Arrays.asList(eq(ModelConstants.AUDIT_LOG_TENANT_ID_PROPERTY, tenantId),
                        eq(ModelConstants.AUDIT_LOG_ENTITY_TYPE_PROPERTY, entityId.getEntityType()),
                        eq(ModelConstants.AUDIT_LOG_ENTITY_ID_PROPERTY, entityId.getId())),
                pageLink);
        log.trace("Found audit logs by tenant [{}], entity [{}] and pageLink [{}]", tenantId, entityId, pageLink);
        return DaoUtil.convertDataList(entities);
    }

    @Override
    public List<AuditLog> findAuditLogsByTenantId(UUID tenantId, TimePageLink pageLink) {
        log.trace("Try to find audit logs by tenant [{}] and pageLink [{}]", tenantId, pageLink);

        // TODO: ADD AUDIT LOG PARTITION CURSOR LOGIC

        long minPartition;
        long maxPartition;

        if (pageLink.getStartTime() != null && pageLink.getStartTime() != 0) {
            minPartition = toPartitionTs(pageLink.getStartTime());
        } else {
            minPartition = toPartitionTs(LocalDate.now().minusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli());
        }

        if (pageLink.getEndTime() != null && pageLink.getEndTime() != 0) {
            maxPartition = toPartitionTs(pageLink.getEndTime());
        } else {
            maxPartition = toPartitionTs(LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli());
        }
        List<AuditLogEntity> entities = findPageWithTimeSearch(AUDIT_LOG_BY_TENANT_ID_CF,
                Arrays.asList(eq(ModelConstants.AUDIT_LOG_TENANT_ID_PROPERTY, tenantId),
                        eq(ModelConstants.AUDIT_LOG_PARTITION_PROPERTY, maxPartition)),
                pageLink);
        log.trace("Found audit logs by tenant [{}] and pageLink [{}]", tenantId, pageLink);
        return DaoUtil.convertDataList(entities);
    }
}
