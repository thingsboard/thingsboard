// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.AgentProfileInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.agent.AgentProfileDao;
import org.thingsboard.server.dao.model.sql.AgentProfileEntity;
import org.thingsboard.server.dao.model.sql.AgentProfileInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.UUID;

@Component
@SqlDao
@Slf4j
public class JpaAgentProfileDao extends JpaAbstractDao<AgentProfileEntity, AgentProfile> implements AgentProfileDao {

    @Autowired
    private AgentProfileRepository profileRepository;

    @Override
    protected Class<AgentProfileEntity> getEntityClass() {
        return AgentProfileEntity.class;
    }

    @Override
    protected JpaRepository<AgentProfileEntity, UUID> getRepository() {
        return profileRepository;
    }

    @Override
    public AgentProfileInfo findAgentProfileInfoById(UUID agentProfileId) {
        AgentProfileInfoEntity entity = profileRepository.findAgentProfileInfoById(agentProfileId);
        return entity != null ? entity.toData() : null;
    }

    @Override
    public PageData<AgentProfile> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(profileRepository.findByTenantId(
                tenantId,
                pageLink.getTextSearch(),
                DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<AgentProfileInfo> findAgentProfileInfosByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.pageToPageData(profileRepository.findAgentProfileInfosByTenantId(
                        tenantId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink))
                .map(AgentProfileInfoEntity::toData));
    }

    @Override
    public Long countByTenantId(TenantId tenantId) {
        return profileRepository.countByTenantId(tenantId.getId());
    }

    @Override
    public AgentProfile findByProvisionKey(String provisionKey) {
        return profileRepository.findByProvisionKey(provisionKey).map(AgentProfileEntity::toData).orElse(null);
    }

    @Override
    public AgentProfile findByTenantIdAndName(UUID tenantId, String name) {
        return profileRepository.findByTenantIdAndName(tenantId, name).map(AgentProfileEntity::toData).orElse(null);
    }

    @Override
    public AgentProfile findDefaultAgentProfile(TenantId tenantId) {
        AgentProfileEntity entity = profileRepository.findByTenantIdAndDefaultTrue(tenantId.getId());
        return entity != null ? entity.toData() : null;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_PROFILE;
    }
}
