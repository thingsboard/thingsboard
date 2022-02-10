/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.util.SecurityUtil;
import org.thingsboard.server.common.data.DynamicProtoUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.thingsboard.server.common.data.DashboardInfo;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileInfo;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.OtaPackage;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;
import org.thingsboard.server.common.data.device.profile.CoapDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.CoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultCoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileAlarm;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.ProtoTransportPayloadConfiguration;
import org.thingsboard.server.common.data.device.profile.TransportPayloadTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.AbstractLwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.RPKLwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.LwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.device.profile.lwm2m.bootstrap.X509LwM2MBootstrapServerCredential;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.msg.EncryptionUtil;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.DeviceCredentialsValidationException;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.queue.QueueService;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.thingsboard.server.common.data.CacheConstants.DEVICE_PROFILE_CACHE;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class DeviceProfileServiceImpl extends AbstractEntityService implements DeviceProfileService {

    private static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    private static final String INCORRECT_DEVICE_PROFILE_ID = "Incorrect deviceProfileId ";
    private static final String INCORRECT_DEVICE_PROFILE_NAME = "Incorrect deviceProfileName ";

    private static final String ATTRIBUTES_PROTO_SCHEMA = "attributes proto schema";
    private static final String TELEMETRY_PROTO_SCHEMA = "telemetry proto schema";
    private static final String RPC_REQUEST_PROTO_SCHEMA = "rpc request proto schema";
    private static final String RPC_RESPONSE_PROTO_SCHEMA = "rpc response proto schema";
    private static final String EXCEPTION_PREFIX = "[Transport Configuration]";

    @Autowired(required = false)
    private QueueService queueService;

    @Autowired
    private DeviceProfileDao deviceProfileDao;

    @Autowired
    private DeviceDao deviceDao;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private OtaPackageService otaPackageService;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private DashboardService dashboardService;

    private final Lock findOrCreateLock = new ReentrantLock();

    @Cacheable(cacheNames = DEVICE_PROFILE_CACHE, key = "{#deviceProfileId.id}")
    @Override
    public DeviceProfile findDeviceProfileById(TenantId tenantId, DeviceProfileId deviceProfileId) {
        log.trace("Executing findDeviceProfileById [{}]", deviceProfileId);
        Validator.validateId(deviceProfileId, INCORRECT_DEVICE_PROFILE_ID + deviceProfileId);
        return deviceProfileDao.findById(tenantId, deviceProfileId.getId());
    }

    @Override
    public DeviceProfile findDeviceProfileByName(TenantId tenantId, String profileName) {
        log.trace("Executing findDeviceProfileByName [{}][{}]", tenantId, profileName);
        Validator.validateString(profileName, INCORRECT_DEVICE_PROFILE_NAME + profileName);
        return deviceProfileDao.findByName(tenantId, profileName);
    }

    @Cacheable(cacheNames = DEVICE_PROFILE_CACHE, key = "{'info', #deviceProfileId.id}")
    @Override
    public DeviceProfileInfo findDeviceProfileInfoById(TenantId tenantId, DeviceProfileId deviceProfileId) {
        log.trace("Executing findDeviceProfileById [{}]", deviceProfileId);
        Validator.validateId(deviceProfileId, INCORRECT_DEVICE_PROFILE_ID + deviceProfileId);
        return deviceProfileDao.findDeviceProfileInfoById(tenantId, deviceProfileId.getId());
    }

    @Override
    public DeviceProfile saveDeviceProfile(DeviceProfile deviceProfile) {
        log.trace("Executing saveDeviceProfile [{}]", deviceProfile);
        deviceProfileValidator.validate(deviceProfile, DeviceProfile::getTenantId);
        DeviceProfile oldDeviceProfile = null;
        if (deviceProfile.getId() != null) {
            oldDeviceProfile = deviceProfileDao.findById(deviceProfile.getTenantId(), deviceProfile.getId().getId());
        }
        DeviceProfile savedDeviceProfile;
        try {
            savedDeviceProfile = deviceProfileDao.saveAndFlush(deviceProfile.getTenantId(), deviceProfile);
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("device_profile_name_unq_key")) {
                throw new DataValidationException("Device profile with such name already exists!");
            } else if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("device_provision_key_unq_key")) {
                throw new DataValidationException("Device profile with such provision device key already exists!");
            } else {
                throw t;
            }
        }
        Cache cache = cacheManager.getCache(DEVICE_PROFILE_CACHE);
        cache.evict(Collections.singletonList(savedDeviceProfile.getId().getId()));
        cache.evict(Arrays.asList("info", savedDeviceProfile.getId().getId()));
        cache.evict(Arrays.asList(deviceProfile.getTenantId().getId(), deviceProfile.getName()));
        if (savedDeviceProfile.isDefault()) {
            cache.evict(Arrays.asList("default", savedDeviceProfile.getTenantId().getId()));
            cache.evict(Arrays.asList("default", "info", savedDeviceProfile.getTenantId().getId()));
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
    public void deleteDeviceProfile(TenantId tenantId, DeviceProfileId deviceProfileId) {
        log.trace("Executing deleteDeviceProfile [{}]", deviceProfileId);
        Validator.validateId(deviceProfileId, INCORRECT_DEVICE_PROFILE_ID + deviceProfileId);
        DeviceProfile deviceProfile = deviceProfileDao.findById(tenantId, deviceProfileId.getId());
        if (deviceProfile != null && deviceProfile.isDefault()) {
            throw new DataValidationException("Deletion of Default Device Profile is prohibited!");
        }
        this.removeDeviceProfile(tenantId, deviceProfile);
    }

    private void removeDeviceProfile(TenantId tenantId, DeviceProfile deviceProfile) {
        DeviceProfileId deviceProfileId = deviceProfile.getId();
        try {
            deviceProfileDao.removeById(tenantId, deviceProfileId.getId());
        } catch (Exception t) {
            ConstraintViolationException e = extractConstraintViolationException(t).orElse(null);
            if (e != null && e.getConstraintName() != null && e.getConstraintName().equalsIgnoreCase("fk_device_profile")) {
                throw new DataValidationException("The device profile referenced by the devices cannot be deleted!");
            } else {
                throw t;
            }
        }
        deleteEntityRelations(tenantId, deviceProfileId);
        Cache cache = cacheManager.getCache(DEVICE_PROFILE_CACHE);
        cache.evict(Collections.singletonList(deviceProfileId.getId()));
        cache.evict(Arrays.asList("info", deviceProfileId.getId()));
        cache.evict(Arrays.asList(tenantId.getId(), deviceProfile.getName()));
    }

    @Override
    public PageData<DeviceProfile> findDeviceProfiles(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findDeviceProfiles tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return deviceProfileDao.findDeviceProfiles(tenantId, pageLink);
    }

    @Override
    public PageData<DeviceProfileInfo> findDeviceProfileInfos(TenantId tenantId, PageLink pageLink, String transportType) {
        log.trace("Executing findDeviceProfileInfos tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Validator.validatePageLink(pageLink);
        return deviceProfileDao.findDeviceProfileInfos(tenantId, pageLink, transportType);
    }

    @Cacheable(cacheNames = DEVICE_PROFILE_CACHE, key = "{#tenantId.id, #name}")
    @Override
    public DeviceProfile findOrCreateDeviceProfile(TenantId tenantId, String name) {
        log.trace("Executing findOrCreateDefaultDeviceProfile");
        DeviceProfile deviceProfile = findDeviceProfileByName(tenantId, name);
        if (deviceProfile == null) {
            findOrCreateLock.lock();
            try {
                deviceProfile = findDeviceProfileByName(tenantId, name);
                if (deviceProfile == null) {
                    deviceProfile = this.doCreateDefaultDeviceProfile(tenantId, name, name.equals("default"));
                }
            } finally {
                findOrCreateLock.unlock();
            }
        }
        return deviceProfile;
    }

    @Override
    public DeviceProfile createDefaultDeviceProfile(TenantId tenantId) {
        log.trace("Executing createDefaultDeviceProfile tenantId [{}]", tenantId);
        return doCreateDefaultDeviceProfile(tenantId, "default", true);
    }

    private DeviceProfile doCreateDefaultDeviceProfile(TenantId tenantId, String profileName, boolean defaultProfile) {
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
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
    public DeviceProfileInfo findDefaultDeviceProfileInfo(TenantId tenantId) {
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
                cache.evict(Arrays.asList(tenantId.getId(), previousDefaultDeviceProfile.getName()));
                changed = true;
            }
            if (changed) {
                cache.evict(Collections.singletonList(deviceProfile.getId().getId()));
                cache.evict(Arrays.asList("info", deviceProfile.getId().getId()));
                cache.evict(Arrays.asList("default", tenantId.getId()));
                cache.evict(Arrays.asList("default", "info", tenantId.getId()));
                cache.evict(Arrays.asList(tenantId.getId(), deviceProfile.getName()));
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
            new DataValidator<>() {
                @Override
                protected void validateDataImpl(TenantId tenantId, DeviceProfile deviceProfile) {
                    if (StringUtils.isEmpty(deviceProfile.getName())) {
                        throw new DataValidationException("Device profile name should be specified!");
                    }
                    if (deviceProfile.getType() == null) {
                        throw new DataValidationException("Device profile type should be specified!");
                    }
                    if (deviceProfile.getTransportType() == null) {
                        throw new DataValidationException("Device profile transport type should be specified!");
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
                    if (!StringUtils.isEmpty(deviceProfile.getDefaultQueueName()) && queueService != null) {
                        if (!queueService.getQueuesByServiceType(ServiceType.TB_RULE_ENGINE).contains(deviceProfile.getDefaultQueueName())) {
                            throw new DataValidationException("Device profile is referencing to non-existent queue!");
                        }
                    }
                    if (deviceProfile.getProvisionType() == null) {
                        deviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
                    }
                    DeviceProfileTransportConfiguration transportConfiguration = deviceProfile.getProfileData().getTransportConfiguration();
                    transportConfiguration.validate();
                    if (transportConfiguration instanceof MqttDeviceProfileTransportConfiguration) {
                        MqttDeviceProfileTransportConfiguration mqttTransportConfiguration = (MqttDeviceProfileTransportConfiguration) transportConfiguration;
                        if (mqttTransportConfiguration.getTransportPayloadTypeConfiguration() instanceof ProtoTransportPayloadConfiguration) {
                            ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration =
                                    (ProtoTransportPayloadConfiguration) mqttTransportConfiguration.getTransportPayloadTypeConfiguration();
                            validateProtoSchemas(protoTransportPayloadConfiguration);
                            validateTelemetryDynamicMessageFields(protoTransportPayloadConfiguration);
                            validateRpcRequestDynamicMessageFields(protoTransportPayloadConfiguration);
                        }
                    } else if (transportConfiguration instanceof CoapDeviceProfileTransportConfiguration) {
                        CoapDeviceProfileTransportConfiguration coapDeviceProfileTransportConfiguration = (CoapDeviceProfileTransportConfiguration) transportConfiguration;
                        CoapDeviceTypeConfiguration coapDeviceTypeConfiguration = coapDeviceProfileTransportConfiguration.getCoapDeviceTypeConfiguration();
                        if (coapDeviceTypeConfiguration instanceof DefaultCoapDeviceTypeConfiguration) {
                            DefaultCoapDeviceTypeConfiguration defaultCoapDeviceTypeConfiguration = (DefaultCoapDeviceTypeConfiguration) coapDeviceTypeConfiguration;
                            TransportPayloadTypeConfiguration transportPayloadTypeConfiguration = defaultCoapDeviceTypeConfiguration.getTransportPayloadTypeConfiguration();
                            if (transportPayloadTypeConfiguration instanceof ProtoTransportPayloadConfiguration) {
                                ProtoTransportPayloadConfiguration protoTransportPayloadConfiguration = (ProtoTransportPayloadConfiguration) transportPayloadTypeConfiguration;
                                validateProtoSchemas(protoTransportPayloadConfiguration);
                                validateTelemetryDynamicMessageFields(protoTransportPayloadConfiguration);
                                validateRpcRequestDynamicMessageFields(protoTransportPayloadConfiguration);
                            }
                        }
                    } else if (transportConfiguration instanceof Lwm2mDeviceProfileTransportConfiguration) {
                        List<LwM2MBootstrapServerCredential> lwM2MBootstrapServersConfigurations = ((Lwm2mDeviceProfileTransportConfiguration) transportConfiguration).getBootstrap();
                        if (lwM2MBootstrapServersConfigurations != null) {
                            validateLwm2mServersConfigOfBootstrapForClient(lwM2MBootstrapServersConfigurations,
                                    ((Lwm2mDeviceProfileTransportConfiguration) transportConfiguration).isBootstrapServerUpdateEnable());
                            for (LwM2MBootstrapServerCredential bootstrapServerCredential : lwM2MBootstrapServersConfigurations) {
                                validateLwm2mServersCredentialOfBootstrapForClient(bootstrapServerCredential);
                            }
                        }
                    }

                    List<DeviceProfileAlarm> profileAlarms = deviceProfile.getProfileData().getAlarms();

                    if (!CollectionUtils.isEmpty(profileAlarms)) {
                        Set<String> alarmTypes = new HashSet<>();
                        for (DeviceProfileAlarm alarm : profileAlarms) {
                            String alarmType = alarm.getAlarmType();
                            if (StringUtils.isEmpty(alarmType)) {
                                throw new DataValidationException("Alarm rule type should be specified!");
                            }
                            if (!alarmTypes.add(alarmType)) {
                                throw new DataValidationException(String.format("Can't create device profile with the same alarm rule types: \"%s\"!", alarmType));
                            }
                        }
                    }

                    if (deviceProfile.getDefaultRuleChainId() != null) {
                        RuleChain ruleChain = ruleChainService.findRuleChainById(tenantId, deviceProfile.getDefaultRuleChainId());
                        if (ruleChain == null) {
                            throw new DataValidationException("Can't assign non-existent rule chain!");
                        }
                    }

                    if (deviceProfile.getDefaultDashboardId() != null) {
                        DashboardInfo dashboard = dashboardService.findDashboardInfoById(tenantId, deviceProfile.getDefaultDashboardId());
                        if (dashboard == null) {
                            throw new DataValidationException("Can't assign non-existent dashboard!");
                        }
                    }

                    if (deviceProfile.getFirmwareId() != null) {
                        OtaPackage firmware = otaPackageService.findOtaPackageById(tenantId, deviceProfile.getFirmwareId());
                        if (firmware == null) {
                            throw new DataValidationException("Can't assign non-existent firmware!");
                        }
                        if (!firmware.getType().equals(OtaPackageType.FIRMWARE)) {
                            throw new DataValidationException("Can't assign firmware with type: " + firmware.getType());
                        }
                        if (firmware.getData() == null && !firmware.hasUrl()) {
                            throw new DataValidationException("Can't assign firmware with empty data!");
                        }
                        if (!firmware.getDeviceProfileId().equals(deviceProfile.getId())) {
                            throw new DataValidationException("Can't assign firmware with different deviceProfile!");
                        }
                    }

                    if (deviceProfile.getSoftwareId() != null) {
                        OtaPackage software = otaPackageService.findOtaPackageById(tenantId, deviceProfile.getSoftwareId());
                        if (software == null) {
                            throw new DataValidationException("Can't assign non-existent software!");
                        }
                        if (!software.getType().equals(OtaPackageType.SOFTWARE)) {
                            throw new DataValidationException("Can't assign software with type: " + software.getType());
                        }
                        if (software.getData() == null && !software.hasUrl()) {
                            throw new DataValidationException("Can't assign software with empty data!");
                        }
                        if (!software.getDeviceProfileId().equals(deviceProfile.getId())) {
                            throw new DataValidationException("Can't assign firmware with different deviceProfile!");
                        }
                    }
                }

                @Override
                protected void validateUpdate(TenantId tenantId, DeviceProfile deviceProfile) {
                    DeviceProfile old = deviceProfileDao.findById(deviceProfile.getTenantId(), deviceProfile.getId().getId());
                    if (old == null) {
                        throw new DataValidationException("Can't update non existing device profile!");
                    }
                    boolean profileTypeChanged = !old.getType().equals(deviceProfile.getType());
                    boolean transportTypeChanged = !old.getTransportType().equals(deviceProfile.getTransportType());
                    if (profileTypeChanged || transportTypeChanged) {
                        Long profileDeviceCount = deviceDao.countDevicesByDeviceProfileId(deviceProfile.getTenantId(), deviceProfile.getId().getId());
                        if (profileDeviceCount > 0) {
                            String message = null;
                            if (profileTypeChanged) {
                                message = "Can't change device profile type because devices referenced it!";
                            } else if (transportTypeChanged) {
                                message = "Can't change device profile transport type because devices referenced it!";
                            }
                            throw new DataValidationException(message);
                        }
                    }
                }

                private void validateProtoSchemas(ProtoTransportPayloadConfiguration protoTransportPayloadTypeConfiguration) {
                    try {
                        DynamicProtoUtils.validateProtoSchema(protoTransportPayloadTypeConfiguration.getDeviceAttributesProtoSchema(), ATTRIBUTES_PROTO_SCHEMA, EXCEPTION_PREFIX);
                        DynamicProtoUtils.validateProtoSchema(protoTransportPayloadTypeConfiguration.getDeviceTelemetryProtoSchema(), TELEMETRY_PROTO_SCHEMA, EXCEPTION_PREFIX);
                        DynamicProtoUtils.validateProtoSchema(protoTransportPayloadTypeConfiguration.getDeviceRpcRequestProtoSchema(), RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX);
                        DynamicProtoUtils.validateProtoSchema(protoTransportPayloadTypeConfiguration.getDeviceRpcResponseProtoSchema(), RPC_RESPONSE_PROTO_SCHEMA, EXCEPTION_PREFIX);
                    } catch (Exception exception) {
                        throw new DataValidationException(exception.getMessage());
                    }
                }

                private void validateLwm2mServersConfigOfBootstrapForClient(List<LwM2MBootstrapServerCredential> lwM2MBootstrapServersConfigurations, boolean isBootstrapServerUpdateEnable) {
                    Set<String> uris = new HashSet<>();
                    Set<Integer> shortServerIds = new HashSet<>();
                    for (LwM2MBootstrapServerCredential bootstrapServerCredential : lwM2MBootstrapServersConfigurations) {
                        AbstractLwM2MBootstrapServerCredential serverConfig = (AbstractLwM2MBootstrapServerCredential) bootstrapServerCredential;
                        if (!isBootstrapServerUpdateEnable && serverConfig.isBootstrapServerIs()) {
                            throw new DeviceCredentialsValidationException("Bootstrap config must not include \"Bootstrap Server\". \"Include Bootstrap Server updates\" is " + isBootstrapServerUpdateEnable + ".");
                        }
                        String server = serverConfig.isBootstrapServerIs() ? "Bootstrap Server" : "LwM2M Server" + " shortServerId: " + serverConfig.getShortServerId() + ":";
                        if (serverConfig.getShortServerId() < 1 || serverConfig.getShortServerId() > 65534) {
                            throw new DeviceCredentialsValidationException(server + " ShortServerId must not be less than 1 and more than 65534!");
                        }
                        if (!shortServerIds.add(serverConfig.getShortServerId())) {
                            throw new DeviceCredentialsValidationException(server + " \"Short server Id\" value = " + serverConfig.getShortServerId() + ". This value must be a unique value for all servers!");
                        }
                        String uri = serverConfig.getHost() + ":" + serverConfig.getPort();
                        if (!uris.add(uri)) {
                            throw new DeviceCredentialsValidationException(server + " \"Host + port\" value = " + uri + ". This value must be a unique value for all servers!");
                        }
                        Integer port;
                        if (LwM2MSecurityMode.NO_SEC.equals(serverConfig.getSecurityMode())) {
                            port = serverConfig.isBootstrapServerIs() ? 5687 : 5685;
                        } else {
                            port = serverConfig.isBootstrapServerIs() ? 5688 : 5686;
                        }
                        if (serverConfig.getPort() == null || serverConfig.getPort().intValue() != port) {
                            throw new DeviceCredentialsValidationException(server + " \"Port\" value = " + serverConfig.getPort() + ". This value for security " + serverConfig.getSecurityMode().name() + " must be " + port + "!");
                        }
                    }
                }

                private void validateLwm2mServersCredentialOfBootstrapForClient(LwM2MBootstrapServerCredential bootstrapServerConfig) {
                    String server;
                    switch (bootstrapServerConfig.getSecurityMode()) {
                        case NO_SEC:
                        case PSK:
                            break;
                        case RPK:
                            RPKLwM2MBootstrapServerCredential rpkServerCredentials = (RPKLwM2MBootstrapServerCredential) bootstrapServerConfig;
                            server = rpkServerCredentials.isBootstrapServerIs() ? "Bootstrap Server" : "LwM2M Server";
                            if (StringUtils.isEmpty(rpkServerCredentials.getServerPublicKey())) {
                                throw new DeviceCredentialsValidationException(server + " RPK public key must be specified!");
                            }
                            try {
                                String pubkRpkSever = EncryptionUtil.pubkTrimNewLines(rpkServerCredentials.getServerPublicKey());
                                rpkServerCredentials.setServerPublicKey(pubkRpkSever);
                                SecurityUtil.publicKey.decode(rpkServerCredentials.getDecodedCServerPublicKey());
                            } catch (Exception e) {
                                throw new DeviceCredentialsValidationException(server + " RPK public key must be in standard [RFC7250] and then encoded to Base64 format!");
                            }
                            break;
                        case X509:
                            X509LwM2MBootstrapServerCredential x509ServerCredentials = (X509LwM2MBootstrapServerCredential) bootstrapServerConfig;
                            server = x509ServerCredentials.isBootstrapServerIs() ? "Bootstrap Server" : "LwM2M Server";
                            if (StringUtils.isEmpty(x509ServerCredentials.getServerPublicKey())) {
                                throw new DeviceCredentialsValidationException(server + " X509 certificate must be specified!");
                            }
                            try {
                                String certServer = EncryptionUtil.certTrimNewLines(x509ServerCredentials.getServerPublicKey());
                                x509ServerCredentials.setServerPublicKey(certServer);
                                SecurityUtil.certificate.decode(x509ServerCredentials.getDecodedCServerPublicKey());
                            } catch (Exception e) {
                                throw new DeviceCredentialsValidationException(server + " X509 certificate must be in DER-encoded X509v3 format and support only EC algorithm and then encoded to Base64 format!");
                            }
                            break;
                    }
                }

                private void validateTelemetryDynamicMessageFields(ProtoTransportPayloadConfiguration protoTransportPayloadTypeConfiguration) {
                    String deviceTelemetryProtoSchema = protoTransportPayloadTypeConfiguration.getDeviceTelemetryProtoSchema();
                    Descriptors.Descriptor telemetryDynamicMessageDescriptor = protoTransportPayloadTypeConfiguration.getTelemetryDynamicMessageDescriptor(deviceTelemetryProtoSchema);
                    if (telemetryDynamicMessageDescriptor == null) {
                        throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(TELEMETRY_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Failed to get telemetryDynamicMessageDescriptor!");
                    } else {
                        List<Descriptors.FieldDescriptor> fields = telemetryDynamicMessageDescriptor.getFields();
                        if (CollectionUtils.isEmpty(fields)) {
                            throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(TELEMETRY_PROTO_SCHEMA, EXCEPTION_PREFIX) + " " + telemetryDynamicMessageDescriptor.getName() + " fields is empty!");
                        } else if (fields.size() == 2) {
                            Descriptors.FieldDescriptor tsFieldDescriptor = telemetryDynamicMessageDescriptor.findFieldByName("ts");
                            Descriptors.FieldDescriptor valuesFieldDescriptor = telemetryDynamicMessageDescriptor.findFieldByName("values");
                            if (tsFieldDescriptor != null && valuesFieldDescriptor != null) {
                                if (!Descriptors.FieldDescriptor.Type.MESSAGE.equals(valuesFieldDescriptor.getType())) {
                                    throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(TELEMETRY_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Field 'values' has invalid data type. Only message type is supported!");
                                }
                                if (!Descriptors.FieldDescriptor.Type.INT64.equals(tsFieldDescriptor.getType())) {
                                    throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(TELEMETRY_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Field 'ts' has invalid data type. Only int64 type is supported!");
                                }
                                if (!tsFieldDescriptor.hasOptionalKeyword()) {
                                    throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(TELEMETRY_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Field 'ts' has invalid label. Field 'ts' should have optional keyword!");
                                }
                            }
                        }
                    }
                }

                private void validateRpcRequestDynamicMessageFields(ProtoTransportPayloadConfiguration protoTransportPayloadTypeConfiguration) {
                    DynamicMessage.Builder rpcRequestDynamicMessageBuilder = protoTransportPayloadTypeConfiguration.getRpcRequestDynamicMessageBuilder(protoTransportPayloadTypeConfiguration.getDeviceRpcRequestProtoSchema());
                    Descriptors.Descriptor rpcRequestDynamicMessageDescriptor = rpcRequestDynamicMessageBuilder.getDescriptorForType();
                    if (rpcRequestDynamicMessageDescriptor == null) {
                        throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Failed to get rpcRequestDynamicMessageDescriptor!");
                    } else {
                        if (CollectionUtils.isEmpty(rpcRequestDynamicMessageDescriptor.getFields()) || rpcRequestDynamicMessageDescriptor.getFields().size() != 3) {
                            throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " " + rpcRequestDynamicMessageDescriptor.getName() + " message should always contains 3 fields: method, requestId and params!");
                        }
                        Descriptors.FieldDescriptor methodFieldDescriptor = rpcRequestDynamicMessageDescriptor.findFieldByName("method");
                        if (methodFieldDescriptor == null) {
                            throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Failed to get field descriptor for field: method!");
                        } else {
                            if (!Descriptors.FieldDescriptor.Type.STRING.equals(methodFieldDescriptor.getType())) {
                                throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Field 'method' has invalid data type. Only string type is supported!");
                            }
                            if (methodFieldDescriptor.isRepeated()) {
                                throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Field 'method' has invalid label!");
                            }
                        }
                        Descriptors.FieldDescriptor requestIdFieldDescriptor = rpcRequestDynamicMessageDescriptor.findFieldByName("requestId");
                        if (requestIdFieldDescriptor == null) {
                            throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Failed to get field descriptor for field: requestId!");
                        } else {
                            if (!Descriptors.FieldDescriptor.Type.INT32.equals(requestIdFieldDescriptor.getType())) {
                                throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Field 'requestId' has invalid data type. Only int32 type is supported!");
                            }
                            if (requestIdFieldDescriptor.isRepeated()) {
                                throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Field 'requestId' has invalid label!");
                            }
                        }
                        Descriptors.FieldDescriptor paramsFieldDescriptor = rpcRequestDynamicMessageDescriptor.findFieldByName("params");
                        if (paramsFieldDescriptor == null) {
                            throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Failed to get field descriptor for field: params!");
                        } else {
                            if (paramsFieldDescriptor.isRepeated()) {
                                throw new DataValidationException(DynamicProtoUtils.invalidSchemaProvidedMessage(RPC_REQUEST_PROTO_SCHEMA, EXCEPTION_PREFIX) + " Field 'params' has invalid label!");
                            }
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
                    removeDeviceProfile(tenantId, entity);
                }
            };

}
