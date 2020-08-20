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
package org.thingsboard.server.dao.device;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;

import java.util.Arrays;
import java.util.Collections;

import static org.thingsboard.server.common.data.CacheConstants.DEVICE_PROFILE_CACHE;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class DeviceProfileServiceImpl extends AbstractEntityService implements DeviceProfileService {

    private static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    private static final String INCORRECT_DEVICE_PROFILE_ID = "Incorrect deviceProfileId ";

    @Autowired
    private DeviceProfileDao deviceProfileDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private CacheManager cacheManager;

    @Cacheable(cacheNames = DEVICE_PROFILE_CACHE, key = "{#deviceProfileId.id}")
    @Override
    public DeviceProfile findDeviceProfileById(TenantId tenantId, DeviceProfileId deviceProfileId) {
        log.trace("Executing findDeviceProfileById [{}]", deviceProfileId);
        Validator.validateId(deviceProfileId, INCORRECT_DEVICE_PROFILE_ID + deviceProfileId);
        return deviceProfileDao.findById(tenantId, deviceProfileId.getId());
    }

    @Cacheable(cacheNames = DEVICE_PROFILE_CACHE, key = "{'info', #deviceProfileId.id}")
    @Override
    public EntityInfo findDeviceProfileInfoById(TenantId tenantId, DeviceProfileId deviceProfileId) {
        log.trace("Executing findDeviceProfileById [{}]", deviceProfileId);
        Validator.validateId(deviceProfileId, INCORRECT_DEVICE_PROFILE_ID + deviceProfileId);
        return deviceProfileDao.findDeviceProfileInfoById(tenantId, deviceProfileId.getId());
    }

    @Override
    public DeviceProfile saveDeviceProfile(DeviceProfile deviceProfile) {
        log.trace("Executing saveDeviceProfile [{}]", deviceProfile);
        deviceProfileValidator.validate(deviceProfile, DeviceProfile::getTenantId);
        DeviceProfile savedDeviceProfile;
        try {
            savedDeviceProfile = deviceProfileDao.save(deviceProfile.getTenantId(), deviceProfile);
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("device_profile_name_unq_key")) {
                throw new DataValidationException("Device profile with such name already exists!");
            } else {
                throw t;
            }
        }
        Cache cache = cacheManager.getCache(DEVICE_PROFILE_CACHE);
        cache.evict(Collections.singletonList(savedDeviceProfile.getId().getId()));
        cache.evict(Arrays.asList("info", savedDeviceProfile.getId().getId()));
        if (savedDeviceProfile.isDefault()) {
            cache.evict(Arrays.asList("default", savedDeviceProfile.getTenantId().getId()));
            cache.evict(Arrays.asList("default", "info", savedDeviceProfile.getTenantId().getId()));
        }
        return savedDeviceProfile;
    }

    @Override
    public void deleteDeviceProfile(TenantId tenantId, DeviceProfileId deviceProfileId) {
        log.trace("Executing deleteDeviceProfile [{}]", deviceProfileId);
        Validator.validateId(deviceProfileId, INCORRECT_DEVICE_PROFILE_ID + deviceProfileId);
        DeviceProfile deviceProfile = deviceProfileDao.findById(tenantId, deviceProfileId.getId());
        if (deviceProfile != null && deviceProfile.isDefault()) {
            throw new DataValidationException("Deletion of Default Device Profile is prohibited!");
        }
        this.removeDeviceProfile(tenantId, deviceProfileId);
    }

    private void removeDeviceProfile(TenantId tenantId, DeviceProfileId deviceProfileId) {
        deleteEntityRelations(tenantId, deviceProfileId);
        deviceProfileDao.removeById(tenantId, deviceProfileId.getId());
        Cache cache = cacheManager.getCache(DEVICE_PROFILE_CACHE);
        cache.evict(Collections.singletonList(deviceProfileId.getId()));
        cache.evict(Arrays.asList("info", deviceProfileId.getId()));
    }

    @Override
    public PageData<DeviceProfile> findDeviceProfiles(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findDeviceProfiles tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return deviceProfileDao.findDeviceProfiles(tenantId, pageLink);
    }

    @Override
    public PageData<EntityInfo> findDeviceProfileInfos(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findDeviceProfileInfos tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return deviceProfileDao.findDeviceProfileInfos(tenantId, pageLink);
    }

    @Override
    public DeviceProfile createDefaultDeviceProfile(TenantId tenantId) {
        log.trace("Executing createDefaultDeviceProfile tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setTenantId(tenantId);
        deviceProfile.setDefault(true);
        deviceProfile.setName("Default");
        deviceProfile.setDescription("Default device profile");
        deviceProfile.setProfileData(JacksonUtil.OBJECT_MAPPER.createObjectNode());
        return saveDeviceProfile(deviceProfile);
    }

    @Cacheable(cacheNames = DEVICE_PROFILE_CACHE, key = "{'default', #tenantId.id}")
    @Override
    public DeviceProfile findDefaultDeviceProfile(TenantId tenantId) {
        log.trace("Executing findDefaultDeviceProfile tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return deviceProfileDao.findDefaultDeviceProfile(tenantId);
    }

    @Cacheable(cacheNames = DEVICE_PROFILE_CACHE, key = "{'default', 'info', #tenantId.id}")
    @Override
    public EntityInfo findDefaultDeviceProfileInfo(TenantId tenantId) {
        log.trace("Executing findDefaultDeviceProfileInfo tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return deviceProfileDao.findDefaultDeviceProfileInfo(tenantId);
    }

    @Override
    public boolean setDefaultDeviceProfile(TenantId tenantId, DeviceProfileId deviceProfileId) {
        log.trace("Executing setDefaultDeviceProfile [{}]", deviceProfileId);
        Validator.validateId(deviceProfileId, INCORRECT_DEVICE_PROFILE_ID + deviceProfileId);
        DeviceProfile deviceProfile = deviceProfileDao.findById(tenantId, deviceProfileId.getId());
        if (!deviceProfile.isDefault()) {
            Cache cache = cacheManager.getCache(DEVICE_PROFILE_CACHE);
            deviceProfile.setDefault(true);
            DeviceProfile previousDefaultDeviceProfile = findDefaultDeviceProfile(tenantId);
            boolean changed = false;
            if (previousDefaultDeviceProfile == null) {
                deviceProfileDao.save(tenantId, deviceProfile);
                changed = true;
            } else if (!previousDefaultDeviceProfile.getId().equals(deviceProfile.getId())) {
                previousDefaultDeviceProfile.setDefault(false);
                deviceProfileDao.save(tenantId, previousDefaultDeviceProfile);
                deviceProfileDao.save(tenantId, deviceProfile);
                cache.evict(Collections.singletonList(previousDefaultDeviceProfile.getId().getId()));
                cache.evict(Arrays.asList("info", previousDefaultDeviceProfile.getId().getId()));
                changed = true;
            }
            if (changed) {
                cache.evict(Collections.singletonList(deviceProfile.getId().getId()));
                cache.evict(Arrays.asList("info", deviceProfile.getId().getId()));
                cache.evict(Arrays.asList("default", tenantId.getId()));
                cache.evict(Arrays.asList("default", "info", tenantId.getId()));
            }
            return changed;
        }
        return false;
    }

    @Override
    public void deleteDeviceProfilesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteDeviceProfilesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantDeviceProfilesRemover.removeEntities(tenantId, tenantId);
    }

    private DataValidator<DeviceProfile> deviceProfileValidator =
            new DataValidator<DeviceProfile>() {
                @Override
                protected void validateDataImpl(TenantId tenantId, DeviceProfile deviceProfile) {
                    if (StringUtils.isEmpty(deviceProfile.getName())) {
                        throw new DataValidationException("Device profile name should be specified!");
                    }
                    if (deviceProfile.getTenantId() == null) {
                        throw new DataValidationException("Device profile should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(deviceProfile.getTenantId(), deviceProfile.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Device profile is referencing to non-existent tenant!");
                        }
                    }
                    if (deviceProfile.isDefault()) {
                        DeviceProfile defaultDeviceProfile = findDefaultDeviceProfile(tenantId);
                        if (defaultDeviceProfile != null && !defaultDeviceProfile.getId().equals(deviceProfile.getId())) {
                            throw new DataValidationException("Another default device profile is present in scope of current tenant!");
                        }
                    }
                }
            };

    private PaginatedRemover<TenantId, DeviceProfile> tenantDeviceProfilesRemover =
            new PaginatedRemover<TenantId, DeviceProfile>() {

                @Override
                protected PageData<DeviceProfile> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return deviceProfileDao.findDeviceProfiles(id, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, DeviceProfile entity) {
                    removeDeviceProfile(tenantId, entity.getId());
                }
            };

}
