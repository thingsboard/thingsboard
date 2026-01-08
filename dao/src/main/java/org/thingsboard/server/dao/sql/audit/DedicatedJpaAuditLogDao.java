/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.audit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.audit.AuditLog;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.config.DedicatedEventsDataSource;
import org.thingsboard.server.dao.sqlts.insert.sql.DedicatedEventsSqlPartitioningRepository;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Collection;
import java.util.UUID;

import static org.thingsboard.server.dao.config.DedicatedEventsJpaDaoConfig.EVENTS_JDBC_TEMPLATE;
import static org.thingsboard.server.dao.config.DedicatedEventsJpaDaoConfig.EVENTS_PERSISTENCE_UNIT;
import static org.thingsboard.server.dao.config.DedicatedEventsJpaDaoConfig.EVENTS_TRANSACTION_MANAGER;

@DedicatedEventsDataSource
@Component
@SqlDao
public class DedicatedJpaAuditLogDao extends JpaAuditLogDao {

    @Autowired
    @Qualifier(EVENTS_JDBC_TEMPLATE)
    private JdbcTemplate jdbcTemplate;
    @PersistenceContext(unitName = EVENTS_PERSISTENCE_UNIT)
    private EntityManager entityManager;

    public DedicatedJpaAuditLogDao(AuditLogRepository auditLogRepository, DedicatedEventsSqlPartitioningRepository partitioningRepository) {
        super(auditLogRepository, partitioningRepository);
    }

    @Transactional(transactionManager = EVENTS_TRANSACTION_MANAGER)
    @Override
    public AuditLog save(TenantId tenantId, AuditLog domain) {
        return super.save(tenantId, domain);
    }

    @Transactional(transactionManager = EVENTS_TRANSACTION_MANAGER)
    @Override
    public AuditLog saveAndFlush(TenantId tenantId, AuditLog domain) {
        return super.saveAndFlush(tenantId, domain);
    }

    @Transactional(transactionManager = EVENTS_TRANSACTION_MANAGER)
    @Override
    public void removeById(TenantId tenantId, UUID id) {
        super.removeById(tenantId, id);
    }

    @Transactional(transactionManager = EVENTS_TRANSACTION_MANAGER)
    @Override
    public void removeAllByIds(Collection<UUID> ids) {
        super.removeAllByIds(ids);
    }

    @Override
    protected EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    protected JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

}
