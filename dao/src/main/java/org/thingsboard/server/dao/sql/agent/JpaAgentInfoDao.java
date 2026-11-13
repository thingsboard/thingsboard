// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.agent.AgentInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.agent.AgentInfoDao;
import org.thingsboard.server.dao.model.sql.AgentInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaAgentInfoDao extends JpaAbstractDao<AgentInfoEntity, AgentInfo> implements AgentInfoDao {

    @Autowired
    private AgentInfoRepository agentInfoRepository;

    @Override
    protected Class<AgentInfoEntity> getEntityClass() {
        return AgentInfoEntity.class;
    }

    @Override
    protected JpaRepository<AgentInfoEntity, UUID> getRepository() {
        return agentInfoRepository;
    }

    @Override
    public AgentInfo findAgentInfoById(TenantId tenantId, UUID agentId) {
        return DaoUtil.getData(agentInfoRepository.findById(agentId).orElse(null));
    }

    @Override
    public List<AgentInfo> findAgentInfosByTenantIdAndIds(UUID tenantId, List<UUID> agentIds) {
        return DaoUtil.convertDataList(agentInfoRepository.findAgentInfosByTenantIdAndIdIn(tenantId, agentIds));
    }

    @Override
    public PageData<AgentInfo> findAgentInfosByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(agentInfoRepository.findAgentInfosByTenantId(
                tenantId,
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<AgentInfo> findAgentInfosByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        return DaoUtil.toPageData(agentInfoRepository.findAgentInfosByTenantIdAndCustomerId(
                tenantId,
                customerId,
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));
    }
}
