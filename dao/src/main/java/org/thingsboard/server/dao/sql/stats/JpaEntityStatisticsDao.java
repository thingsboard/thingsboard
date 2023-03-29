/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.stats;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.stats.EntityStatistics;
import org.thingsboard.server.common.data.stats.EntityStatisticsValue;
import org.thingsboard.server.common.msg.tools.SchedulerUtils;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.EntityStatisticsEntity;
import org.thingsboard.server.dao.stats.EntityStatisticsDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.function.UnaryOperator;

@Component
@SqlDao
@RequiredArgsConstructor
public class JpaEntityStatisticsDao implements EntityStatisticsDao {

    private final EntityStatisticsRepository repository;

    @Transactional
    @Override
    public void updateStats(TenantId tenantId, EntityId entityId, UnaryOperator<EntityStatisticsValue> newValue, long ts) {
        EntityStatisticsEntity stats = repository.findByEntityIdAndEntityType(entityId.getId(), entityId.getEntityType());
        if (stats == null) {
            stats = new EntityStatisticsEntity();
            stats.setEntityId(entityId.getId());
            stats.setEntityType(entityId.getEntityType());
            stats.setTenantId(tenantId.getId());
        }
        EntityStatisticsValue previousValue = JacksonUtil.treeToValue(stats.getLatestValue(), EntityStatisticsValue.class);
        if (!isCurrentPeriod(stats.getTs()) && isCurrentPeriod(ts)) { // if new period just started
            previousValue = null;
        }
        EntityStatisticsValue latestValue = newValue.apply(previousValue);
        stats.setLatestValue(JacksonUtil.valueToTree(latestValue));
        stats.setTs(ts);
        repository.save(stats);
    }

    @Override
    public int countByTenantIdAndTsBetweenAndLatestValueProperty(TenantId tenantId, long startTs, long endTs, String property, String value) {
        if (tenantId != null) {
            return repository.countByTenantIdAndTsBetweenAndLatestValueProperty(tenantId.getId(), startTs, endTs, property, value);
        } else {
            return repository.countByTsBetweenAndLatestValueProperty(startTs, endTs, property, value);
        }
    }

    @Override
    public PageData<EntityStatistics> findByTenantIdAndEntityType(TenantId tenantId, EntityType entityType, PageLink pageLink) {
        Pageable pageable = DaoUtil.toPageable(pageLink, List.of(new SortOrder("entityId")));
        return DaoUtil.toPageData(tenantId != null ?
                repository.findByTenantIdAndEntityType(tenantId.getId(), entityType, pageable) :
                repository.findByEntityType(entityType, pageable));
    }

    @Override
    public void deleteByTsBefore(long expTime) {
        repository.deleteByTsBefore(expTime);
    }

    private boolean isCurrentPeriod(long ts) {
        return ts >= getCurrentPeriodStart();
    }

    private static long getCurrentPeriodStart() {
        return SchedulerUtils.getStartOfCurrentMonth();
    }

}
