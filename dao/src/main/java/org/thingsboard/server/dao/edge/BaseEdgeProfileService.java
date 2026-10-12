// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.edge;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.cache.edge.EdgeProfileCacheEvictEvent;
import org.thingsboard.server.cache.edge.EdgeProfileCacheKey;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.edge.EdgeProfile;
import org.thingsboard.server.common.data.edge.EdgeProfileData;
import org.thingsboard.server.common.data.edge.EdgeProfileEntityGroup;
import org.thingsboard.server.common.data.id.EdgeProfileId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.id.SchedulerEventId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.rule.RuleChainDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service("EdgeProfileDaoService")
@Slf4j
public class BaseEdgeProfileService extends AbstractCachedEntityService<EdgeProfileCacheKey, EdgeProfile, EdgeProfileCacheEvictEvent> implements EdgeProfileService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_EDGE_PROFILE_ID = "Incorrect edgeProfileId ";
    public static final String DEFAULT_EDGE_PROFILE_NAME = "default";
    public static final String EDGE_PROFILE_WITH_SUCH_NAME_ALREADY_EXISTS = "Edge profile with such name already exists!";

    @Autowired
    private EdgeProfileDao edgeProfileDao;

    @Autowired
    private EdgeDao edgeDao;

    @Autowired
    private RuleChainDao ruleChainDao;

    @Autowired
    @Lazy
    private EdgeService edgeService;

    @Autowired
    @Lazy
    private AgentAppProfileService agentAppProfileService;

    @Autowired
    private DataValidator<EdgeProfile> edgeProfileValidator;

    @Override
    @TransactionalEventListener
    public void handleEvictEvent(EdgeProfileCacheEvictEvent event) {
        List<EdgeProfileCacheKey> keys = new ArrayList<>(4);
        if (event.getId() != null) {
            keys.add(EdgeProfileCacheKey.forId(event.getTenantId(), event.getId()));
        }
        if (StringUtils.isNotEmpty(event.getNewName())) {
            keys.add(EdgeProfileCacheKey.forName(event.getTenantId(), event.getNewName()));
        }
        if (StringUtils.isNotEmpty(event.getOldName()) && !event.getOldName().equals(event.getNewName())) {
            keys.add(EdgeProfileCacheKey.forName(event.getTenantId(), event.getOldName()));
        }
        if (event.isDefaultProfile()) {
            keys.add(EdgeProfileCacheKey.forDefaultProfile(event.getTenantId()));
        }
        cache.evict(keys);
    }

    @Override
    @Transactional
    public EdgeProfile saveEdgeProfile(EdgeProfile profile) {
        return saveEdgeProfile(profile, true);
    }

    @Override
    @Transactional
    public EdgeProfile saveEdgeProfile(EdgeProfile profile, boolean doValidate) {
        return saveEntity(profile, () -> doSaveEdgeProfile(profile, doValidate));
    }

    private EdgeProfile doSaveEdgeProfile(EdgeProfile profile, boolean doValidate) {
        log.trace("Executing saveEdgeProfile [{}]", profile);
        dedupe(profile.getProfileData());
        EdgeProfile oldProfile;
        if (doValidate) {
            oldProfile = edgeProfileValidator.validate(profile, EdgeProfile::getTenantId);
        } else {
            oldProfile = profile.getId() != null ? edgeProfileDao.findById(profile.getTenantId(), profile.getId().getId()) : null;
        }
        String oldName = oldProfile != null ? oldProfile.getName() : null;
        EdgeProfileCacheEvictEvent evictEvent = new EdgeProfileCacheEvictEvent(profile.getTenantId(), profile.getId(), profile.getName(), oldName,
                profile.isDefault() || (oldProfile != null && oldProfile.isDefault()));
        try {
            EdgeProfile saved = edgeProfileDao.save(profile.getTenantId(), profile);
            publishEvictEvent(evictEvent);
            if (oldName != null && !oldName.equals(saved.getName())) {
                edgeService.updateTypeByEdgeProfileId(saved.getTenantId(), saved.getId(), saved.getName());
            }
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(saved.getTenantId())
                    .entityId(saved.getId()).entity(saved).oldEntity(oldProfile).created(profile.getId() == null).build());
            return saved;
        } catch (Exception t) {
            handleEvictEvent(evictEvent);
            checkConstraintViolation(t, "edge_profile_name_unq_key", EDGE_PROFILE_WITH_SUCH_NAME_ALREADY_EXISTS);
            throw t;
        }
    }

    private void dedupe(EdgeProfileData data) {
        data.setRuleChainIds(new ArrayList<>(new LinkedHashSet<>(data.getRuleChainIds())));
        data.setIntegrationIds(new ArrayList<>(new LinkedHashSet<>(data.getIntegrationIds())));
        data.setSchedulerEventIds(new ArrayList<>(new LinkedHashSet<>(data.getSchedulerEventIds())));
        List<EdgeProfileEntityGroup> groups = new ArrayList<>();
        for (EdgeProfileEntityGroup row : data.getEntityGroups()) {
            if (groups.stream().noneMatch(g -> g.getEntityGroupId() != null && g.getEntityGroupId().equals(row.getEntityGroupId()))) {
                groups.add(row);
            }
        }
        data.setEntityGroups(groups);
    }

    @Override
    public EdgeProfile findEdgeProfileById(TenantId tenantId, EdgeProfileId edgeProfileId) {
        log.trace("Executing findEdgeProfileById [{}]", edgeProfileId);
        validateId(edgeProfileId, id -> INCORRECT_EDGE_PROFILE_ID + id);
        EdgeProfile profile = cache.getAndPutInTransaction(EdgeProfileCacheKey.forId(tenantId, edgeProfileId),
                () -> edgeProfileDao.findById(tenantId, edgeProfileId.getId()), true);
        if (profile == null || TenantId.SYS_TENANT_ID.equals(tenantId) || tenantId.equals(profile.getTenantId())) {
            return profile;
        }
        return null;
    }

    @Override
    public EdgeProfile findEdgeProfileByName(TenantId tenantId, String name) {
        log.trace("Executing findEdgeProfileByName [{}][{}]", tenantId, name);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        if (StringUtils.isEmpty(name)) {
            return null;
        }
        return cache.getAndPutInTransaction(EdgeProfileCacheKey.forName(tenantId, name),
                () -> edgeProfileDao.findByTenantIdAndName(tenantId.getId(), name), true);
    }

    @Override
    public EdgeProfile findDefaultEdgeProfile(TenantId tenantId) {
        log.trace("Executing findDefaultEdgeProfile tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return cache.getAndPutInTransaction(EdgeProfileCacheKey.forDefaultProfile(tenantId),
                () -> edgeProfileDao.findDefaultEdgeProfile(tenantId.getId()), true);
    }

    @Override
    public EdgeProfile createDefaultEdgeProfile(TenantId tenantId) {
        log.trace("Executing createDefaultEdgeProfile tenantId [{}]", tenantId);
        EdgeProfile profile = new EdgeProfile();
        profile.setTenantId(tenantId);
        profile.setName(DEFAULT_EDGE_PROFILE_NAME);
        profile.setDescription("Default edge profile");
        profile.setDefault(true);
        RuleChain root = ruleChainDao.findRootRuleChainByTenantIdAndType(tenantId.getId(), RuleChainType.EDGE);
        if (root != null) {
            profile.getProfileData().getRuleChainIds().add(root.getId());
            profile.setRootRuleChainId(root.getId());
        }
        return saveEdgeProfile(profile);
    }

    @Override
    @Transactional
    public EdgeProfile findOrCreateEdgeProfile(TenantId tenantId, String name) {
        log.trace("Executing findOrCreateEdgeProfile [{}][{}]", tenantId, name);
        EdgeProfile existing = findEdgeProfileByName(tenantId, name);
        if (existing != null) {
            return existing;
        }
        // held until the caller's transaction ends, so two creators of one name serialize instead of racing the unique constraint
        edgeProfileDao.lockNameForCreate(tenantId.getId(), name);
        // read the table, not the cache: the winner's cache evict only runs after its commit, so a cached null can still be stale
        existing = edgeProfileDao.findByTenantIdAndName(tenantId.getId(), name);
        if (existing != null) {
            return existing;
        }
        EdgeProfile profile = new EdgeProfile();
        profile.setTenantId(tenantId);
        profile.setName(name);
        EdgeProfile defaultProfile = findDefaultEdgeProfile(tenantId);
        if (defaultProfile != null) {
            profile.getProfileData().setRuleChainIds(new ArrayList<>(defaultProfile.getProfileData().getRuleChainIds()));
            profile.setRootRuleChainId(defaultProfile.getRootRuleChainId());
        } else if (DEFAULT_EDGE_PROFILE_NAME.equals(name)) {
            profile.setDescription("Default edge profile");
            profile.setDefault(true);
        }
        // the caller's transaction owns the new profile, so a save that fails after this cannot leave it behind
        return saveEdgeProfile(profile);
    }

    @Override
    public EdgeProfile resolveEdgeProfile(TenantId tenantId, EdgeProfileId edgeProfileId, String type) {
        if (tenantId == null) {
            throw new DataValidationException("Edge should be assigned to tenant!");
        }
        if (edgeProfileId != null) {
            EdgeProfile profile = findEdgeProfileById(tenantId, edgeProfileId);
            if (profile == null) {
                throw new DataValidationException("Edge is referencing to non-existent edge profile!");
            }
            return profile;
        }
        if (StringUtils.isNotEmpty(type)) {
            return findOrCreateEdgeProfile(tenantId, type);
        }
        EdgeProfile defaultProfile = findDefaultEdgeProfile(tenantId);
        return defaultProfile != null ? defaultProfile : findOrCreateEdgeProfile(tenantId, DEFAULT_EDGE_PROFILE_NAME);
    }

    @Override
    @Transactional
    public boolean setDefaultEdgeProfile(TenantId tenantId, EdgeProfileId edgeProfileId) {
        log.trace("Executing setDefaultEdgeProfile [{}]", edgeProfileId);
        validateId(edgeProfileId, id -> INCORRECT_EDGE_PROFILE_ID + id);
        EdgeProfile profile = edgeProfileDao.findById(tenantId, edgeProfileId.getId());
        if (profile == null || !tenantId.equals(profile.getTenantId())) {
            throw new DataValidationException("Edge profile not found");
        }
        if (profile.isDefault()) {
            return false;
        }
        if (profile.getRootRuleChainId() == null) {
            throw new DataValidationException("Edge profile [" + profile.getName() + "] has no root rule chain and cannot be default!");
        }
        EdgeProfile previousDefault = findDefaultEdgeProfile(tenantId);
        if (previousDefault != null) {
            EdgeProfile previousDefaultCopy = new EdgeProfile(previousDefault);
            previousDefaultCopy.setDefault(false);
            edgeProfileDao.save(tenantId, previousDefaultCopy);
            publishEvictEvent(new EdgeProfileCacheEvictEvent(tenantId, previousDefaultCopy.getId(), previousDefaultCopy.getName(), null, true));
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId)
                    .entityId(previousDefaultCopy.getId()).entity(previousDefaultCopy).created(false).build());
        }
        profile.setDefault(true);
        edgeProfileDao.save(tenantId, profile);
        publishEvictEvent(new EdgeProfileCacheEvictEvent(tenantId, profile.getId(), profile.getName(), null, true));
        eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId)
                .entityId(profile.getId()).entity(profile).created(false).build());
        return true;
    }

    @Override
    public PageData<EdgeProfile> findEdgeProfilesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findEdgeProfilesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return edgeProfileDao.findByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    @Transactional
    public void deleteEdgeProfile(TenantId tenantId, EdgeProfileId edgeProfileId) {
        log.trace("Executing deleteEdgeProfile [{}]", edgeProfileId);
        validateId(edgeProfileId, id -> INCORRECT_EDGE_PROFILE_ID + id);
        deleteEntity(tenantId, edgeProfileId, false);
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        EdgeProfile profile = edgeProfileDao.findById(tenantId, id.getId());
        if (profile == null || !tenantId.equals(profile.getTenantId())) {
            return;
        }
        if (!force) {
            if (profile.isDefault()) {
                throw new DataValidationException("Deletion of Default Edge Profile is prohibited!");
            }
            if (edgeDao.existsByTenantIdAndEdgeProfileId(tenantId.getId(), profile.getId().getId())) {
                throw new DataValidationException("Edge profile [" + profile.getName() + "] is in use by "
                        + edgeDao.countByTenantIdAndEdgeProfileId(tenantId.getId(), profile.getId().getId()) + " edge(s) and cannot be deleted!");
            }
            // the relation would be cleaned up silently on delete, leaving those app profiles quietly back on the tenant default
            long appProfiles = agentAppProfileService.countAppProfilesByEdgeProfileId(tenantId, profile.getId());
            if (appProfiles > 0) {
                throw new DataValidationException("Edge profile [" + profile.getName() + "] is used by "
                        + appProfiles + " agent application profile(s) and cannot be deleted!");
            }
        }
        removeEdgeProfile(tenantId, profile);
    }

    private void removeEdgeProfile(TenantId tenantId, EdgeProfile profile) {
        edgeProfileDao.removeById(tenantId, profile.getId().getId());
        publishEvictEvent(new EdgeProfileCacheEvictEvent(tenantId, profile.getId(), profile.getName(), null, profile.isDefault()));
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(profile.getId()).entity(profile).build());
    }

    @Override
    @Transactional
    public void deleteEdgeProfilesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteEdgeProfilesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        tenantProfilesRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    @Transactional
    public void deleteByTenantId(TenantId tenantId) {
        deleteEdgeProfilesByTenantId(tenantId);
    }

    @Override
    public EdgeProfile addRuleChain(TenantId tenantId, EdgeProfileId edgeProfileId, RuleChainId ruleChainId) {
        return mutate(tenantId, edgeProfileId, p -> {
            if (!p.getProfileData().getRuleChainIds().contains(ruleChainId)) {
                p.getProfileData().getRuleChainIds().add(ruleChainId);
            }
        });
    }

    @Override
    public EdgeProfile removeRuleChain(TenantId tenantId, EdgeProfileId edgeProfileId, RuleChainId ruleChainId) {
        return mutate(tenantId, edgeProfileId, p -> {
            if (ruleChainId.equals(p.getRootRuleChainId())) {
                throw new DataValidationException("The root rule chain cannot be removed from the edge profile. Set another root first!");
            }
            p.getProfileData().getRuleChainIds().remove(ruleChainId);
        });
    }

    @Override
    public EdgeProfile setRootRuleChain(TenantId tenantId, EdgeProfileId edgeProfileId, RuleChainId ruleChainId) {
        return mutate(tenantId, edgeProfileId, p -> {
            if (!p.getProfileData().getRuleChainIds().contains(ruleChainId)) {
                p.getProfileData().getRuleChainIds().add(ruleChainId);
            }
            p.setRootRuleChainId(ruleChainId);
        });
    }

    @Override
    public EdgeProfile addEntityGroup(TenantId tenantId, EdgeProfileId edgeProfileId, EntityGroupId entityGroupId, EntityType groupType) {
        return mutate(tenantId, edgeProfileId, p -> {
            boolean present = p.getProfileData().getEntityGroups().stream().anyMatch(g -> entityGroupId.equals(g.getEntityGroupId()));
            if (!present) {
                p.getProfileData().getEntityGroups().add(new EdgeProfileEntityGroup(groupType, entityGroupId));
            }
        });
    }

    @Override
    public EdgeProfile removeEntityGroup(TenantId tenantId, EdgeProfileId edgeProfileId, EntityGroupId entityGroupId) {
        return mutate(tenantId, edgeProfileId, p -> p.getProfileData().getEntityGroups().removeIf(g -> entityGroupId.equals(g.getEntityGroupId())));
    }

    @Override
    public EdgeProfile addIntegration(TenantId tenantId, EdgeProfileId edgeProfileId, IntegrationId integrationId) {
        return mutate(tenantId, edgeProfileId, p -> {
            if (!p.getProfileData().getIntegrationIds().contains(integrationId)) {
                p.getProfileData().getIntegrationIds().add(integrationId);
            }
        });
    }

    @Override
    public EdgeProfile removeIntegration(TenantId tenantId, EdgeProfileId edgeProfileId, IntegrationId integrationId) {
        return mutate(tenantId, edgeProfileId, p -> p.getProfileData().getIntegrationIds().remove(integrationId));
    }

    @Override
    public EdgeProfile addSchedulerEvent(TenantId tenantId, EdgeProfileId edgeProfileId, SchedulerEventId schedulerEventId) {
        return mutate(tenantId, edgeProfileId, p -> {
            if (!p.getProfileData().getSchedulerEventIds().contains(schedulerEventId)) {
                p.getProfileData().getSchedulerEventIds().add(schedulerEventId);
            }
        });
    }

    @Override
    public EdgeProfile removeSchedulerEvent(TenantId tenantId, EdgeProfileId edgeProfileId, SchedulerEventId schedulerEventId) {
        return mutate(tenantId, edgeProfileId, p -> p.getProfileData().getSchedulerEventIds().remove(schedulerEventId));
    }

    private EdgeProfile mutate(TenantId tenantId, EdgeProfileId edgeProfileId, Consumer<EdgeProfile> change) {
        EdgeProfile profile = findEdgeProfileById(tenantId, edgeProfileId);
        if (profile == null) {
            throw new DataValidationException("Edge profile not found");
        }
        EdgeProfile copy = new EdgeProfile(profile);
        change.accept(copy);
        return saveEdgeProfile(copy);
    }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void handleReferencedEntityDeleted(DeleteEntityEvent<?> event) {
        EntityId entityId = event.getEntityId();
        if (entityId == null || !EdgeProfileData.REFERENCED_ENTITY_TYPES.contains(entityId.getEntityType())) {
            return;
        }
        TenantId tenantId = event.getTenantId();
        for (EdgeProfile profile : edgeProfileDao.findByTenantIdAndReferencedEntityId(tenantId.getId(), entityId.getId())) {
            try {
                if (profile.getProfileData().removeReferencesTo(entityId)) {
                    if (profile.getRootRuleChainId() != null && profile.getRootRuleChainId().getId().equals(entityId.getId())) {
                        log.warn("[{}] Root rule chain {} of edge profile {} was deleted", tenantId, entityId, profile.getName());
                        profile.setRootRuleChainId(null);
                    }
                    saveEdgeProfile(profile, false);
                }
            } catch (Throwable e) {
                log.error("[{}] Failed to strip deleted entity [{}] from edge profile [{}]", tenantId, entityId, profile.getId(), e);
            }
        }
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findEdgeProfileById(tenantId, new EdgeProfileId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        ListenableFuture<EdgeProfile> future = edgeProfileDao.findByIdAsync(tenantId, entityId.getId());
        return FluentFuture.from(future).transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return edgeProfileDao.countByTenantId(tenantId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.EDGE_PROFILE;
    }

    private final PaginatedRemover<TenantId, EdgeProfile> tenantProfilesRemover = new PaginatedRemover<>() {
        @Override
        protected PageData<EdgeProfile> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return edgeProfileDao.findByTenantId(id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, EdgeProfile entity) {
            removeEdgeProfile(tenantId, entity);
        }
    };
}
