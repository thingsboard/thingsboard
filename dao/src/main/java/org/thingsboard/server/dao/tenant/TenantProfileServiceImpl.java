/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;

import java.util.Arrays;
import java.util.Collections;

import static org.thingsboard.server.common.data.CacheConstants.TENANT_PROFILE_CACHE;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class TenantProfileServiceImpl extends AbstractEntityService implements TenantProfileService {

    private static final String INCORRECT_TENANT_PROFILE_ID = "Incorrect tenantProfileId ";

    @Autowired
    private TenantProfileDao tenantProfileDao;

    @Autowired
    private CacheManager cacheManager;

    @Cacheable(cacheNames = TENANT_PROFILE_CACHE, key = "{#tenantProfileId.id}")
    @Override
    public TenantProfile findTenantProfileById(TenantId tenantId, TenantProfileId tenantProfileId) {
        log.trace("Executing findTenantProfileById [{}]", tenantProfileId);
        Validator.validateId(tenantProfileId, INCORRECT_TENANT_PROFILE_ID + tenantProfileId);
        return tenantProfileDao.findById(tenantId, tenantProfileId.getId());
    }

    @Cacheable(cacheNames = TENANT_PROFILE_CACHE, key = "{'info', #tenantProfileId.id}")
    @Override
    public EntityInfo findTenantProfileInfoById(TenantId tenantId, TenantProfileId tenantProfileId) {
        log.trace("Executing findTenantProfileInfoById [{}]", tenantProfileId);
        Validator.validateId(tenantProfileId, INCORRECT_TENANT_PROFILE_ID + tenantProfileId);
        return tenantProfileDao.findTenantProfileInfoById(tenantId, tenantProfileId.getId());
    }

    @Override
    public TenantProfile saveTenantProfile(TenantId tenantId, TenantProfile tenantProfile) {
        log.trace("Executing saveTenantProfile [{}]", tenantProfile);
        tenantProfileValidator.validate(tenantProfile, (tenantProfile1) -> TenantId.SYS_TENANT_ID);
        TenantProfile savedTenantProfile;
        try {
            savedTenantProfile = tenantProfileDao.save(tenantId, tenantProfile);
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("tenant_profile_name_unq_key")) {
                throw new DataValidationException("Tenant profile with such name already exists!");
            } else {
                throw t;
            }
        }
        Cache cache = cacheManager.getCache(TENANT_PROFILE_CACHE);
        cache.evict(Collections.singletonList(savedTenantProfile.getId().getId()));
        cache.evict(Arrays.asList("info", savedTenantProfile.getId().getId()));
        if (savedTenantProfile.isDefault()) {
            cache.evict(Collections.singletonList("default"));
            cache.evict(Arrays.asList("default", "info"));
        }
        return savedTenantProfile;
    }

    @Override
    public void deleteTenantProfile(TenantId tenantId, TenantProfileId tenantProfileId) {
        log.trace("Executing deleteTenantProfile [{}]", tenantProfileId);
        validateId(tenantId, INCORRECT_TENANT_PROFILE_ID + tenantProfileId);
        TenantProfile tenantProfile = tenantProfileDao.findById(tenantId, tenantProfileId.getId());
        if (tenantProfile != null && tenantProfile.isDefault()) {
            throw new DataValidationException("Deletion of Default Tenant Profile is prohibited!");
        }
        this.removeTenantProfile(tenantId, tenantProfileId);
    }

    private void removeTenantProfile(TenantId tenantId, TenantProfileId tenantProfileId) {
        deleteEntityRelations(tenantId, tenantProfileId);
        tenantProfileDao.removeById(tenantId, tenantProfileId.getId());
        Cache cache = cacheManager.getCache(TENANT_PROFILE_CACHE);
        cache.evict(Collections.singletonList(tenantProfileId.getId()));
        cache.evict(Arrays.asList("info", tenantProfileId.getId()));
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
            defaultTenantProfile.setProfileData(JacksonUtil.OBJECT_MAPPER.createObjectNode());
            defaultTenantProfile.setDescription("Default tenant profile");
            defaultTenantProfile.setIsolatedTbCore(false);
            defaultTenantProfile.setIsolatedTbRuleEngine(false);
            defaultTenantProfile = saveTenantProfile(tenantId, defaultTenantProfile);
        }
        return defaultTenantProfile;
    }

    @Cacheable(cacheNames = TENANT_PROFILE_CACHE, key = "{'default'}")
    @Override
    public TenantProfile findDefaultTenantProfile(TenantId tenantId) {
        log.trace("Executing findDefaultTenantProfile");
        return tenantProfileDao.findDefaultTenantProfile(tenantId);
    }

    @Cacheable(cacheNames = TENANT_PROFILE_CACHE, key = "{'default', 'info'}")
    @Override
    public EntityInfo findDefaultTenantProfileInfo(TenantId tenantId) {
        log.trace("Executing findDefaultTenantProfileInfo");
        return tenantProfileDao.findDefaultTenantProfileInfo(tenantId);
    }

    @Override
    public boolean setDefaultTenantProfile(TenantId tenantId, TenantProfileId tenantProfileId) {
        log.trace("Executing setDefaultTenantProfile [{}]", tenantProfileId);
        validateId(tenantId, INCORRECT_TENANT_PROFILE_ID + tenantProfileId);
        TenantProfile tenantProfile = tenantProfileDao.findById(tenantId, tenantProfileId.getId());
        if (!tenantProfile.isDefault()) {
            Cache cache = cacheManager.getCache(TENANT_PROFILE_CACHE);
            tenantProfile.setDefault(true);
            TenantProfile previousDefaultTenantProfile = findDefaultTenantProfile(tenantId);
            boolean changed = false;
            if (previousDefaultTenantProfile == null) {
                tenantProfileDao.save(tenantId, tenantProfile);
                changed = true;
            } else if (!previousDefaultTenantProfile.getId().equals(tenantProfile.getId())) {
                previousDefaultTenantProfile.setDefault(false);
                tenantProfileDao.save(tenantId, previousDefaultTenantProfile);
                tenantProfileDao.save(tenantId, tenantProfile);
                cache.evict(Collections.singletonList(previousDefaultTenantProfile.getId().getId()));
                cache.evict(Arrays.asList("info", previousDefaultTenantProfile.getId().getId()));
                changed = true;
            }
            if (changed) {
                cache.evict(Collections.singletonList(tenantProfile.getId().getId()));
                cache.evict(Arrays.asList("info", tenantProfile.getId().getId()));
                cache.evict(Collections.singletonList("default"));
                cache.evict(Arrays.asList("default", "info"));
            }
            return changed;
        }
        return false;
    }

    @Override
    public void deleteTenantProfiles(TenantId tenantId) {
        log.trace("Executing deleteTenantProfiles");
        tenantProfilesRemover.removeEntities(tenantId, null);
    }

    private DataValidator<TenantProfile> tenantProfileValidator =
            new DataValidator<TenantProfile>() {
                @Override
                protected void validateDataImpl(TenantId tenantId, TenantProfile tenantProfile) {
                    if (StringUtils.isEmpty(tenantProfile.getName())) {
                        throw new DataValidationException("Tenant profile name should be specified!");
                    }
                    if (tenantProfile.isDefault()) {
                        TenantProfile defaultTenantProfile = findDefaultTenantProfile(tenantId);
                        if (defaultTenantProfile != null && !defaultTenantProfile.getId().equals(tenantProfile.getId())) {
                            throw new DataValidationException("Another default tenant profile is present!");
                        }
                    }
                }
            };

    private PaginatedRemover<String, TenantProfile> tenantProfilesRemover =
            new PaginatedRemover<String, TenantProfile>() {

                @Override
                protected PageData<TenantProfile> findEntities(TenantId tenantId, String id, PageLink pageLink) {
                    return tenantProfileDao.findTenantProfiles(tenantId, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, TenantProfile entity) {
                    removeTenantProfile(tenantId, entity.getId());
                }
            };

}
