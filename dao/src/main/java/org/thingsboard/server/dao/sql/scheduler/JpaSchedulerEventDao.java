// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.SchedulerEventFields;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.scheduler.SchedulerEvent;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.SchedulerEventEntity;
import org.thingsboard.server.dao.scheduler.SchedulerEventDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@SqlDao
public class JpaSchedulerEventDao extends JpaAbstractDao<SchedulerEventEntity, SchedulerEvent> implements SchedulerEventDao {

    @Autowired
    SchedulerEventRepository schedulerEventRepository;

    @Override
    public PageData<SchedulerEvent> findSchedulerEventsByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, PageLink pageLink) {
        log.debug("Try to find scheduler events by tenantId [{}], edgeId [{}] and pageLink [{}]", tenantId, edgeId, pageLink);
        return DaoUtil.toPageData(schedulerEventRepository
                .findByTenantIdAndEdgeId(
                        tenantId,
                        edgeId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<SchedulerEvent> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(schedulerEventRepository.findByTenantId(tenantId, DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<SchedulerEventId> findIdsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.pageToPageData(schedulerEventRepository.findIdsByTenantId(tenantId, DaoUtil.toPageable(pageLink)).map(SchedulerEventId::new));
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return schedulerEventRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public PageData<SchedulerEvent> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public List<SchedulerEventFields> findNextBatch(UUID id, int batchSize) {
        return schedulerEventRepository.findNextBatch(id, Limit.of(batchSize));
    }

    @Override
    public SchedulerEvent findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(schedulerEventRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public SchedulerEventId getExternalIdByInternal(SchedulerEventId internalId) {
        return DaoUtil.toEntityId(schedulerEventRepository.getExternalIdById(internalId.getId()), SchedulerEventId::new);
    }

    @Override
    protected Class<SchedulerEventEntity> getEntityClass() {
        return SchedulerEventEntity.class;
    }

    @Override
    protected JpaRepository<SchedulerEventEntity, UUID> getRepository() {
        return schedulerEventRepository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SCHEDULER_EVENT;
    }

}
