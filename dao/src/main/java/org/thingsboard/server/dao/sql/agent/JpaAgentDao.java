// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.Agent;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.agent.AgentDao;
import org.thingsboard.server.dao.model.sql.AgentEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

@Component
@SqlDao
@Slf4j
public class JpaAgentDao extends JpaAbstractDao<AgentEntity, Agent> implements AgentDao {

    @Autowired
    private AgentRepository agentRepository;

    @Override
    protected Class<AgentEntity> getEntityClass() {
        return AgentEntity.class;
    }

    @Override
    protected JpaRepository<AgentEntity, UUID> getRepository() {
        return agentRepository;
    }

    @Override
    public PageData<Agent> findAgentsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(agentRepository
                .findByTenantIdAndCustomerId(
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<AgentId> findAgentIdsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        Page<UUID> page;
        if (customerId == null) {
            page = agentRepository.findIdsByTenantIdAndNullCustomerId(tenantId, DaoUtil.toPageable(pageLink));
        } else {
            page = agentRepository.findIdsByTenantIdAndCustomerId(tenantId, customerId, DaoUtil.toPageable(pageLink));
        }
        return DaoUtil.pageToPageData(page, AgentId::new);
    }

    @Override
    public PageData<Agent> findAgentsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(agentRepository
                .findByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Agent> findAgentsByEntityGroupId(UUID groupId, PageLink pageLink) {
        return DaoUtil.toPageData(agentRepository
                .findByEntityGroupId(groupId, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Agent> findAgentsByEntityGroupIds(List<UUID> groupIds, PageLink pageLink) {
        return DaoUtil.toPageData(agentRepository
                .findByEntityGroupIds(groupIds, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public Agent findByRoutingKey(UUID tenantId, String routingKey) {
        return DaoUtil.getData(agentRepository.findByRoutingKey(routingKey));
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return agentRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public Long countAgents() {
        return agentRepository.count();
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT;
    }
}
