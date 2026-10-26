// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.tracecoverage;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.trace.TraceCoverageSetting;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.tracecoverage.TraceCoverageSettingEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@Component
@SqlDao
@RequiredArgsConstructor
public class JpaTraceCoverageSettingDao extends JpaAbstractDao<TraceCoverageSettingEntity, TraceCoverageSetting> implements TraceCoverageSettingDao {
    private final TraceCoverageSettingRepository repository;

    @Override
    public TraceCoverageSetting findByTenantId(UUID tenantId) {
        return DaoUtil.getData(repository.findByTenantId(tenantId));
    }

    @Override
    @Transactional
    public TraceCoverageSetting upsertByTenantId(UUID tenantId, TraceCoverageSetting setting) {
        TraceCoverageSettingEntity entity = new TraceCoverageSettingEntity(setting);
        entity.setUuid(Uuids.timeBased());
        entity.setCreatedTime(System.currentTimeMillis());
        entity.setTenantId(tenantId);
        return DaoUtil.getData(repository.upsertByTenantId(entity));
    }

    @Override
    @Transactional
    public void removeByTenantId(UUID tenantId) {
        repository.deleteByTenantId(tenantId);
    }

    @Override
    protected Class<TraceCoverageSettingEntity> getEntityClass() {
        return TraceCoverageSettingEntity.class;
    }

    @Override
    protected JpaRepository<TraceCoverageSettingEntity, UUID> getRepository() {
        return repository;
    }
}
