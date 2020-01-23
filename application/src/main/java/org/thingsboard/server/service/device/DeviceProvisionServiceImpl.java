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
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ProvisionProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.cluster.SendToClusterMsg;
import org.thingsboard.server.common.msg.system.ServiceToRuleEngineMsg;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProvisionService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.device.ProvisionProfileDao;
import org.thingsboard.server.dao.device.provision.ProvisionProfile;
import org.thingsboard.server.dao.device.provision.ProvisionProfileCredentials;
import org.thingsboard.server.dao.device.provision.ProvisionRequest;
import org.thingsboard.server.dao.device.provision.ProvisionResponse;
import org.thingsboard.server.dao.device.provision.ProvisionResponseStatus;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantDao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import static org.thingsboard.server.common.data.CacheConstants.DEVICE_PROVISION_CACHE;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class DeviceProvisionServiceImpl extends AbstractEntityService implements DeviceProvisionService {

    private static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    private static final String INCORRECT_PROFILE_ID = "Incorrect profileId ";
    private static final String DEVICE_PROVISIONED = "deviceProvisioned";

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
        provisionProfileValidator.validate(provisionProfile, ProvisionProfile::getTenantId);
        ProvisionProfile savedProvisionProfile;
        if (!sqlDatabaseUsed) {
            savedProvisionProfile = provisionProfileDao.save(provisionProfile.getTenantId(), provisionProfile);
        } else {
            try {
                savedProvisionProfile = provisionProfileDao.save(provisionProfile.getTenantId(), provisionProfile);
            } catch (Exception t) {
                ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
                if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("provision_profile_unq_key")) {
                    log.warn("Profile with such key already exists!", e);
                    log.debug("Generating a new provision key!");
                    provisionProfile.getCredentials().setProvisionProfileKey(generateProvisionProfileCredentials());
                    savedProvisionProfile = saveProvisionProfile(provisionProfile);
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
    public ListenableFuture<ProvisionResponse> provisionDevice(ProvisionRequest provisionRequest) {
        ProvisionProfile targetProfile = provisionProfileDao.findByKey(
                TenantId.SYS_TENANT_ID,
                provisionRequest.getCredentials().getProvisionProfileKey());
        if (targetProfile == null || !provisionRequest.getCredentials().getProvisionProfileSecret().equals(targetProfile.getCredentials().getProvisionProfileSecret())) {
            return Futures.immediateFuture(new ProvisionResponse(null, ProvisionResponseStatus.NOT_FOUND));
        }
        Device device = getOrCreateDevice(provisionRequest, targetProfile);
        if (provisionRequest.isSingleProvisioning()) {
            return processProvision(device);
        } else {
            return Futures.immediateFuture(
                    new ProvisionResponse(
                            deviceCredentialsService.findDeviceCredentialsByDeviceId(device.getTenantId(), device.getId()),
                            ProvisionResponseStatus.SUCCESS));
        }
    }

    private ListenableFuture<ProvisionResponse> processProvision(Device device) {
        ListenableFuture<Optional<AttributeKvEntry>> future = attributesService.find(device.getTenantId(), device.getId(), DataConstants.SERVER_SCOPE, DEVICE_PROVISIONED);
        ListenableFuture<Boolean> booleanFuture = Futures.transformAsync(future, optional -> {
            if (optional.isPresent() && optional.get().getBooleanValue().orElse(false)) {
                return Futures.immediateFuture(true);
            }
            return Futures.transform(attributesService.save(device.getTenantId(), device.getId(), DataConstants.SERVER_SCOPE,
                    Collections.singletonList(new BaseAttributeKvEntry(new BooleanDataEntry(DEVICE_PROVISIONED, true), System.currentTimeMillis()))), input -> false);
        });
        return Futures.transform(booleanFuture, b -> {
            if (b) {
                return new ProvisionResponse(null, ProvisionResponseStatus.DENIED);
            }
            return new ProvisionResponse(deviceCredentialsService.findDeviceCredentialsByDeviceId(device.getTenantId(), device.getId()), ProvisionResponseStatus.SUCCESS);
        });
    }

    private Device getOrCreateDevice(ProvisionRequest provisionRequest, ProvisionProfile profile) {
        Device device = deviceService.findDeviceByTenantIdAndName(profile.getTenantId(), provisionRequest.getDeviceName());
        if (device == null) {
            deviceCreationLock.lock();
            try {
                return processGetOrCreateDevice(provisionRequest, profile);
            } finally {
                deviceCreationLock.unlock();
            }
        }
        return device;
    }

    private Device processGetOrCreateDevice(ProvisionRequest provisionRequest, ProvisionProfile profile) {
        Device device = deviceService.findDeviceByTenantIdAndName(profile.getTenantId(), provisionRequest.getDeviceName());
        if (device == null) {
            device = new Device();
            device.setName(provisionRequest.getDeviceName());
            device.setType(provisionRequest.getDeviceType());
            device.setTenantId(profile.getTenantId());
            if (profile.getCustomerId() != null) {
                device.setCustomerId(profile.getCustomerId());
            }
            device = deviceService.saveDevice(device);

            actorService.onDeviceAdded(device);
            pushDeviceCreatedEventToRuleEngine(device);

            if (!StringUtils.isEmpty(provisionRequest.getX509CertPubKey())) {
                DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(profile.getTenantId(), device.getId());
                deviceCredentials.setCredentialsType(DeviceCredentialsType.X509_CERTIFICATE);
                deviceCredentials.setCredentialsValue(provisionRequest.getX509CertPubKey());
                deviceCredentialsService.updateDeviceCredentials(profile.getTenantId(), deviceCredentials);
            }
        }
        return device;
    }

    private void pushDeviceCreatedEventToRuleEngine(Device device) {
        try {
            ObjectNode entityNode = mapper.valueToTree(device);
            TbMsg msg = new TbMsg(UUIDs.timeBased(), DataConstants.ENTITY_CREATED, device.getId(), deviceActionTbMsgMetaData(device), mapper.writeValueAsString(entityNode), null, null, 0L);
            actorService.onMsg(new SendToClusterMsg(device.getId(), new ServiceToRuleEngineMsg(device.getTenantId(), msg)));
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.warn("[{}] Failed to push device action to rule engine: {}", device.getId(), DataConstants.ENTITY_CREATED, e);
        }
    }

    private TbMsgMetaData deviceActionTbMsgMetaData(Device device) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("tenantId", device.getTenantId().toString());
        CustomerId customerId = device.getCustomerId();
        if (customerId != null && !customerId.isNullUid()) {
            metaData.putValue("customerId", customerId.toString());
        }
        return metaData;
    }

    private String generateProvisionProfileCredentials() {
        return RandomStringUtils.randomAlphanumeric(20);
    }

    private DataValidator<ProvisionProfile> provisionProfileValidator =
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
                    ProvisionProfile existingProfileEntity = provisionProfileDao.findById(tenantId, profile.getUuidId());
                    if (existingProfileEntity == null) {
                        throw new DataValidationException("Unable to update non-existent provision profile!");
                    }
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
                    if (profile.getCredentials() == null) {
                        profile.setCredentials(
                                new ProvisionProfileCredentials(
                                        generateProvisionProfileCredentials(),
                                        generateProvisionProfileCredentials()));
                    } else {
                        ProvisionProfileCredentials credentials = profile.getCredentials();
                        if (StringUtils.isEmpty(profile.getCredentials().getProvisionProfileKey())) {
                            credentials.setProvisionProfileKey(generateProvisionProfileCredentials());
                        }
                        if (StringUtils.isEmpty(profile.getCredentials().getProvisionProfileSecret())) {
                            credentials.setProvisionProfileSecret(generateProvisionProfileCredentials());
                        }
                    }
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
                }
            };
}
