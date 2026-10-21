/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
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
