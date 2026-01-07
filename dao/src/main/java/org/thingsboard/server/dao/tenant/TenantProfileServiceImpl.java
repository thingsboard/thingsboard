/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.tenant;

import com.google.common.util.concurrent.FluentFuture;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.common.util.DebugModeUtil.DEBUG_MODE_DEFAULT_DURATION_MINUTES;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service("TenantProfileDaoService")
@Slf4j
public class TenantProfileServiceImpl extends AbstractCachedEntityService<TenantProfileCacheKey, TenantProfile, TenantProfileEvictEvent> implements TenantProfileService {

    private static final String INCORRECT_TENANT_PROFILE_ID = "Incorrect tenantProfileId ";
    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";

    @Autowired
    private TenantProfileDao tenantProfileDao;

    @Autowired
    private DataValidator<TenantProfile> tenantProfileValidator;

    @TransactionalEventListener(classes = TenantProfileEvictEvent.class)
    @Override
    public void handleEvictEvent(TenantProfileEvictEvent event) {
        List<TenantProfileCacheKey> keys = new ArrayList<>(2);
        if (event.getTenantProfileId() != null) {
            keys.add(TenantProfileCacheKey.fromId(event.getTenantProfileId()));
        }
        if (event.isDefaultProfile()) {
            keys.add(TenantProfileCacheKey.defaultProfile());
        }
        cache.evict(keys);
    }

    @Override
    public TenantProfile findTenantProfileById(TenantId tenantId, TenantProfileId tenantProfileId) {
        log.trace("Executing findTenantProfileById [{}]", tenantProfileId);
        Validator.validateId(tenantProfileId, id -> INCORRECT_TENANT_PROFILE_ID + id);
        return cache.getAndPutInTransaction(TenantProfileCacheKey.fromId(tenantProfileId),
                () -> tenantProfileDao.findById(tenantId, tenantProfileId.getId()), true);
    }

    @Override
    public EntityInfo findTenantProfileInfoById(TenantId tenantId, TenantProfileId tenantProfileId) {
        log.trace("Executing findTenantProfileInfoById [{}]", tenantProfileId);
        TenantProfile profile = findTenantProfileById(tenantId, tenantProfileId);
        return profile == null ? null : new EntityInfo(profile.getId(), profile.getName());
    }

    @Override
    public TenantProfile saveTenantProfile(TenantId tenantId, TenantProfile tenantProfile) {
        log.trace("Executing saveTenantProfile [{}]", tenantProfile);
        tenantProfileValidator.validate(tenantProfile, (tenantProfile1) -> TenantId.SYS_TENANT_ID);
        TenantProfile savedTenantProfile;
        try {
            savedTenantProfile = tenantProfileDao.save(tenantId, tenantProfile);
            publishEvictEvent(new TenantProfileEvictEvent(savedTenantProfile.getId(), savedTenantProfile.isDefault()));
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(tenantId).entity(savedTenantProfile)
                    .entityId(savedTenantProfile.getId()).created(tenantProfile.getId() == null).build());
        } catch (Exception t) {
            handleEvictEvent(new TenantProfileEvictEvent(null, tenantProfile.isDefault()));
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("tenant_profile_name_unq_key")) {
                throw new DataValidationException("Tenant profile with such name already exists!");
            } else {
                throw t;
            }
        }
        return savedTenantProfile;
    }

    @Override
    public void deleteTenantProfile(TenantId tenantId, TenantProfileId tenantProfileId) {
        log.trace("Executing deleteTenantProfile [{}]", tenantProfileId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(tenantProfileId, id -> INCORRECT_TENANT_PROFILE_ID + id);
        TenantProfile tenantProfile = tenantProfileDao.findById(tenantId, tenantProfileId.getId());
        if (tenantProfile != null && tenantProfile.isDefault()) {
            throw new DataValidationException("Deletion of Default Tenant Profile is prohibited!");
        }
        this.removeTenantProfile(tenantId, tenantProfile, false);
    }

    private void removeTenantProfile(TenantId tenantId, TenantProfile tenantProfile, boolean isDefault) {
        TenantProfileId tenantProfileId = tenantProfile.getId();
        try {
            tenantProfileDao.removeById(tenantId, tenantProfileId.getId());
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_tenant_profile")) {
                throw new DataValidationException("The tenant profile referenced by the tenants cannot be deleted!");
            } else {
                throw t;
            }
        }
        publishEvictEvent(new TenantProfileEvictEvent(tenantProfileId, isDefault));
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entity(tenantProfile).entityId(tenantProfileId).build());
    }

    @Override
    public PageData<TenantProfile> findTenantProfiles(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantProfiles pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink);
        return tenantProfileDao.findTenantProfiles(tenantId, pageLink);
    }

    @Override
    public PageData<EntityInfo> findTenantProfileInfos(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findTenantProfileInfos pageLink [{}]", pageLink);
        Validator.validatePageLink(pageLink);
        return tenantProfileDao.findTenantProfileInfos(tenantId, pageLink);
    }

    @Override
    public TenantProfile findOrCreateDefaultTenantProfile(TenantId tenantId) {
        log.trace("Executing findOrCreateDefaultTenantProfile");
        TenantProfile defaultTenantProfile = findDefaultTenantProfile(tenantId);
        if (defaultTenantProfile == null) {
            defaultTenantProfile = new TenantProfile();
            defaultTenantProfile.setDefault(true);
            defaultTenantProfile.setName("Default");
            TenantProfileData profileData = new TenantProfileData();
            DefaultTenantProfileConfiguration configuration = new DefaultTenantProfileConfiguration();
            configuration.setMaxDebugModeDurationMinutes(DEBUG_MODE_DEFAULT_DURATION_MINUTES);
            profileData.setConfiguration(configuration);
            defaultTenantProfile.setProfileData(profileData);
            defaultTenantProfile.setDescription("Default tenant profile");
            defaultTenantProfile.setIsolatedTbRuleEngine(false);
            defaultTenantProfile = saveTenantProfile(tenantId, defaultTenantProfile);
        }
        return defaultTenantProfile;
    }

    @Override
    public TenantProfile findDefaultTenantProfile(TenantId tenantId) {
        log.trace("Executing findDefaultTenantProfile");
        return cache.getAndPutInTransaction(TenantProfileCacheKey.defaultProfile(),
                () -> tenantProfileDao.findDefaultTenantProfile(tenantId), true);

    }

    @Override
    public EntityInfo findDefaultTenantProfileInfo(TenantId tenantId) {
        log.trace("Executing findDefaultTenantProfileInfo");
        var tenantProfile = findDefaultTenantProfile(tenantId);
        return tenantProfile == null ? null : new EntityInfo(tenantProfile.getId(), tenantProfile.getName());
    }

    @Override
    public boolean setDefaultTenantProfile(TenantId tenantId, TenantProfileId tenantProfileId) {
        log.trace("Executing setDefaultTenantProfile [{}]", tenantProfileId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(tenantProfileId, id -> INCORRECT_TENANT_PROFILE_ID + id);
        TenantProfile tenantProfile = tenantProfileDao.findById(tenantId, tenantProfileId.getId());
        if (!tenantProfile.isDefault()) {
            tenantProfile.setDefault(true);
            TenantProfile previousDefaultTenantProfile = findDefaultTenantProfile(tenantId);
            boolean changed = false;
            if (previousDefaultTenantProfile == null) {
                tenantProfileDao.save(tenantId, tenantProfile);
                publishEvictEvent(new TenantProfileEvictEvent(tenantProfileId, true));
                changed = true;
            } else if (!previousDefaultTenantProfile.getId().equals(tenantProfile.getId())) {
                previousDefaultTenantProfile.setDefault(false);
                tenantProfileDao.save(tenantId, previousDefaultTenantProfile);
                tenantProfileDao.save(tenantId, tenantProfile);
                publishEvictEvent(new TenantProfileEvictEvent(previousDefaultTenantProfile.getId(), false));
                publishEvictEvent(new TenantProfileEvictEvent(tenantProfileId, true));
                changed = true;
            }
            return changed;
        }
        return false;
    }

    @Override
    public List<TenantProfile> findTenantProfilesByIds(TenantId tenantId, UUID[] ids) {
        return tenantProfileDao.findTenantProfilesByIds(tenantId, ids);
    }

    @Override
    public void deleteTenantProfiles(TenantId tenantId) {
        log.trace("Executing deleteTenantProfiles");
        tenantProfilesRemover.removeEntities(tenantId, null);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findTenantProfileById(tenantId, new TenantProfileId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(tenantProfileDao.findByIdAsync(tenantId, entityId.getId()))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.TENANT_PROFILE;
    }

    private final PaginatedRemover<String, TenantProfile> tenantProfilesRemover = new PaginatedRemover<>() {

        @Override
        protected PageData<TenantProfile> findEntities(TenantId tenantId, String id, PageLink pageLink) {
            return tenantProfileDao.findTenantProfiles(tenantId, pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, TenantProfile entity) {
            removeTenantProfile(tenantId, entity, entity.isDefault());
        }

    };

}
