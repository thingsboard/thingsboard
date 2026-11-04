// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentAppProfileInfo;
import org.thingsboard.server.common.data.agent.AgentAppProfileRelationInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.agent.AgentAppProfileDao;
import org.thingsboard.server.dao.model.sql.AgentAppProfileEntity;
import org.thingsboard.server.dao.model.sql.AgentAppProfileInfoEntity;
import org.thingsboard.server.dao.model.sql.AgentAppProfileRelationInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;

@Component
@SqlDao
@Slf4j
public class JpaAgentAppProfileDao extends JpaAbstractDao<AgentAppProfileEntity, AgentAppProfile> implements AgentAppProfileDao {

    @Autowired
    private AgentAppProfileRepository profileRepository;

    @Override
    protected Class<AgentAppProfileEntity> getEntityClass() {
        return AgentAppProfileEntity.class;
    }

    @Override
    protected JpaRepository<AgentAppProfileEntity, UUID> getRepository() {
        return profileRepository;
    }

    @Override
    public PageData<AgentAppProfile> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(profileRepository.findByTenantId(
                tenantId,
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public AgentAppProfileInfo findInfoById(UUID profileId) {
        AgentAppProfileInfoEntity entity = profileRepository.findInfoById(profileId);
        return entity != null ? entity.toData() : null;
    }

    @Override
    public List<AgentAppProfileInfo> findInfosByTenantIdAndAppType(UUID tenantId, AgentApplicationType appType) {
        return profileRepository.findInfosByTenantIdAndAppType(tenantId, appType).stream()
                .map(AgentAppProfileInfoEntity::toData)
                .toList();
    }

    @Override
    public List<AgentAppProfile> findByTenantIdAndAppTypeAndTemplateVersion(UUID tenantId, AgentApplicationType appType, String templateVersion) {
        return DaoUtil.convertDataList(profileRepository.findByTenantIdAndAppTypeAndTemplateVersion(tenantId, appType, templateVersion));
    }

    @Override
    public List<AgentAppProfile> findByTenantIdAndIds(UUID tenantId, List<UUID> profileIds) {
        return DaoUtil.convertDataList(profileRepository.findByTenantIdAndIdIn(tenantId, profileIds));
    }

    @Override
    public List<AgentAppProfileRelationInfo> findRelationInfosByAgentProfileId(UUID agentProfileId, String relationType) {
        return profileRepository.findRelationInfosByAgentProfileId(agentProfileId, relationType).stream()
                .map(AgentAppProfileRelationInfoEntity::toData)
                .toList();
    }

    @Override
    public List<AgentAppProfileRelationInfo> findRelationInfosByAgentProfileIdAndAppTypeAndTemplateVersion(UUID agentProfileId, AgentApplicationType appType, String templateVersion, String relationType) {
        return profileRepository.findRelationInfosByAgentProfileIdAndAppTypeAndTemplateVersion(agentProfileId, appType, templateVersion, relationType).stream()
                .map(AgentAppProfileRelationInfoEntity::toData)
                .toList();
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return profileRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public List<AgentAppProfile> findUninstalledAppProfilesForAgentProfile(UUID agentProfileId, UUID agentId, String relationType) {
        return DaoUtil.convertDataList(profileRepository.findUninstalledAppProfilesForAgentProfile(agentProfileId, agentId, relationType));
    }

    @Override
    public List<AgentAppProfile> findUninstalledAppProfilesByAppTypeForAgentProfile(UUID agentProfileId, UUID agentId, String relationType) {
        return DaoUtil.convertDataList(profileRepository.findUninstalledAppProfilesByAppTypeForAgentProfile(agentProfileId, agentId, relationType));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_APP_PROFILE;
    }
}
