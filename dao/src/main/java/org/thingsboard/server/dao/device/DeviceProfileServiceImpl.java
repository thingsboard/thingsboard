/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import com.google.common.util.concurrent.FluentFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.X509CertificateChainProvisionConfiguration;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.dao.entity.CachedVersionedEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.service.validator.DeviceProfileDataValidator;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Service("DeviceProfileDaoService")
@Slf4j
@RequiredArgsConstructor
public class DeviceProfileServiceImpl extends CachedVersionedEntityService<DeviceProfileCacheKey, DeviceProfile, DeviceProfileEvictEvent> implements DeviceProfileService {

    private static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    private static final String INCORRECT_DEVICE_PROFILE_ID = "Incorrect deviceProfileId ";
    private static final String INCORRECT_DEVICE_PROFILE_NAME = "Incorrect deviceProfileName ";
    private static final String INCORRECT_PROVISION_DEVICE_KEY = "Incorrect provisionDeviceKey ";
    private static final String DEVICE_PROFILE_WITH_SUCH_NAME_ALREADY_EXISTS = "Device profile with such name already exists!";

    @Autowired
    private DeviceProfileDao deviceProfileDao;

    @Autowired
    private DeviceDao deviceDao;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceProfileDataValidator deviceProfileValidator;

    @Autowired
    private ImageService imageService;

    @TransactionalEventListener(classes = DeviceProfileEvictEvent.class)
    @Override
    public void handleEvictEvent(DeviceProfileEvictEvent event) {
        List<DeviceProfileCacheKey> toEvict = new ArrayList<>(2);
        toEvict.add(DeviceProfileCacheKey.forName(event.getTenantId(), event.getNewName()));
        if (event.getSavedDeviceProfile() != null) {
            cache.put(DeviceProfileCacheKey.forId(event.getSavedDeviceProfile().getId()), event.getSavedDeviceProfile());
        } else if (event.getDeviceProfileId() != null) {
            toEvict.add(DeviceProfileCacheKey.forId(event.getDeviceProfileId()));
        }
        if (event.isDefaultProfile()) {
            toEvict.add(DeviceProfileCacheKey.forDefaultProfile(event.getTenantId()));
        }
        if (StringUtils.isNotEmpty(event.getOldName()) && !event.getOldName().equals(event.getNewName())) {
            toEvict.add(DeviceProfileCacheKey.forName(event.getTenantId(), event.getOldName()));
        }
        if (StringUtils.isNotEmpty(event.getProvisionDeviceKey())) {
            toEvict.add(DeviceProfileCacheKey.forProvisionKey(event.getProvisionDeviceKey()));
        }
        cache.evict(toEvict);
    }

    @Override
    public DeviceProfile findDeviceProfileById(TenantId tenantId, DeviceProfileId deviceProfileId) {
        return findDeviceProfileById(tenantId, deviceProfileId, true);
    }

    @Override
    public DeviceProfile findDeviceProfileById(TenantId tenantId, DeviceProfileId deviceProfileId, boolean putInCache) {
        log.trace("Executing findDeviceProfileById [{}]", deviceProfileId);
        validateId(deviceProfileId, id -> INCORRECT_DEVICE_PROFILE_ID + id);
        return cache.get(DeviceProfileCacheKey.forId(deviceProfileId),
                () -> deviceProfileDao.findById(tenantId, deviceProfileId.getId()), putInCache);
    }

    @Override
    public DeviceProfile findDeviceProfileByName(TenantId tenantId, String profileName) {
        return findDeviceProfileByName(tenantId, profileName, true);
    }

    @Override
    public DeviceProfile findDeviceProfileByName(TenantId tenantId, String profileName, boolean putInCache) {
        log.trace("Executing findDeviceProfileByName [{}][{}]", tenantId, profileName);
        validateString(profileName, pn -> INCORRECT_DEVICE_PROFILE_NAME + pn);
        return cache.getOrFetchFromDB(DeviceProfileCacheKey.forName(tenantId, profileName),
                () -> deviceProfileDao.findByName(tenantId, profileName), true, putInCache);
    }

    @Override
    public DeviceProfile findDeviceProfileByProvisionDeviceKey(String provisionDeviceKey) {
        log.trace("Executing findDeviceProfileByProvisionDeviceKey provisionKey [{}]", provisionDeviceKey);
        validateString(provisionDeviceKey, dk -> INCORRECT_PROVISION_DEVICE_KEY + dk);
        return cache.getAndPutInTransaction(DeviceProfileCacheKey.forProvisionKey(provisionDeviceKey),
                () -> deviceProfileDao.findByProvisionDeviceKey(provisionDeviceKey), false);
    }

    @Override
    public DeviceProfileInfo findDeviceProfileInfoById(TenantId tenantId, DeviceProfileId deviceProfileId) {
        log.trace("Executing findDeviceProfileById [{}]", deviceProfileId);
        validateId(deviceProfileId, id -> INCORRECT_DEVICE_PROFILE_ID + id);
        return toDeviceProfileInfo(findDeviceProfileById(tenantId, deviceProfileId));
    }

    @Override
    public DeviceProfile saveDeviceProfile(DeviceProfile deviceProfile) {
        return saveDeviceProfile(deviceProfile, true, true);
    }

    @Override
    public DeviceProfile saveDeviceProfile(DeviceProfile deviceProfile, boolean doValidate, boolean publishSaveEvent) {
        log.trace("Executing saveDeviceProfile [{}]", deviceProfile);
        if (deviceProfile.getProfileData() != null && deviceProfile.getProfileData().getProvisionConfiguration() instanceof X509CertificateChainProvisionConfiguration) {
            X509CertificateChainProvisionConfiguration x509Configuration = (X509CertificateChainProvisionConfiguration) deviceProfile.getProfileData().getProvisionConfiguration();
            if (x509Configuration.getProvisionDeviceSecret() != null) {
                formatDeviceProfileCertificate(deviceProfile, x509Configuration);
            }
        }
        DeviceProfile oldDeviceProfile = null;
        if (doValidate) {
            oldDeviceProfile = deviceProfileValidator.validate(deviceProfile, DeviceProfile::getTenantId);
        } else if (deviceProfile.getId() != null) {
            oldDeviceProfile = findDeviceProfileById(deviceProfile.getTenantId(), deviceProfile.getId(), false);
        }
        DeviceProfile savedDeviceProfile;
        try {
            imageService.replaceBase64WithImageUrl(deviceProfile, "device profile");
            savedDeviceProfile = deviceProfileDao.saveAndFlush(deviceProfile.getTenantId(), deviceProfile);

            publishEvictEvent(new DeviceProfileEvictEvent(savedDeviceProfile.getTenantId(), savedDeviceProfile.getName(),
                    oldDeviceProfile != null ? oldDeviceProfile.getName() : null, savedDeviceProfile.getId(), savedDeviceProfile.isDefault(),
                    oldDeviceProfile != null ? oldDeviceProfile.getProvisionDeviceKey() : null, savedDeviceProfile));
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(savedDeviceProfile.getTenantId()).entityId(savedDeviceProfile.getId())
                    .entity(savedDeviceProfile).oldEntity(oldDeviceProfile).created(oldDeviceProfile == null).broadcastEvent(publishSaveEvent).build());
        } catch (Exception t) {
            handleEvictEvent(new DeviceProfileEvictEvent(deviceProfile.getTenantId(), deviceProfile.getName(),
                    oldDeviceProfile != null ? oldDeviceProfile.getName() : null, null, deviceProfile.isDefault(),
                    oldDeviceProfile != null ? oldDeviceProfile.getProvisionDeviceKey() : null));
            String unqProvisionKeyErrorMsg = DeviceProfileProvisionType.X509_CERTIFICATE_CHAIN.equals(deviceProfile.getProvisionType())
                    ? "Device profile with such certificate already exists!"
                    : "Device profile with such provision device key already exists!";
            checkConstraintViolation(t,
                    Map.of("device_profile_name_unq_key", DEVICE_PROFILE_WITH_SUCH_NAME_ALREADY_EXISTS,
                            "device_provision_key_unq_key", unqProvisionKeyErrorMsg,
                            "device_profile_external_id_unq_key", "Device profile with such external id already exists!"));
            throw t;
        }
        if (oldDeviceProfile != null && !oldDeviceProfile.getName().equals(deviceProfile.getName())) {
            PageLink pageLink = new PageLink(100);
            PageData<Device> pageData;
            do {
                pageData = deviceDao.findDevicesByTenantIdAndProfileId(deviceProfile.getTenantId().getId(), deviceProfile.getUuidId(), pageLink);
                for (Device device : pageData.getData()) {
                    device.setType(deviceProfile.getName());
                    deviceService.saveDevice(device);
                }
                pageLink = pageLink.nextPageLink();
            } while (pageData.hasNext());
        }
        return savedDeviceProfile;
    }

    @Override
    @Transactional
    public void deleteDeviceProfile(TenantId tenantId, DeviceProfileId deviceProfileId) {
        log.trace("Executing deleteDeviceProfile [{}]", deviceProfileId);
        validateId(deviceProfileId, id -> INCORRECT_DEVICE_PROFILE_ID + id);
        deleteEntity(tenantId, deviceProfileId, false);
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        DeviceProfile deviceProfile = deviceProfileDao.findById(tenantId, id.getId());
        if (deviceProfile == null) {
            return;
        }
        if (!force && deviceProfile.isDefault()) {
            throw new DataValidationException("Deletion of Default Device Profile is prohibited!");
        }
        removeDeviceProfile(tenantId, deviceProfile);
    }

    private void removeDeviceProfile(TenantId tenantId, DeviceProfile deviceProfile) {
        DeviceProfileId deviceProfileId = deviceProfile.getId();
        try {
            deviceProfileDao.removeById(tenantId, deviceProfileId.getId());
            publishEvictEvent(new DeviceProfileEvictEvent(deviceProfile.getTenantId(), deviceProfile.getName(),
                    null, deviceProfile.getId(), deviceProfile.isDefault(),
                    deviceProfile.getProvisionDeviceKey()));
            eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(deviceProfileId).entity(deviceProfile).build());
        } catch (Exception e) {
            checkConstraintViolation(e, "fk_device_profile", "The device profile referenced by the devices cannot be deleted!");
            throw e;
        }
    }

    @Override
    public PageData<DeviceProfile> findDeviceProfiles(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findDeviceProfiles tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validatePageLink(pageLink);
        return deviceProfileDao.findDeviceProfiles(tenantId, pageLink);
    }

    @Override
    public PageData<DeviceProfileInfo> findDeviceProfileInfos(TenantId tenantId, PageLink pageLink, String transportType) {
        log.trace("Executing findDeviceProfileInfos tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        Validator.validatePageLink(pageLink);
        return deviceProfileDao.findDeviceProfileInfos(tenantId, pageLink, transportType);
    }

    @Override
    public DeviceProfile findOrCreateDeviceProfile(TenantId tenantId, String name) {
        log.trace("Executing findOrCreateDefaultDeviceProfile");
        DeviceProfile deviceProfile = findDeviceProfileByName(tenantId, name, false);
        if (deviceProfile == null) {
            boolean isDefault = "default".equals(name) && findDefaultDeviceProfile(tenantId) == null;
            try {
                deviceProfile = this.doCreateDeviceProfile(tenantId, name, isDefault, true);
            } catch (DataValidationException e) {
                if (DEVICE_PROFILE_WITH_SUCH_NAME_ALREADY_EXISTS.equals(e.getMessage())) {
                    deviceProfile = findDeviceProfileByName(tenantId, name, false);
                } else {
                    throw e;
                }
            }
        }
        return deviceProfile;
    }

    @Override
    public DeviceProfile createDefaultDeviceProfile(TenantId tenantId) {
        log.trace("Executing createDefaultDeviceProfile tenantId [{}]", tenantId);
        return doCreateDeviceProfile(tenantId, "default", true, false);
    }

    private DeviceProfile doCreateDeviceProfile(TenantId tenantId, String profileName, boolean defaultProfile, boolean publishSaveEvent) {
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setTenantId(tenantId);
        deviceProfile.setDefault(defaultProfile);
        deviceProfile.setName(profileName);
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        deviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        deviceProfile.setDescription("Default device profile");
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
        DefaultDeviceProfileTransportConfiguration transportConfiguration = new DefaultDeviceProfileTransportConfiguration();
        DisabledDeviceProfileProvisionConfiguration provisionConfiguration = new DisabledDeviceProfileProvisionConfiguration(null);
        deviceProfileData.setConfiguration(configuration);
        deviceProfileData.setTransportConfiguration(transportConfiguration);
        deviceProfileData.setProvisionConfiguration(provisionConfiguration);
        deviceProfile.setProfileData(deviceProfileData);
        return saveDeviceProfile(deviceProfile, true, publishSaveEvent);
    }

    @Override
    public DeviceProfile findDefaultDeviceProfile(TenantId tenantId) {
        log.trace("Executing findDefaultDeviceProfile tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return cache.getAndPutInTransaction(DeviceProfileCacheKey.forDefaultProfile(tenantId),
                () -> deviceProfileDao.findDefaultDeviceProfile(tenantId), true);
    }

    @Override
    public DeviceProfileInfo findDefaultDeviceProfileInfo(TenantId tenantId) {
        log.trace("Executing findDefaultDeviceProfileInfo tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return toDeviceProfileInfo(findDefaultDeviceProfile(tenantId));
    }

    @Override
    public boolean setDefaultDeviceProfile(TenantId tenantId, DeviceProfileId deviceProfileId) {
        log.trace("Executing setDefaultDeviceProfile [{}]", deviceProfileId);
        validateId(deviceProfileId, id -> INCORRECT_DEVICE_PROFILE_ID + id);
        DeviceProfile deviceProfile = deviceProfileDao.findById(tenantId, deviceProfileId.getId());
        if (!deviceProfile.isDefault()) {
            deviceProfile.setDefault(true);
            DeviceProfile previousDefaultDeviceProfile = findDefaultDeviceProfile(tenantId);
            boolean changed = false;
            if (previousDefaultDeviceProfile == null) {
                deviceProfileDao.save(tenantId, deviceProfile);
                publishEvictEvent(new DeviceProfileEvictEvent(deviceProfile.getTenantId(), deviceProfile.getName(), null, deviceProfile.getId(), true, deviceProfile.getProvisionDeviceKey()));
                changed = true;
            } else if (!previousDefaultDeviceProfile.getId().equals(deviceProfile.getId())) {
                previousDefaultDeviceProfile.setDefault(false);
                deviceProfileDao.save(tenantId, previousDefaultDeviceProfile);
                deviceProfileDao.save(tenantId, deviceProfile);
                publishEvictEvent(new DeviceProfileEvictEvent(previousDefaultDeviceProfile.getTenantId(), previousDefaultDeviceProfile.getName(), null, previousDefaultDeviceProfile.getId(), false, deviceProfile.getProvisionDeviceKey()));
                publishEvictEvent(new DeviceProfileEvictEvent(deviceProfile.getTenantId(), deviceProfile.getName(), null, deviceProfile.getId(), true, deviceProfile.getProvisionDeviceKey()));
                changed = true;
            }
            return changed;
        }
        return false;
    }

    @Override
    public void deleteDeviceProfilesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteDeviceProfilesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        tenantDeviceProfilesRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteDeviceProfilesByTenantId(tenantId);
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findDeviceProfileById(tenantId, new DeviceProfileId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(deviceProfileDao.findByIdAsync(tenantId, entityId.getId()))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DEVICE_PROFILE;
    }

    @Override
    public List<EntityInfo> findDeviceProfileNamesByTenantId(TenantId tenantId, boolean activeOnly) {
        log.trace("Executing findDeviceProfileNamesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return deviceProfileDao.findTenantDeviceProfileNames(tenantId.getId(), activeOnly)
                .stream().sorted(Comparator.comparing(EntityInfo::getName))
                .collect(Collectors.toList());
    }

    private final PaginatedRemover<TenantId, DeviceProfile> tenantDeviceProfilesRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<DeviceProfile> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return deviceProfileDao.findDeviceProfiles(id, pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, DeviceProfile entity) {
                    removeDeviceProfile(tenantId, entity);
                }
            };

    private DeviceProfileInfo toDeviceProfileInfo(DeviceProfile profile) {
        return profile == null ? null : new DeviceProfileInfo(profile.getId(), profile.getTenantId(), profile.getName(), profile.getImage(),
                profile.getDefaultDashboardId(), profile.getType(), profile.getTransportType());
    }

    private void formatDeviceProfileCertificate(DeviceProfile deviceProfile, X509CertificateChainProvisionConfiguration x509Configuration) {
        String formattedCertificateValue = formatCertificateValue(x509Configuration.getProvisionDeviceSecret());
        String cert = fetchLeafCertificateFromChain(formattedCertificateValue);
        String sha3Hash = EncryptionUtil.getSha3Hash(cert);
        DeviceProfileData deviceProfileData = deviceProfile.getProfileData();
        x509Configuration.setProvisionDeviceSecret(formattedCertificateValue);
        deviceProfileData.setProvisionConfiguration(x509Configuration);
        deviceProfile.setProfileData(deviceProfileData);
        deviceProfile.setProvisionDeviceKey(sha3Hash);
    }

    private String fetchLeafCertificateFromChain(String value) {
        String regex = "-----BEGIN CERTIFICATE-----\\s*.*?\\s*-----END CERTIFICATE-----";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(value);
        if (matcher.find()) {
            // if the method receives a chain it fetches the leaf (end-entity) certificate, else if it gets a single certificate, it returns the single certificate
            return matcher.group(0);
        }
        return value;
    }

    private String formatCertificateValue(String certificateValue) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream inputStream = new ByteArrayInputStream(certificateValue.getBytes());
            Certificate[] certificates = cf.generateCertificates(inputStream).toArray(new Certificate[0]);
            if (certificates.length > 1) {
                return EncryptionUtil.certTrimNewLinesForChainInDeviceProfile(certificateValue);
            }
        } catch (CertificateException ignored) {}
        return EncryptionUtil.certTrimNewLines(certificateValue);
    }

}
