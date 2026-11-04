// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.agent.AgentBulkActionStatus;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentBulkActionId;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.agent.AgentBulkActionDao;
import org.thingsboard.server.dao.model.sql.AgentBulkActionEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@Component
@SqlDao
@Slf4j
public class JpaAgentBulkActionDao extends JpaAbstractDao<AgentBulkActionEntity, AgentBulkAction> implements AgentBulkActionDao {

    @Autowired
    private AgentBulkActionRepository repository;

    @Override
    protected Class<AgentBulkActionEntity> getEntityClass() {
        return AgentBulkActionEntity.class;
    }

    @Override
    protected JpaRepository<AgentBulkActionEntity, UUID> getRepository() {
        return repository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_BULK_ACTION;
    }

    @Override
    public int cleanUpExpiredBulkActions(long expirationTs, int batchSize) {
        int removedEvents;
        do {
            removedEvents = repository.deleteEventsByBulkActionCreatedTimeBeforeBatch(expirationTs, batchSize);
        } while (removedEvents >= batchSize);
        int removed = 0;
        int batchRemoved;
        do {
            batchRemoved = repository.deleteBulkActionsCreatedTimeBeforeBatch(expirationTs, batchSize);
            removed += batchRemoved;
        } while (batchRemoved >= batchSize);
        return removed;
    }

    @Override
    public int failIfStillStuck(AgentBulkActionId id, AgentBulkActionStatus status, String errorMsg) {
        return repository.failIfStillStuck(id.getId(), status, errorMsg);
    }

    @Override
    public PageData<AgentBulkAction> findStuckBulkActions(long threshold, PageLink pageLink) {
        return DaoUtil.pageToPageData(
                repository.findStuckBulkActions(threshold, DaoUtil.toPageable(pageLink))
                        .map(AgentBulkActionEntity::toData));
    }

    @Override
    public PageData<AgentBulkAction> findByAgentProfileId(TenantId tenantId, AgentProfileId agentProfileId, PageLink pageLink) {
        return DaoUtil.pageToPageData(
                repository.findByAgentProfileId(tenantId.getId(), agentProfileId.getId(), DaoUtil.toPageable(pageLink))
                        .map(AgentBulkActionEntity::toData));
    }

    @Override
    public PageData<AgentBulkAction> findByAgentProfileIdAndApplicationProfileId(TenantId tenantId,
                                                                                 AgentProfileId agentProfileId,
                                                                                 AgentAppProfileId applicationProfileId,
                                                                                 PageLink pageLink) {
        return DaoUtil.pageToPageData(
                repository.findByAgentProfileIdAndApplicationProfileId(
                        tenantId.getId(),
                        agentProfileId.getId(),
                        applicationProfileId.getId(),
                        DaoUtil.toPageable(pageLink))
                        .map(AgentBulkActionEntity::toData));
    }
}
