// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.agent;

import jakarta.persistence.Tuple;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.AgentApplicationInfo;
import org.thingsboard.server.common.data.agent.config.AgentAppConfigType;
import org.thingsboard.server.common.data.id.AgentApplicationId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.agent.AgentApplicationDao;
import org.thingsboard.server.dao.model.sql.AgentApplicationEntity;
import org.thingsboard.server.dao.model.sql.AgentApplicationInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

@Component
@SqlDao
@Slf4j
public class JpaAgentApplicationDao extends JpaAbstractDao<AgentApplicationEntity, AgentApplication> implements AgentApplicationDao {

    @Autowired
    private AgentApplicationRepository agentApplicationRepository;

    @Autowired
    private AppTemplateRegistry templateRegistry;

    private AgentApplicationInfo toInfo(AgentApplicationInfoEntity entity) {
        AgentApplicationInfo info = entity.toData();
        // nextVersion comes from the external version graph, not the DB; enrich from the in-memory registry.
        AgentAppConfigType configType = info.getConfig() != null ? info.getConfig().getType() : AgentAppConfigType.DOCKER_COMPOSE;
        info.setNextVersion(templateRegistry.next(info.getAppType(), configType, info.getTemplateVersion()));
        return info;
    }

    @Override
    protected Class<AgentApplicationEntity> getEntityClass() {
        return AgentApplicationEntity.class;
    }

    @Override
    protected JpaRepository<AgentApplicationEntity, UUID> getRepository() {
        return agentApplicationRepository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_APPLICATION;
    }

    @Override
    public AgentApplication findByIdForUpdate(TenantId tenantId, UUID id) {
        return DaoUtil.getData(agentApplicationRepository.findByIdForUpdate(id));
    }

    @Override
    public void removeByAgentId(TenantId tenantId, UUID agentId) {
        agentApplicationRepository.deleteByAgentId(tenantId.getId(), agentId);
    }

    @Override
    public List<AgentApplication> findByAgentId(TenantId tenantId, UUID agentId) {
        return DaoUtil.convertDataList(agentApplicationRepository.findByTenantIdAndAgentId(tenantId.getId(), agentId));
    }

    @Override
    public PageData<AgentApplication> findByAgentId(TenantId tenantId, UUID agentId, PageLink pageLink) {
        return DaoUtil.toPageData(agentApplicationRepository.findByAgentId(
                tenantId.getId(),
                agentId,
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public AgentApplication findByProjectName(TenantId tenantId, AgentId agentId, String projectName) {
        return DaoUtil.getData(agentApplicationRepository.findByTenantIdAndAgentIdAndProjectName(tenantId.getId(), agentId.getId(), projectName));
    }

    @Override
    public AgentApplication findByEventId(TenantId tenantId, UUID eventId) {
        return DaoUtil.getData(agentApplicationRepository.findByEventId(tenantId.getId(), eventId));
    }

    @Override
    public AgentApplicationInfo findInfoById(TenantId tenantId, UUID id) {
        AgentApplicationInfoEntity entity = agentApplicationRepository.findInfoById(tenantId.getId(), id);
        return entity != null ? toInfo(entity) : null;
    }

    @Override
    public PageData<AgentApplicationInfo> findInfosByAgentId(TenantId tenantId, UUID agentId, PageLink pageLink) {
        return DaoUtil.pageToPageData(agentApplicationRepository.findInfosByAgentId(
                tenantId.getId(), agentId, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink))
                .map(this::toInfo));
    }

    @Override
    public PageData<AgentApplicationInfo> findByApplicationProfileIdAndAgentProfileId(TenantId tenantId, UUID profileId, UUID agentProfileId, PageLink pageLink) {
        return DaoUtil.pageToPageData(agentApplicationRepository.findByApplicationProfileIdAndAgentProfileId(
                tenantId.getId(), profileId, agentProfileId, DaoUtil.toPageable(pageLink))
                .map(this::toInfo));
    }

    @Override
    public PageData<AgentApplication> findByEntityGroupId(UUID groupId, PageLink pageLink) {
        return DaoUtil.toPageData(agentApplicationRepository
                .findByEntityGroupId(groupId, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<AgentApplication> findByEntityGroupIds(List<UUID> groupIds, PageLink pageLink) {
        return DaoUtil.toPageData(agentApplicationRepository
                .findByEntityGroupIds(groupIds, pageLink.getTextSearch(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public AgentApplication findByRelatedEntity(TenantId tenantId, UUID relatedEntityId) {
        return DaoUtil.getData(agentApplicationRepository.findByRelatedEntityId(tenantId.getId(), relatedEntityId));
    }

    @Override
    public List<UUID> findManagedRelatedEntityIds(TenantId tenantId, String relatedEntityType) {
        return agentApplicationRepository.findManagedRelatedEntityIds(tenantId.getId(), relatedEntityType);
    }

    @Override
    public PageData<EntityInfo> findRelatedEntityCandidates(TenantId tenantId, EntityType relatedEntityType, UUID currentEntityId, PageLink pageLink) {
        Page<Tuple> candidates = switch (relatedEntityType) {
            case EDGE -> agentApplicationRepository.findEdgeCandidates(
                    tenantId.getId(), pageLink.getTextSearch(), currentEntityId, DaoUtil.toPageable(pageLink));
            case DEVICE -> agentApplicationRepository.findGatewayDeviceCandidates(
                    tenantId.getId(), pageLink.getTextSearch(), currentEntityId, DaoUtil.toPageable(pageLink));
            default -> throw new IllegalArgumentException("Unsupported related entity type: " + relatedEntityType);
        };
        return DaoUtil.pageToPageData(candidates, candidate ->
                new EntityInfo(candidate.get("id", UUID.class), relatedEntityType.name(), candidate.get("name", String.class)));
    }

    @Override
    public int promoteDesiredTemplate(TenantId tenantId, AgentApplicationId applicationId, String templateVersion) {
        return agentApplicationRepository.promoteDesiredTemplate(tenantId.getId(), applicationId.getId(), templateVersion);
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return agentApplicationRepository.countByTenantId(tenantId.getId());
    }
}
