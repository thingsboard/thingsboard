// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.AgentAppUnit;
import org.thingsboard.server.common.data.agent.AgentAppUnitFilter;
import org.thingsboard.server.common.data.agent.AgentAppUnitInfo;
import org.thingsboard.server.common.data.agent.AgentAppUnitType;
import org.thingsboard.server.common.data.id.AgentAppUnitId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.agent.AgentAppUnitDao;
import org.thingsboard.server.dao.model.sql.AgentAppUnitEntity;
import org.thingsboard.server.dao.model.sql.AgentAppUnitInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@SqlDao
@Slf4j
public class JpaAgentAppUnitDao extends JpaAbstractDao<AgentAppUnitEntity, AgentAppUnit> implements AgentAppUnitDao {

    @Autowired
    private AgentAppUnitRepository agentAppUnitRepository;

    @Override
    protected Class<AgentAppUnitEntity> getEntityClass() {
        return AgentAppUnitEntity.class;
    }

    @Override
    protected JpaRepository<AgentAppUnitEntity, UUID> getRepository() {
        return agentAppUnitRepository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_APP_UNIT;
    }

    private static final Map<String, String> UNIT_COLUMN_MAP = Map.of(
            "createdTime", "created_time",
            "identifier", "identifier",
            "type", "type"
    );

    @Override
    public List<AgentAppUnit> findByAgentApplicationId(TenantId tenantId, UUID agentApplicationId) {
        return DaoUtil.convertDataList(agentAppUnitRepository.findByAgentApplicationId(agentApplicationId));
    }

    @Override
    public AgentAppUnit findByAgentAndProjectAndIdentifier(TenantId tenantId, AgentId agentId, String projectName,
                                                           String identifier, AgentAppUnitType type) {
        AgentAppUnitEntity entity = agentAppUnitRepository.findByAgentAndProjectAndIdentifier(
                tenantId.getId(), agentId.getId(), projectName, identifier, type);
        return entity == null ? null : entity.toData();
    }

    @Override
    public AgentAppUnitInfo findAgentAppUnitInfoById(TenantId tenantId, AgentAppUnitId agentAppUnitId) {
        AgentAppUnitInfoEntity entity = agentAppUnitRepository.findInfoById(tenantId.getId(), agentAppUnitId.getId());
        return entity == null ? null : entity.toData();
    }

    @Override
    public PageData<AgentAppUnit> findByFilter(AgentAppUnitFilter filter, PageLink pageLink) {
        String ts = pageLink.getTextSearch();
        String textSearch = (ts == null || ts.isBlank()) ? null : ts.trim();
        return DaoUtil.pageToPageData(
                agentAppUnitRepository.findByFilter(
                                filter.getApplicationId().getId(),
                                filter.getType() != null ? filter.getType().name() : null,
                                textSearch,
                                DaoUtil.toPageable(pageLink, UNIT_COLUMN_MAP))
                        .map(AgentAppUnitEntity::toData)
        );
    }

    @Override
    public void removeByAgentApplicationId(TenantId tenantId, UUID agentApplicationId) {
        agentAppUnitRepository.deleteByAgentApplicationId(agentApplicationId);
    }

}
