// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cache.agent.AgentProfileCacheEvictEvent;
import org.thingsboard.server.cache.agent.AgentProfileCacheKey;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.agent.AgentProfile;
import org.thingsboard.server.common.data.agent.AgentProfileInfo;
import org.thingsboard.server.common.data.agent.AgentProvisionType;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service("AgentProfileDaoService")
@Slf4j
public class BaseAgentProfileService extends AbstractCachedEntityService<AgentProfileCacheKey, AgentProfile, AgentProfileCacheEvictEvent> implements AgentProfileService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_PROFILE_ID = "Incorrect profileId ";

    public static final String HAS_PROFILE_RELATION_TYPE = "HasProfile";

    public static final String DEFAULT_AGENT_PROFILE_NAME = "default";
    public static final String AGENT_PROFILE_WITH_SUCH_NAME_ALREADY_EXISTS = "Agent profile with such name already exists!";

    @Autowired
    private AgentProfileDao profileDao;

    @Autowired
    private RelationService relationService;

    @Autowired
    private DataValidator<AgentProfile> profileValidator;

    @Override
    @TransactionalEventListener
    public void handleEvictEvent(AgentProfileCacheEvictEvent event) {
        List<AgentProfileCacheKey> keys = new ArrayList<>(3);
        keys.add(AgentProfileCacheKey.forName(event.getTenantId(), event.getNewName()));
        if (StringUtils.isNotEmpty(event.getOldName()) && !event.getOldName().equals(event.getNewName())) {
            keys.add(AgentProfileCacheKey.forName(event.getTenantId(), event.getOldName()));
        }
        if (event.isDefaultProfile()) {
            keys.add(AgentProfileCacheKey.forDefaultProfile(event.getTenantId()));
        }
        cache.evict(keys);
    }

    @Override
    @Transactional
    public AgentProfile saveProfile(AgentProfile profile) {
        return saveEntity(profile, () -> doSaveProfile(profile));
    }

    private AgentProfile doSaveProfile(AgentProfile profile) {
        log.trace("Executing saveProfile [{}]", profile);
        if (profile.getProvisionType() == null) {
            profile.setProvisionType(AgentProvisionType.AUTO_INSTALL_PER_APP_TYPE);
        }
        if (profile.getProvisionType() != AgentProvisionType.DISABLED) {
            if (StringUtils.isEmpty(profile.getProvisionKey())) {
                profile.setProvisionKey(StringUtils.randomAlphanumeric(20));
            }
            if (StringUtils.isEmpty(profile.getProvisionSecret())) {
                profile.setProvisionSecret(StringUtils.randomAlphanumeric(20));
            }
        }
        AgentProfile oldProfile = profileValidator.validate(profile, AgentProfile::getTenantId);
        String oldName = oldProfile != null ? oldProfile.getName() : null;
        AgentProfileCacheEvictEvent evictEvent = new AgentProfileCacheEvictEvent(profile.getTenantId(), profile.getName(), oldName,
                profile.isDefault() || (oldProfile != null && oldProfile.isDefault()));
        try {
            AgentProfile saved = profileDao.save(profile.getTenantId(), profile);
            publishEvictEvent(evictEvent);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(saved.getTenantId())
                    .entityId(saved.getId()).entity(saved).oldEntity(oldProfile).created(profile.getId() == null).build());
            return saved;
        } catch (Exception t) {
            handleEvictEvent(evictEvent);
            checkConstraintViolation(t, "agent_profile_name_unq_key", AGENT_PROFILE_WITH_SUCH_NAME_ALREADY_EXISTS);
            checkConstraintViolation(t, "agent_profile_provision_key_unq_key", "Agent profile with such provision key already exists!");
            throw t;
        }
    }

    @Override
    public AgentProfile createDefaultAgentProfile(TenantId tenantId) {
        log.trace("Executing createDefaultAgentProfile tenantId [{}]", tenantId);
        return doCreateAgentProfile(tenantId, DEFAULT_AGENT_PROFILE_NAME, true);
    }

    @Override
    public AgentProfile findOrCreateAgentProfile(TenantId tenantId, String name) {
        log.trace("Executing findOrCreateAgentProfile [{}][{}]", tenantId, name);
        AgentProfile profile = findProfileByName(tenantId, name);
        if (profile != null) {
            return profile;
        }
        boolean isDefault = DEFAULT_AGENT_PROFILE_NAME.equals(name) && findDefaultAgentProfile(tenantId) == null;
        try {
            return doCreateAgentProfile(tenantId, name, isDefault);
        } catch (DataValidationException e) {
            if (AGENT_PROFILE_WITH_SUCH_NAME_ALREADY_EXISTS.equals(e.getMessage())) {
                return findProfileByName(tenantId, name);
            }
            throw e;
        }
    }

    private AgentProfile doCreateAgentProfile(TenantId tenantId, String name, boolean isDefault) {
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        AgentProfile profile = new AgentProfile();
        profile.setTenantId(tenantId);
        profile.setName(name);
        profile.setDescription("Default agent profile");
        profile.setProvisionType(AgentProvisionType.AUTO_INSTALL_PER_APP_TYPE);
        profile.setDefault(isDefault);
        return saveProfile(profile);
    }

    @Override
    public AgentProfile findDefaultAgentProfile(TenantId tenantId) {
        log.trace("Executing findDefaultAgentProfile tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return cache.getAndPutInTransaction(AgentProfileCacheKey.forDefaultProfile(tenantId),
                () -> profileDao.findDefaultAgentProfile(tenantId), true);
    }

    @Override
    public AgentProfileInfo findDefaultAgentProfileInfo(TenantId tenantId) {
        log.trace("Executing findDefaultAgentProfileInfo tenantId [{}]", tenantId);
        AgentProfile defaultProfile = findDefaultAgentProfile(tenantId);
        return defaultProfile != null ? new AgentProfileInfo(defaultProfile) : null;
    }

    @Override
    @Transactional
    public boolean setDefaultAgentProfile(TenantId tenantId, AgentProfileId profileId) {
        log.trace("Executing setDefaultAgentProfile [{}]", profileId);
        validateId(profileId, id -> INCORRECT_PROFILE_ID + id);
        AgentProfile profile = profileDao.findById(tenantId, profileId.getId());
        if (profile == null) {
            throw new DataValidationException("Agent profile not found");
        }
        if (profile.isDefault()) {
            return false;
        }
        AgentProfile previousDefault = findDefaultAgentProfile(tenantId);
        if (previousDefault != null && previousDefault.getId().equals(profile.getId())) {
            return false;
        }
        profile.setDefault(true);
        if (previousDefault != null) {
            previousDefault.setDefault(false);
            profileDao.save(tenantId, previousDefault);
            publishEvictEvent(new AgentProfileCacheEvictEvent(previousDefault.getTenantId(), previousDefault.getName(), null, true));
        }
        profileDao.save(tenantId, profile);
        publishEvictEvent(new AgentProfileCacheEvictEvent(profile.getTenantId(), profile.getName(), null, true));
        return true;
    }

    @Override
    public AgentProfile findProfileById(TenantId tenantId, AgentProfileId profileId) {
        log.trace("Executing findProfileById [{}]", profileId);
        validateId(profileId, id -> INCORRECT_PROFILE_ID + id);
        AgentProfile profile = profileDao.findById(tenantId, profileId.getId());
        if (profile == null || TenantId.SYS_TENANT_ID.equals(tenantId) || tenantId.equals(profile.getTenantId())) {
            return profile;
        }
        return null;
    }

    @Override
    public AgentProfile findProfileByName(TenantId tenantId, String name) {
        log.trace("Executing findProfileByName [{}][{}]", tenantId, name);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        return cache.getAndPutInTransaction(AgentProfileCacheKey.forName(tenantId, name),
                () -> profileDao.findByTenantIdAndName(tenantId.getId(), name), true);
    }

    @Override
    public AgentProfile findProfileByProvisionKey(String provisionKey) {
        log.trace("Executing findProfileByProvisionKey");
        if (StringUtils.isEmpty(provisionKey)) {
            return null;
        }
        return profileDao.findByProvisionKey(provisionKey);
    }

    @Override
    public AgentProfileInfo findAgentProfileInfoById(TenantId tenantId, AgentProfileId profileId) {
        log.trace("Executing findAgentProfileInfoById [{}]", profileId);
        validateId(profileId, id -> INCORRECT_PROFILE_ID + id);
        return profileDao.findAgentProfileInfoById(profileId.getId());
    }

    @Override
    public PageData<AgentProfile> findAgentProfilesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAgentProfilesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return profileDao.findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<AgentProfileInfo> findAgentProfileInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAgentProfileInfosByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return profileDao.findAgentProfileInfosByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    @Transactional
    public void deleteProfile(TenantId tenantId, AgentProfileId profileId) {
        log.trace("Executing deleteProfile [{}]", profileId);
        validateId(profileId, id -> INCORRECT_PROFILE_ID + id);
        deleteEntity(tenantId, profileId, false);
    }

    private void removeAgentProfile(TenantId tenantId, AgentProfile profile) {
        AgentProfileId profileId = profile.getId();
        try {
            profileDao.removeById(tenantId, profileId.getId());
            publishEvictEvent(new AgentProfileCacheEvictEvent(profile.getTenantId(), profile.getName(), null, profile.isDefault()));
            eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(profileId).entity(profile).build());
        } catch (Exception e) {
            checkConstraintViolation(e, "fk_agent_agent_profile", "The agent profile referenced by the agents cannot be deleted!");
            throw e;
        }
    }

    @Override
    public void assignAppProfileToAgentProfile(TenantId tenantId, AgentProfileId agentProfileId, AgentAppProfileId applicationProfileId) {
        if (relationService.checkRelation(tenantId, agentProfileId, applicationProfileId, HAS_PROFILE_RELATION_TYPE, RelationTypeGroup.AGENT)) {
            throw new DataValidationException("App Profile is already assigned to this agent profile!");
        }
        relationService.saveRelation(tenantId, new EntityRelation(agentProfileId, applicationProfileId, HAS_PROFILE_RELATION_TYPE, RelationTypeGroup.AGENT));
    }

    @Override
    public void unassignAppProfileFromAgentProfile(TenantId tenantId, AgentProfileId agentProfileId, AgentAppProfileId applicationProfileId) {
        relationService.deleteRelation(tenantId, agentProfileId, applicationProfileId, HAS_PROFILE_RELATION_TYPE, RelationTypeGroup.AGENT);
    }

    @Override
    public void setAppProfileRelatesOnAutoDiscovery(TenantId tenantId, AgentProfileId agentProfileId, AgentAppProfileId appProfileId, boolean enabled) {
        EntityRelation relation = relationService.getRelation(tenantId, agentProfileId, appProfileId, HAS_PROFILE_RELATION_TYPE, RelationTypeGroup.AGENT);
        if (relation == null) {
            throw new DataValidationException("App Profile is not assigned to this agent profile!");
        }
        ObjectNode additionalInfo = JacksonUtil.asObject(relation.getAdditionalInfo());
        additionalInfo.put(RELATES_ON_AUTO_DISCOVERY, enabled);
        relation.setAdditionalInfo(additionalInfo);
        relationService.saveRelation(tenantId, relation);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findProfileById(tenantId, new AgentProfileId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        ListenableFuture<AgentProfile> future = profileDao.findByIdAsync(tenantId, entityId.getId());
        return FluentFuture.from(future)
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return profileDao.countByTenantId(tenantId);
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        AgentProfile profile = profileDao.findById(tenantId, id.getId());
        if (profile == null) {
            return;
        }
        if (!force && profile.isDefault()) {
            throw new DataValidationException("Deletion of Default Agent Profile is prohibited!");
        }
        removeAgentProfile(tenantId, profile);
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
        return EntityType.AGENT_PROFILE;
    }

    private final PaginatedRemover<TenantId, AgentProfile> tenantProfilesRemover = new PaginatedRemover<>() {
        @Override
        protected PageData<AgentProfile> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return profileDao.findByTenantId(id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, AgentProfile entity) {
            removeAgentProfile(tenantId, entity);
        }
    };
}
