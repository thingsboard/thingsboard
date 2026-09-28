// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.agent.AgentAppProfileCacheEvictEvent;
import org.thingsboard.server.cache.agent.AgentAppProfileCacheKey;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentAppProfileInfo;
import org.thingsboard.server.common.data.agent.AgentAppProfileRelationInfo;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service("AgentAppProfileDaoService")
@Slf4j
public class BaseAgentAppProfileService extends AbstractCachedEntityService<AgentAppProfileCacheKey, AgentAppProfile, AgentAppProfileCacheEvictEvent> implements AgentAppProfileService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_PROFILE_ID = "Incorrect profileId ";

    @Autowired
    private AgentAppProfileDao profileDao;

    @Autowired
    private DataValidator<AgentAppProfile> profileValidator;

    @Override
    @TransactionalEventListener
    public void handleEvictEvent(AgentAppProfileCacheEvictEvent event) {
        if (event.getProfileId() != null) {
            cache.evict(List.of(AgentAppProfileCacheKey.forId(event.getProfileId())));
        }
    }

    @Override
    @Transactional
    public AgentAppProfile saveProfile(AgentAppProfile profile) {
        return saveEntity(profile, () -> doSaveProfile(profile));
    }

    private AgentAppProfile doSaveProfile(AgentAppProfile profile) {
        log.trace("Executing saveProfile [{}]", profile);
        AgentAppProfile oldProfile = profileValidator.validate(profile, AgentAppProfile::getTenantId);
        AgentAppProfileCacheEvictEvent evictEvent = new AgentAppProfileCacheEvictEvent(profile.getId());
        try {
            AgentAppProfile saved = profileDao.save(profile.getTenantId(), profile);
            publishEvictEvent(new AgentAppProfileCacheEvictEvent(saved.getId()));
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(saved.getTenantId())
                    .entityId(saved.getId()).entity(saved).oldEntity(oldProfile).created(profile.getId() == null).build());
            return saved;
        } catch (Exception t) {
            handleEvictEvent(evictEvent);
            checkConstraintViolation(t, "agent_app_profile_name_unq_key", "Agent application profile with such name already exists!");
            throw t;
        }
    }

    @Override
    public AgentAppProfile findProfileById(TenantId tenantId, AgentAppProfileId profileId) {
        log.trace("Executing findProfileById [{}]", profileId);
        validateId(profileId, id -> INCORRECT_PROFILE_ID + id);
        AgentAppProfile profile = cache.getAndPutInTransaction(AgentAppProfileCacheKey.forId(profileId),
                () -> profileDao.findById(tenantId, profileId.getId()), true);
        if (profile == null || TenantId.SYS_TENANT_ID.equals(tenantId) || tenantId.equals(profile.getTenantId())) {
            return profile;
        }
        return null;
    }

    @Override
    public AgentAppProfileInfo findProfileInfoById(TenantId tenantId, AgentAppProfileId profileId) {
        log.trace("Executing findProfileInfoById [{}]", profileId);
        validateId(profileId, id -> INCORRECT_PROFILE_ID + id);
        return profileDao.findInfoById(profileId.getId());
    }

    @Override
    public PageData<AgentAppProfile> findProfilesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findProfilesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return profileDao.findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public List<AgentAppProfileInfo> findProfileInfosByTenantIdAndAppType(TenantId tenantId, AgentApplicationType appType) {
        log.trace("Executing findProfileInfosByTenantIdAndAppType, tenantId [{}], appType [{}]", tenantId, appType);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return profileDao.findInfosByTenantIdAndAppType(tenantId.getId(), appType);
    }

    @Override
    public List<AgentAppProfile> findProfilesByTenantIdAndAppTypeAndTemplateVersion(TenantId tenantId, AgentApplicationType appType, String templateVersion) {
        log.trace("Executing findProfilesByTenantIdAndAppTypeAndTemplateVersion, tenantId [{}], appType [{}], templateVersion [{}]", tenantId, appType, templateVersion);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return profileDao.findByTenantIdAndAppTypeAndTemplateVersion(tenantId.getId(), appType, templateVersion);
    }

    @Override
    public List<AgentAppProfile> findProfilesByTenantIdAndIds(TenantId tenantId, List<AgentAppProfileId> profileIds) {
        log.trace("Executing findProfilesByTenantIdAndIds, tenantId [{}], profileIds [{}]", tenantId, profileIds);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateIds(profileIds, ids -> "Incorrect profileIds " + ids);
        return profileDao.findByTenantIdAndIds(tenantId.getId(), DaoUtil.toUUIDs(profileIds));
    }

    @Override
    public List<AgentAppProfileRelationInfo> findProfileRelationInfosByAgentProfileId(TenantId tenantId, AgentProfileId agentProfileId) {
        log.trace("Executing findProfileRelationInfosByAgentProfileId, tenantId [{}], agentProfileId [{}]", tenantId, agentProfileId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(agentProfileId, id -> INCORRECT_PROFILE_ID + id);
        return profileDao.findRelationInfosByAgentProfileId(agentProfileId.getId(), BaseAgentProfileService.HAS_PROFILE_RELATION_TYPE);
    }

    @Override
    public List<AgentAppProfileRelationInfo> findProfileRelationInfosByAgentProfileIdAndAppTypeAndTemplateVersion(TenantId tenantId, AgentProfileId agentProfileId, AgentApplicationType appType, String templateVersion) {
        log.trace("Executing findProfileRelationInfosByAgentProfileIdAndAppTypeAndTemplateVersion, tenantId [{}], agentProfileId [{}], appType [{}], templateVersion [{}]", tenantId, agentProfileId, appType, templateVersion);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(agentProfileId, id -> INCORRECT_PROFILE_ID + id);
        return profileDao.findRelationInfosByAgentProfileIdAndAppTypeAndTemplateVersion(agentProfileId.getId(), appType, templateVersion, BaseAgentProfileService.HAS_PROFILE_RELATION_TYPE);
    }

    @Override
    public List<AgentAppProfile> findUninstalledAppProfilesForAgentProfile(TenantId tenantId, AgentProfileId agentProfileId, AgentId agentId) {
        log.trace("Executing findUninstalledAppProfilesForAgentProfile, agentProfileId [{}], agentId [{}]", agentProfileId, agentId);
        if (agentProfileId == null || agentId == null) {
            return Collections.emptyList();
        }
        return profileDao.findUninstalledAppProfilesForAgentProfile(agentProfileId.getId(), agentId.getId(),
                BaseAgentProfileService.HAS_PROFILE_RELATION_TYPE);
    }

    @Override
    public List<AgentAppProfile> findUninstalledAppProfilesByAppTypeForAgentProfile(TenantId tenantId, AgentProfileId agentProfileId, AgentId agentId) {
        log.trace("Executing findUninstalledAppProfilesByAppTypeForAgentProfile, agentProfileId [{}], agentId [{}]", agentProfileId, agentId);
        if (agentProfileId == null || agentId == null) {
            return Collections.emptyList();
        }
        return profileDao.findUninstalledAppProfilesByAppTypeForAgentProfile(agentProfileId.getId(), agentId.getId(),
                BaseAgentProfileService.HAS_PROFILE_RELATION_TYPE);
    }

    @Override
    @Transactional
    public void deleteProfile(TenantId tenantId, AgentAppProfileId profileId) {
        log.trace("Executing deleteProfile [{}]", profileId);
        validateId(profileId, id -> INCORRECT_PROFILE_ID + id);
        deleteEntity(tenantId, profileId, false);
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        AgentAppProfile profile = profileDao.findById(tenantId, id.getId());
        if (profile == null) {
            return;
        }
        removeProfile(tenantId, profile);
    }

    private void removeProfile(TenantId tenantId, AgentAppProfile profile) {
        AgentAppProfileId profileId = profile.getId();
        try {
            profileDao.removeById(tenantId, profileId.getId());
            publishEvictEvent(new AgentAppProfileCacheEvictEvent(profileId));
            eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(profileId).entity(profile).build());
        } catch (Exception t) {
            checkConstraintViolation(t, "fk_agent_app_profile", "The application profile referenced by agent applications cannot be deleted!");
            throw t;
        }
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findProfileById(tenantId, new AgentAppProfileId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        ListenableFuture<AgentAppProfile> future = profileDao.findByIdAsync(tenantId, entityId.getId());
        return FluentFuture.from(future)
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return profileDao.countByTenantId(tenantId);
    }

    @Override
    @Transactional
    public void deleteByTenantId(TenantId tenantId) {
        log.trace("Executing deleteByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        tenantProfilesRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.AGENT_APP_PROFILE;
    }

    private final PaginatedRemover<TenantId, AgentAppProfile> tenantProfilesRemover = new PaginatedRemover<>() {
        @Override
        protected PageData<AgentAppProfile> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return profileDao.findByTenantId(id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, AgentAppProfile entity) {
            removeProfile(tenantId, entity);
        }
    };
}
