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
package org.thingsboard.server.service.device;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ProvisionProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.cluster.SendToClusterMsg;
import org.thingsboard.server.common.msg.system.ServiceToRuleEngineMsg;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.provisionprofile.DeviceProvisionService;
import org.thingsboard.server.dao.provisionprofile.ProvisionProfileDao;
import org.thingsboard.server.dao.provisionprofile.provision.ProvisionProfile;
import org.thingsboard.server.dao.provisionprofile.provision.ProvisionProfileCredentials;
import org.thingsboard.server.dao.provisionprofile.provision.ProvisionRequest;
import org.thingsboard.server.dao.provisionprofile.provision.ProvisionRequestValidationStrategy;
import org.thingsboard.server.dao.provisionprofile.provision.ProvisionRequestValidationStrategyType;
import org.thingsboard.server.dao.provisionprofile.provision.ProvisionResponse;
import org.thingsboard.server.dao.provisionprofile.provision.ProvisionResponseStatus;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import static org.thingsboard.server.common.data.CacheConstants.DEVICE_PROVISION_CACHE;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service
@Slf4j
public class DeviceProvisionServiceImpl extends AbstractEntityService implements DeviceProvisionService {

    private static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_PAGE_LINK = "Incorrect page link ";
    private static final String INCORRECT_PROFILE_ID = "Incorrect profileId ";

    private static final String DEVICE_PROVISION_STATE = "provisionState";
    private static final String PROVISIONED_STATE = "provisioned";

    private static final UserId PROVISION_USER_ID = UserId.fromString(NULL_UUID.toString());

    private final ObjectMapper mapper = new ObjectMapper();
    private final ReentrantLock deviceCreationLock = new ReentrantLock();

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private ProvisionProfileDao provisionProfileDao;

    @Autowired
    private ActorService actorService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private CacheManager cacheManager;

    @Override
    public ProvisionProfile findProfileById(TenantId tenantId, ProvisionProfileId profileId) {
        log.trace("Executing findProfileById [{}]", profileId);
        validateId(profileId, INCORRECT_PROFILE_ID + profileId);
        return provisionProfileDao.findById(tenantId, profileId.getId());
    }

    @Override
    public ListenableFuture<ProvisionProfile> findProfileByIdAsync(TenantId tenantId, ProvisionProfileId profileId) {
        log.trace("Executing findProfileByIdAsync [{}]", profileId);
        validateId(profileId, INCORRECT_PROFILE_ID + profileId);
        return provisionProfileDao.findByIdAsync(tenantId, profileId.getId());
    }

    @CacheEvict(cacheNames = DEVICE_PROVISION_CACHE, key = "{#provisionProfile.tenantId, #provisionProfile.credentials.provisionProfileKey}")
    @Override
    public ProvisionProfile saveProvisionProfile(ProvisionProfile provisionProfile) {
        log.trace("Executing saveProvisionProfile [{}]", provisionProfile);
        boolean isKeyPresent = false;
        if (provisionProfile.getCredentials() != null && !StringUtils.isEmpty(provisionProfile.getCredentials().getProvisionProfileKey())) {
            isKeyPresent = true;
            provisionProfileValidator.validate(provisionProfile, ProvisionProfile::getTenantId);
        } else {
            emptyProvisionProfileValidator.validate(provisionProfile, ProvisionProfile::getTenantId);
        }
        ProvisionProfile savedProvisionProfile;
        if (!sqlDatabaseUsed) {
            savedProvisionProfile = provisionProfileDao.save(provisionProfile.getTenantId(), provisionProfile);
        } else {
            try {
                savedProvisionProfile = provisionProfileDao.save(provisionProfile.getTenantId(), provisionProfile);
            } catch (Exception t) {
                ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
                if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("provision_profile_unq_key")) {
                    if (isKeyPresent) {
                        throw new DataValidationException("Profile with such key already exists!");
                    } else {
                        log.warn("Profile with such key already exists!", e);
                        log.debug("Prepare to generate a new provision key!");
                        provisionProfile.getCredentials().setProvisionProfileKey(null);
                        savedProvisionProfile = saveProvisionProfile(provisionProfile);
                    }
                } else {
                    throw t;
                }
            }
        }
        return savedProvisionProfile;
    }

    @Cacheable(cacheNames = DEVICE_PROVISION_CACHE, key = "{#tenantId, #key}")
    @Override
    public ProvisionProfile findProvisionProfileByKey(TenantId tenantId, String key) {
        log.trace("Executing findProvisionProfileByKey [{}][{}]", tenantId, key);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return provisionProfileDao.findByKey(tenantId, key);
    }

    @Override
    public void deleteProfile(TenantId tenantId, ProvisionProfileId profileId) {
        log.trace("Executing deleteProfile [{}]", profileId);
        validateId(profileId, INCORRECT_PROFILE_ID + profileId);

        ProvisionProfile profile = provisionProfileDao.findById(tenantId, profileId.getId());
        List<Object> list = new ArrayList<>();
        list.add(profile.getTenantId());
        list.add(profile.getCredentials().getProvisionProfileKey());
        Cache cache = cacheManager.getCache(DEVICE_PROVISION_CACHE);
        cache.evict(list);

        provisionProfileDao.removeById(tenantId, profileId.getId());
    }

    @Override
    public ProvisionProfile assignProfileToCustomer(TenantId tenantId, ProvisionProfileId profileId, CustomerId customerId) {
        ProvisionProfile profile = findProfileById(tenantId, profileId);
        profile.setCustomerId(customerId);
        return saveProvisionProfile(profile);
    }

    @Override
    public ProvisionProfile unassignProfileFromCustomer(TenantId tenantId, ProvisionProfileId profileId) {
        ProvisionProfile profile = findProfileById(tenantId, profileId);
        profile.setCustomerId(null);
        return saveProvisionProfile(profile);
    }

    @Override
    public TextPageData<ProvisionProfile> findProfilesByTenantId(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findProfilesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<ProvisionProfile> provisionProfiles = provisionProfileDao.findProfilesByTenantId(tenantId.getId(), pageLink);
        return new TextPageData<>(provisionProfiles, pageLink);
    }

    @Override
    public ListenableFuture<ProvisionResponse> provisionDevice(ProvisionRequest provisionRequest) {
        ProvisionProfile targetProfile = provisionProfileDao.findByKey(
                TenantId.SYS_TENANT_ID,
                provisionRequest.getCredentials().getProvisionProfileKey());
        if (targetProfile == null || !provisionRequest.getCredentials().getProvisionProfileSecret().equals(targetProfile.getCredentials().getProvisionProfileSecret())) {
            return Futures.immediateFuture(new ProvisionResponse(null, ProvisionResponseStatus.NOT_FOUND));
        }

        Device device = deviceService.findDeviceByTenantIdAndName(targetProfile.getTenantId(), provisionRequest.getDeviceName());
        switch (targetProfile.getStrategy().getValidationStrategyType()) {
            case CHECK_NEW_DEVICE:
                if (device == null) {
                    return createDevice(provisionRequest, targetProfile);
                } else {
                    log.warn("[{}] The device is present and could not be provisioned once more!", device.getName());
                    notify(device, provisionRequest, DataConstants.PROVISION_FAILURE, false);
                    return Futures.immediateFuture(new ProvisionResponse(null, ProvisionResponseStatus.FAILURE));
                }
            case CHECK_PRE_PROVISIONED_DEVICE:
                if (device == null) {
                    log.warn("[{}] Failed to find pre provisioned device!", provisionRequest.getDeviceName());
                    return Futures.immediateFuture(new ProvisionResponse(null, ProvisionResponseStatus.FAILURE));
                } else {
                    return processProvision(device, provisionRequest);
                }
            default:
                throw new RuntimeException("Strategy is not supported - " + targetProfile.getStrategy().getValidationStrategyType().name());
        }
    }

    private ListenableFuture<ProvisionResponse> processProvision(Device device, ProvisionRequest provisionRequest) {
        ListenableFuture<Optional<AttributeKvEntry>> provisionStateFuture = attributesService.find(device.getTenantId(), device.getId(),
                DataConstants.SERVER_SCOPE, DEVICE_PROVISION_STATE);
        ListenableFuture<Boolean> provisionedFuture = Futures.transformAsync(provisionStateFuture, optionalAtr -> {
            if (optionalAtr.isPresent()) {
                String state = optionalAtr.get().getValueAsString();
                if (state.equals(PROVISIONED_STATE)) {
                    return Futures.immediateFuture(true);
                } else {
                    log.error("[{}][{}] Unknown provision state: {}!", device.getName(), DEVICE_PROVISION_STATE, state);
                    return Futures.immediateCancelledFuture();
                }
            }
            return Futures.transform(saveProvisionStateAttribute(device), input -> false);
        });
        if (provisionedFuture.isCancelled()) {
            throw new RuntimeException("Unknown provision state!");
        }
        return Futures.transform(provisionedFuture, provisioned -> {
            if (provisioned) {
                notify(device, provisionRequest, DataConstants.PROVISION_FAILURE, false);
                return new ProvisionResponse(null, ProvisionResponseStatus.FAILURE);
            }
            notify(device, provisionRequest, DataConstants.PROVISION_SUCCESS, true);
            return new ProvisionResponse(deviceCredentialsService.findDeviceCredentialsByDeviceId(device.getTenantId(), device.getId()), ProvisionResponseStatus.SUCCESS);
        });
    }

    private ListenableFuture<ProvisionResponse> createDevice(ProvisionRequest provisionRequest, ProvisionProfile profile) {
        deviceCreationLock.lock();
        try {
            return processCreateDevice(provisionRequest, profile);
        } finally {
            deviceCreationLock.unlock();
        }
    }

    private ListenableFuture<ProvisionResponse> processCreateDevice(ProvisionRequest provisionRequest, ProvisionProfile profile) {
        Device device = deviceService.findDeviceByTenantIdAndName(profile.getTenantId(), provisionRequest.getDeviceName());
        if (device == null) {
            Device savedDevice = saveDevice(provisionRequest, profile);

            actorService.onDeviceAdded(savedDevice);
            pushDeviceCreatedEventToRuleEngine(savedDevice);
            notify(savedDevice, provisionRequest, DataConstants.PROVISION_SUCCESS, true);

            return Futures.transform(saveProvisionStateAttribute(savedDevice), input ->
                    new ProvisionResponse(
                            getDeviceCredentials(savedDevice, provisionRequest.getX509CertPubKey()),
                            ProvisionResponseStatus.SUCCESS));
        }
        log.warn("[{}] The device is already provisioned!", device.getName());
        notify(device, provisionRequest, DataConstants.PROVISION_FAILURE, false);
        return Futures.immediateFuture(new ProvisionResponse(null, ProvisionResponseStatus.FAILURE));
    }

    private ListenableFuture<List<Void>> saveProvisionStateAttribute(Device device) {
        return attributesService.save(device.getTenantId(), device.getId(), DataConstants.SERVER_SCOPE,
                Collections.singletonList(new BaseAttributeKvEntry(new StringDataEntry(DEVICE_PROVISION_STATE, PROVISIONED_STATE),
                        System.currentTimeMillis())));
    }

    private Device saveDevice(ProvisionRequest provisionRequest, ProvisionProfile profile) {
        Device device = new Device();
        device.setName(provisionRequest.getDeviceName());
        device.setType(provisionRequest.getDeviceType());
        device.setTenantId(profile.getTenantId());
        if (profile.getCustomerId() != null) {
            device.setCustomerId(profile.getCustomerId());
        }
        return deviceService.saveDevice(device);
    }

    private DeviceCredentials getDeviceCredentials(Device device, String x509CertPubKey) {
        DeviceCredentials credentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(device.getTenantId(), device.getId());
        if (!StringUtils.isEmpty(x509CertPubKey)) {
            credentials.setCredentialsType(DeviceCredentialsType.X509_CERTIFICATE);
            credentials.setCredentialsValue(x509CertPubKey);
            return deviceCredentialsService.updateDeviceCredentials(device.getTenantId(), credentials);
        }
        return credentials;
    }

    private void notify(Device device, ProvisionRequest provisionRequest, String type, boolean success) {
        pushProvisionEventToRuleEngine(provisionRequest, device, type);
        logAction(device.getTenantId(), device.getCustomerId(), device, success, provisionRequest);
    }

    private void pushProvisionEventToRuleEngine(ProvisionRequest request, Device device, String type) {
        try {
            ObjectNode entityNode = mapper.valueToTree(request);
            TbMsg msg = new TbMsg(UUIDs.timeBased(), type, device.getId(), createTbMsgMetaData(device), mapper.writeValueAsString(entityNode), null, null, 0L);
            actorService.onMsg(new SendToClusterMsg(device.getId(), new ServiceToRuleEngineMsg(device.getTenantId(), msg)));
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[{}] Failed to push device action to rule engine: {}", device.getId(), type, e);
        }
    }

    private void pushDeviceCreatedEventToRuleEngine(Device device) {
        try {
            ObjectNode entityNode = mapper.valueToTree(device);
            TbMsg msg = new TbMsg(UUIDs.timeBased(), DataConstants.ENTITY_CREATED, device.getId(), createTbMsgMetaData(device), mapper.writeValueAsString(entityNode), null, null, 0L);
            actorService.onMsg(new SendToClusterMsg(device.getId(), new ServiceToRuleEngineMsg(device.getTenantId(), msg)));
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[{}] Failed to push device action to rule engine: {}", device.getId(), DataConstants.ENTITY_CREATED, e);
        }
    }

    private TbMsgMetaData createTbMsgMetaData(Device device) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("tenantId", device.getTenantId().toString());
        CustomerId customerId = device.getCustomerId();
        if (customerId != null && !customerId.isNullUid()) {
            metaData.putValue("customerId", customerId.toString());
        }
        return metaData;
    }

    private void logAction(TenantId tenantId, CustomerId customerId, Device device, boolean success, ProvisionRequest provisionRequest) {
        ActionType actionType = success ? ActionType.PROVISION_SUCCESS : ActionType.PROVISION_FAILURE;
        auditLogService.logEntityAction(tenantId, customerId, PROVISION_USER_ID, device.getName(), device.getId(), device, actionType, null, provisionRequest);
    }

    private String generateProvisionProfileCredentials() {
        return RandomStringUtils.randomAlphanumeric(20);
    }

    private DataValidator<ProvisionProfile> emptyProvisionProfileValidator =
            new DataValidator<ProvisionProfile>() {

                @Override
                protected void validateCreate(TenantId tenantId, ProvisionProfile profile) {
                    if (!sqlDatabaseUsed) {
                        ProvisionProfile existingProfileEntity = provisionProfileDao.findByKey(tenantId,
                                profile.getCredentials().getProvisionProfileKey());
                        if (existingProfileEntity != null) {
                            log.debug("Generating a new provision key for create!");
                            profile.getCredentials().setProvisionProfileKey(generateProvisionProfileCredentials());
                            validateCreate(tenantId, profile);
                        }
                    }
                }

                @Override
                protected void validateUpdate(TenantId tenantId, ProvisionProfile profile) {
                    validateById(tenantId, profile.getUuidId());
                    if (!sqlDatabaseUsed) {
                        ProvisionProfile sameProvisionProfileKey = provisionProfileDao.findByKey(tenantId,
                                profile.getCredentials().getProvisionProfileKey());
                        if (sameProvisionProfileKey != null && !sameProvisionProfileKey.getUuidId().equals(profile.getUuidId())) {
                            log.debug("Generating a new provision key for update!");
                            profile.getCredentials().setProvisionProfileKey(generateProvisionProfileCredentials());
                            validateUpdate(tenantId, profile);
                        }
                    }
                }

                @Override
                protected void validateDataImpl(TenantId tenantId, ProvisionProfile profile) {
                    validateProfile(profile);
                    if (profile.getCredentials() == null) {
                        profile.setCredentials(
                                new ProvisionProfileCredentials(
                                        generateProvisionProfileCredentials(),
                                        generateProvisionProfileCredentials()));
                    } else {
                        ProvisionProfileCredentials credentials = profile.getCredentials();
                        if (StringUtils.isEmpty(credentials.getProvisionProfileKey())) {
                            credentials.setProvisionProfileKey(generateProvisionProfileCredentials());
                        }
                        if (StringUtils.isEmpty(credentials.getProvisionProfileSecret())) {
                            credentials.setProvisionProfileSecret(generateProvisionProfileCredentials());
                        }
                    }
                }
            };

    private DataValidator<ProvisionProfile> provisionProfileValidator =
            new DataValidator<ProvisionProfile>() {

                @Override
                protected void validateCreate(TenantId tenantId, ProvisionProfile profile) {
                    if (!sqlDatabaseUsed) {
                        ProvisionProfile existingProfileEntity = provisionProfileDao.findByKey(tenantId,
                                profile.getCredentials().getProvisionProfileKey());
                        if (existingProfileEntity != null) {
                            throw new DataValidationException("Profile with such key already exists!");
                        }
                    }
                }

                @Override
                protected void validateUpdate(TenantId tenantId, ProvisionProfile profile) {
                    validateById(tenantId, profile.getUuidId());
                    if (!sqlDatabaseUsed) {
                        ProvisionProfile sameProvisionProfileKey = provisionProfileDao.findByKey(tenantId,
                                profile.getCredentials().getProvisionProfileKey());
                        if (sameProvisionProfileKey != null && !sameProvisionProfileKey.getUuidId().equals(profile.getUuidId())) {
                            throw new DataValidationException("Profile with such key already exists!");
                        }
                    }
                }

                @Override
                protected void validateDataImpl(TenantId tenantId, ProvisionProfile profile) {
                    validateProfile(profile);
                    if (StringUtils.isEmpty(profile.getCredentials().getProvisionProfileSecret())) {
                        profile.getCredentials().setProvisionProfileSecret(generateProvisionProfileCredentials());
                    }
                }
            };

    private void validateById(TenantId tenantId, UUID uuid) {
        if (provisionProfileDao.findById(tenantId, uuid) == null) {
            throw new DataValidationException("Unable to update non-existent provision profile!");
        }
    }

    private void validateProfile(ProvisionProfile profile) {
        if (profile.getTenantId() == null) {
            throw new DataValidationException("Profile should be assigned to tenant!");
        } else {
            Tenant tenant = tenantDao.findById(profile.getTenantId(), profile.getTenantId().getId());
            if (tenant == null) {
                throw new DataValidationException("Profile is referencing to non-existent tenant!");
            }
        }
        if (profile.getCustomerId() == null) {
            profile.setCustomerId(new CustomerId(NULL_UUID));
        } else if (!profile.getCustomerId().getId().equals(NULL_UUID)) {
            Customer customer = customerDao.findById(profile.getTenantId(), profile.getCustomerId().getId());
            if (customer == null) {
                throw new DataValidationException("Can't assign profile to non-existent customer!");
            }
            if (!customer.getTenantId().getId().equals(profile.getTenantId().getId())) {
                throw new DataValidationException("Can't assign profile to customer from different tenant!");
            }
        }
        if (profile.getStrategy() == null) {
            profile.setStrategy(new ProvisionRequestValidationStrategy(ProvisionRequestValidationStrategyType.CHECK_NEW_DEVICE));
        }
    }
}
