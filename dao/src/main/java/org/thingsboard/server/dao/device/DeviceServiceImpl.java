/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.CollectionUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cache.device.DeviceCacheEvictEvent;
import org.thingsboard.server.cache.device.DeviceCacheKey;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceIdInfo;
import org.thingsboard.server.common.data.DeviceInfo;
import org.thingsboard.server.common.data.DeviceInfoFilter;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.device.data.CoapDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.data.Lwm2mDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.MqttDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.SnmpDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.CoapDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.CoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultCoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.EfentoCoapDeviceTypeConfiguration;
import org.thingsboard.server.common.data.device.profile.MqttDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.device.provision.ProvisionFailedException;
import org.thingsboard.server.dao.device.provision.ProvisionRequest;
import org.thingsboard.server.dao.device.provision.ProvisionResponseStatus;
import org.thingsboard.server.dao.entity.AbstractCachedEntityService;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Service("DeviceDaoService")
@Slf4j
public class DeviceServiceImpl extends AbstractCachedEntityService<DeviceCacheKey, Device, DeviceCacheEvictEvent> implements DeviceService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_DEVICE_PROFILE_ID = "Incorrect deviceProfileId ";
    public static final String INCORRECT_PAGE_LINK = "Incorrect page link ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_DEVICE_ID = "Incorrect deviceId ";
    public static final String INCORRECT_EDGE_ID = "Incorrect edgeId ";
    public static final String HTTP_PROTOCOL = "http";
    public static final String HTTPS_PROTOCOL = "https";
    public static final String MQTT_PROTOCOL = "mqtt";
    public static final String MQTTS_PROTOCOL = "mqtts";
    public static final String COAP_PROTOCOL = "coap";
    public static final String COAPS_PROTOCOL = "coaps";
    public static final String PAYLOAD = "\"{temperature:25}\"";
    public static final String NOT_SUPPORTED = "Not supported";
    public static final String DEFAULT_DEVICE_TELEMETRY_TOPIC = "v1/devices/me/telemetry";

    @Autowired
    private DeviceDao deviceDao;

    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    @Autowired
    private DeviceProfileService deviceProfileService;

    @Autowired
    private EventService eventService;

    @Autowired
    private DataValidator<Device> deviceValidator;

    @Autowired
    private EntityCountService countService;

    @Autowired
    private DeviceConnectivityConfiguration deviceConnectivityConfiguration;

    @Override
    public DeviceInfo findDeviceInfoById(TenantId tenantId, DeviceId deviceId) {
        log.trace("Executing findDeviceInfoById [{}]", deviceId);
        validateId(deviceId, INCORRECT_DEVICE_ID + deviceId);
        return deviceDao.findDeviceInfoById(tenantId, deviceId.getId());
    }

    @Override
    public Map<String, String> findDevicePublishTelemetryCommands(String baseUrl, Device device) throws URISyntaxException {
        DeviceId deviceId = device.getId();
        log.trace("Executing findDevicePublishTelemetryCommands [{}]", deviceId);
        validateId(deviceId, INCORRECT_DEVICE_ID + deviceId);

        String defaultHostname = new URI(baseUrl).getHost();
        DeviceCredentials creds = deviceCredentialsService.findDeviceCredentialsByDeviceId(device.getTenantId(), deviceId);
        DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileById(device.getTenantId(), device.getDeviceProfileId());
        DeviceTransportType transportType = deviceProfile.getTransportType();

        Map<String, String> commands = new HashMap<>();
        switch (transportType) {
            case DEFAULT:
                Optional.ofNullable(getHttpPublishCommand(defaultHostname, creds)).ifPresent(v -> commands.put(HTTP_PROTOCOL, v));
                Optional.ofNullable(getHttpsPublishCommand(defaultHostname, creds)).ifPresent(v -> commands.put(HTTPS_PROTOCOL, v));
                Optional.ofNullable(getMqttPublishCommand(defaultHostname, creds)).ifPresent(v -> commands.put(MQTT_PROTOCOL, v));
                Optional.ofNullable(getMqttsPublishCommand(defaultHostname, creds)).ifPresent(v -> commands.put(MQTTS_PROTOCOL, v));
                Optional.ofNullable(getCoapPublishCommand(defaultHostname, creds)).ifPresent(v -> commands.put(COAP_PROTOCOL, v));
                Optional.ofNullable(getCoapsPublishCommand(defaultHostname, creds)).ifPresent(v -> commands.put(COAPS_PROTOCOL, v));
                break;
            case MQTT:
                MqttDeviceProfileTransportConfiguration transportConfiguration =
                        (MqttDeviceProfileTransportConfiguration) deviceProfile.getProfileData().getTransportConfiguration();
                String topicName = transportConfiguration.getDeviceTelemetryTopic();
                TransportPayloadType payloadType = transportConfiguration.getTransportPayloadTypeConfiguration().getTransportPayloadType();
                String payload = (payloadType == TransportPayloadType.PROTOBUF) ? " -f protobufFileName" : " -m " + PAYLOAD;

                Optional.ofNullable(getMqttPublishCommand(defaultHostname, topicName, creds, payload)).ifPresent(v -> commands.put(MQTT_PROTOCOL, v));
                Optional.ofNullable(getMqttsPublishCommand(defaultHostname, topicName, creds, payload)).ifPresent(v -> commands.put(MQTTS_PROTOCOL, v));
                break;
            case COAP:
                    Optional.ofNullable(getCoapPublishCommand(defaultHostname, creds)).ifPresent(v -> commands.put(COAP_PROTOCOL, v));
                    Optional.ofNullable(getCoapsPublishCommand(defaultHostname, creds)).ifPresent(v -> commands.put(COAPS_PROTOCOL, v));
                break;
            default:
                commands.put(transportType.name(), NOT_SUPPORTED);
        }
        return commands;
    }

    @Override
    public Device findDeviceById(TenantId tenantId, DeviceId deviceId) {
        log.trace("Executing findDeviceById [{}]", deviceId);
        validateId(deviceId, INCORRECT_DEVICE_ID + deviceId);
        return cache.getAndPutInTransaction(new DeviceCacheKey(tenantId, deviceId),
                () -> {
                    //TODO: possible bug source since sometimes we need to clear cache by tenant id and sometimes by sys tenant id?
                    if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
                        return deviceDao.findById(tenantId, deviceId.getId());
                    } else {
                        return deviceDao.findDeviceByTenantIdAndId(tenantId, deviceId.getId());
                    }
                }, true);
    }

    @Override
    public ListenableFuture<Device> findDeviceByIdAsync(TenantId tenantId, DeviceId deviceId) {
        log.trace("Executing findDeviceById [{}]", deviceId);
        validateId(deviceId, INCORRECT_DEVICE_ID + deviceId);
        if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
            return deviceDao.findByIdAsync(tenantId, deviceId.getId());
        } else {
            return deviceDao.findDeviceByTenantIdAndIdAsync(tenantId, deviceId.getId());
        }
    }

    @Override
    public Device findDeviceByTenantIdAndName(TenantId tenantId, String name) {
        log.trace("Executing findDeviceByTenantIdAndName [{}][{}]", tenantId, name);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        return cache.getAndPutInTransaction(new DeviceCacheKey(tenantId, name),
                () -> deviceDao.findDeviceByTenantIdAndName(tenantId.getId(), name).orElse(null), true);
    }

    @Override
    public Device saveDeviceWithAccessToken(Device device, String accessToken) {
        return doSaveDevice(device, accessToken, true);
    }

    @Override
    public Device saveDevice(Device device, boolean doValidate) {
        return doSaveDevice(device, null, doValidate);
    }

    @Override
    public Device saveDevice(Device device) {
        return doSaveDevice(device, null, true);
    }

    @Transactional
    @Override
    public Device saveDeviceWithCredentials(Device device, DeviceCredentials deviceCredentials) {
        if (device.getId() == null) {
            Device deviceWithName = this.findDeviceByTenantIdAndName(device.getTenantId(), device.getName());
            device = deviceWithName == null ? device : deviceWithName.updateDevice(device);
        }
        Device savedDevice = this.saveDeviceWithoutCredentials(device, true);
        deviceCredentials.setDeviceId(savedDevice.getId());
        if (device.getId() == null) {
            deviceCredentialsService.createDeviceCredentials(savedDevice.getTenantId(), deviceCredentials);
        } else {
            DeviceCredentials foundDeviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(device.getTenantId(), savedDevice.getId());
            if (foundDeviceCredentials == null) {
                deviceCredentialsService.createDeviceCredentials(savedDevice.getTenantId(), deviceCredentials);
            } else {
                deviceCredentials.setId(foundDeviceCredentials.getId());
                deviceCredentialsService.updateDeviceCredentials(device.getTenantId(), deviceCredentials);
            }
        }
        return savedDevice;
    }

    private Device doSaveDevice(Device device, String accessToken, boolean doValidate) {
        Device savedDevice = this.saveDeviceWithoutCredentials(device, doValidate);
        if (device.getId() == null) {
            DeviceCredentials deviceCredentials = new DeviceCredentials();
            deviceCredentials.setDeviceId(new DeviceId(savedDevice.getUuidId()));
            deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
            deviceCredentials.setCredentialsId(!StringUtils.isEmpty(accessToken) ? accessToken : StringUtils.randomAlphanumeric(20));
            deviceCredentialsService.createDeviceCredentials(savedDevice.getTenantId(), deviceCredentials);
        }
        return savedDevice;
    }

    private Device saveDeviceWithoutCredentials(Device device, boolean doValidate) {
        log.trace("Executing saveDevice [{}]", device);
        Device oldDevice = null;
        if (doValidate) {
            oldDevice = deviceValidator.validate(device, Device::getTenantId);
        } else if (device.getId() != null) {
            oldDevice = findDeviceById(device.getTenantId(), device.getId());
        }
        DeviceCacheEvictEvent deviceCacheEvictEvent = new DeviceCacheEvictEvent(device.getTenantId(), device.getId(), device.getName(), oldDevice != null ? oldDevice.getName() : null);
        try {
            DeviceProfile deviceProfile;
            if (device.getDeviceProfileId() == null) {
                if (!StringUtils.isEmpty(device.getType())) {
                    deviceProfile = this.deviceProfileService.findOrCreateDeviceProfile(device.getTenantId(), device.getType());
                } else {
                    deviceProfile = this.deviceProfileService.findDefaultDeviceProfile(device.getTenantId());
                }
                device.setDeviceProfileId(new DeviceProfileId(deviceProfile.getId().getId()));
            } else {
                deviceProfile = this.deviceProfileService.findDeviceProfileById(device.getTenantId(), device.getDeviceProfileId());
                if (deviceProfile == null) {
                    throw new DataValidationException("Device is referencing non existing device profile!");
                }
                if (!deviceProfile.getTenantId().equals(device.getTenantId())) {
                    throw new DataValidationException("Device can`t be referencing to device profile from different tenant!");
                }
            }
            device.setType(deviceProfile.getName());
            device.setDeviceData(syncDeviceData(deviceProfile, device.getDeviceData()));
            Device result = deviceDao.saveAndFlush(device.getTenantId(), device);
            publishEvictEvent(deviceCacheEvictEvent);
            if (device.getId() == null) {
                countService.publishCountEntityEvictEvent(result.getTenantId(), EntityType.DEVICE);
            }
            return result;
        } catch (Exception t) {
            handleEvictEvent(deviceCacheEvictEvent);
            checkConstraintViolation(t,
                    "device_name_unq_key", "Device with such name already exists!",
                    "device_external_id_unq_key", "Device with such external id already exists!");
            throw t;
        }
    }

    @TransactionalEventListener(classes = DeviceCacheEvictEvent.class)
    @Override
    public void handleEvictEvent(DeviceCacheEvictEvent event) {
        List<DeviceCacheKey> keys = new ArrayList<>(3);
        keys.add(new DeviceCacheKey(event.getTenantId(), event.getNewName()));
        if (event.getDeviceId() != null) {
            keys.add(new DeviceCacheKey(event.getTenantId(), event.getDeviceId()));
        }
        if (StringUtils.isNotEmpty(event.getOldName()) && !event.getOldName().equals(event.getNewName())) {
            keys.add(new DeviceCacheKey(event.getTenantId(), event.getOldName()));
        }
        cache.evict(keys);
    }

    private DeviceData syncDeviceData(DeviceProfile deviceProfile, DeviceData deviceData) {
        if (deviceData == null) {
            deviceData = new DeviceData();
        }
        if (deviceData.getConfiguration() == null || !deviceProfile.getType().equals(deviceData.getConfiguration().getType())) {
            if (deviceProfile.getType() == DeviceProfileType.DEFAULT) {
                deviceData.setConfiguration(new DefaultDeviceConfiguration());
            }
        }
        if (deviceData.getTransportConfiguration() == null || !deviceProfile.getTransportType().equals(deviceData.getTransportConfiguration().getType())) {
            switch (deviceProfile.getTransportType()) {
                case DEFAULT:
                    deviceData.setTransportConfiguration(new DefaultDeviceTransportConfiguration());
                    break;
                case MQTT:
                    deviceData.setTransportConfiguration(new MqttDeviceTransportConfiguration());
                    break;
                case COAP:
                    deviceData.setTransportConfiguration(new CoapDeviceTransportConfiguration());
                    break;
                case LWM2M:
                    deviceData.setTransportConfiguration(new Lwm2mDeviceTransportConfiguration());
                    break;
                case SNMP:
                    deviceData.setTransportConfiguration(new SnmpDeviceTransportConfiguration());
                    break;
            }
        }
        return deviceData;
    }

    @Transactional
    @Override
    public Device assignDeviceToCustomer(TenantId tenantId, DeviceId deviceId, CustomerId customerId) {
        Device device = findDeviceById(tenantId, deviceId);
        device.setCustomerId(customerId);
        return saveDevice(device);
    }

    @Transactional
    @Override
    public Device unassignDeviceFromCustomer(TenantId tenantId, DeviceId deviceId) {
        Device device = findDeviceById(tenantId, deviceId);
        device.setCustomerId(null);
        return saveDevice(device);
    }

    @Transactional
    @Override
    public void deleteDevice(final TenantId tenantId, final DeviceId deviceId) {
        log.trace("Executing deleteDevice [{}]", deviceId);
        validateId(deviceId, INCORRECT_DEVICE_ID + deviceId);

        Device device = deviceDao.findById(tenantId, deviceId.getId());
        DeviceCacheEvictEvent deviceCacheEvictEvent = new DeviceCacheEvictEvent(device.getTenantId(), device.getId(), device.getName(), null);
        List<EntityView> entityViews = entityViewService.findEntityViewsByTenantIdAndEntityId(device.getTenantId(), deviceId);
        if (entityViews != null && !entityViews.isEmpty()) {
            throw new DataValidationException("Can't delete device that has entity views!");
        }

        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(tenantId, deviceId);
        if (deviceCredentials != null) {
            deviceCredentialsService.deleteDeviceCredentials(tenantId, deviceCredentials);
        }
        deleteEntityRelations(tenantId, deviceId);

        deviceDao.removeById(tenantId, deviceId.getId());

        publishEvictEvent(deviceCacheEvictEvent);
        countService.publishCountEntityEvictEvent(tenantId, EntityType.DEVICE);
    }

    @Override
    public PageData<Device> findDevicesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findDevicesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink);
        return deviceDao.findDevicesByTenantId(tenantId.getId(), pageLink);
    }


    @Override
    public PageData<DeviceInfo> findDeviceInfosByFilter(DeviceInfoFilter filter, PageLink pageLink) {
        log.trace("Executing findDeviceInfosByFilter, filter [{}], pageLink [{}]", filter, pageLink);
        if (filter == null) {
            throw new IncorrectParameterException("Filter is empty!");
        }
        validateId(filter.getTenantId(), INCORRECT_TENANT_ID + filter.getTenantId());
        validatePageLink(pageLink);
        return deviceDao.findDeviceInfosByFilter(filter, pageLink);

    }

    @Override
    public PageData<DeviceIdInfo> findDeviceIdInfos(PageLink pageLink) {
        log.trace("Executing findTenantDeviceIdPairs, pageLink [{}]", pageLink);
        validatePageLink(pageLink);
        return deviceDao.findDeviceIdInfos(pageLink);
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return deviceDao.findDevicesByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndTypeAndEmptyOtaPackage(TenantId tenantId,
                                                                           DeviceProfileId deviceProfileId,
                                                                           OtaPackageType type,
                                                                           PageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndTypeAndEmptyOtaPackage, tenantId [{}], deviceProfileId [{}], type [{}], pageLink [{}]",
                tenantId, deviceProfileId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(tenantId, INCORRECT_DEVICE_PROFILE_ID + deviceProfileId);
        validatePageLink(pageLink);
        return deviceDao.findDevicesByTenantIdAndTypeAndEmptyOtaPackage(tenantId.getId(), deviceProfileId.getId(), type, pageLink);
    }

    @Override
    public Long countDevicesByTenantIdAndDeviceProfileIdAndEmptyOtaPackage(TenantId tenantId, DeviceProfileId deviceProfileId, OtaPackageType type) {
        log.trace("Executing countDevicesByTenantIdAndDeviceProfileIdAndEmptyOtaPackage, tenantId [{}], deviceProfileId [{}], type [{}]", tenantId, deviceProfileId, type);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(tenantId, INCORRECT_DEVICE_PROFILE_ID + deviceProfileId);
        return deviceDao.countDevicesByTenantIdAndDeviceProfileIdAndEmptyOtaPackage(tenantId.getId(), deviceProfileId.getId(), type);
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdAndIdsAsync(TenantId tenantId, List<DeviceId> deviceIds) {
        log.trace("Executing findDevicesByTenantIdAndIdsAsync, tenantId [{}], deviceIds [{}]", tenantId, deviceIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateIds(deviceIds, "Incorrect deviceIds " + deviceIds);
        return deviceDao.findDevicesByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(deviceIds));
    }

    @Override
    public List<Device> findDevicesByIds(List<DeviceId> deviceIds) {
        log.trace("Executing findDevicesByIdsAsync, deviceIds [{}]", deviceIds);
        validateIds(deviceIds, "Incorrect deviceIds " + deviceIds);
        return deviceDao.findDevicesByIds(toUUIDs(deviceIds));
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByIdsAsync(List<DeviceId> deviceIds) {
        log.trace("Executing findDevicesByIdsAsync, deviceIds [{}]", deviceIds);
        validateIds(deviceIds, "Incorrect deviceIds " + deviceIds);
        return deviceDao.findDevicesByIdsAsync(toUUIDs(deviceIds));
    }

    @Transactional
    @Override
    public void deleteDevicesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteDevicesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantDevicesRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink);
        return deviceDao.findDevicesByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return deviceDao.findDevicesByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdCustomerIdAndIdsAsync(TenantId tenantId, CustomerId customerId, List<DeviceId> deviceIds) {
        log.trace("Executing findDevicesByTenantIdCustomerIdAndIdsAsync, tenantId [{}], customerId [{}], deviceIds [{}]", tenantId, customerId, deviceIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateIds(deviceIds, "Incorrect deviceIds " + deviceIds);
        return deviceDao.findDevicesByTenantIdCustomerIdAndIdsAsync(tenantId.getId(),
                customerId.getId(), toUUIDs(deviceIds));
    }

    @Override
    public void unassignCustomerDevices(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing unassignCustomerDevices, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        customerDeviceUnasigner.removeEntities(tenantId, customerId);
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByQuery(TenantId tenantId, DeviceSearchQuery query) {
        ListenableFuture<List<EntityRelation>> relations = relationService.findByQuery(tenantId, query.toEntitySearchQuery());
        return Futures.transform(relations, r -> {
            EntitySearchDirection direction = query.toEntitySearchQuery().getParameters().getDirection();
            List<Device> devices = new ArrayList<>();
            for (EntityRelation relation : r) {
                EntityId entityId = direction == EntitySearchDirection.FROM ? relation.getTo() : relation.getFrom();
                if (entityId.getEntityType() == EntityType.DEVICE) {
                    Device device = findDeviceById(tenantId, new DeviceId(entityId.getId()));
                    if (query.getDeviceTypes().contains(device.getType())) {
                        devices.add(device);
                    }
                }
            }
            return devices;
        }, MoreExecutors.directExecutor());
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findDeviceTypesByTenantId(TenantId tenantId) {
        log.trace("Executing findDeviceTypesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        ListenableFuture<List<EntitySubtype>> tenantDeviceTypes = deviceDao.findTenantDeviceTypesAsync(tenantId.getId());
        return Futures.transform(tenantDeviceTypes,
                deviceTypes -> {
                    deviceTypes.sort(Comparator.comparing(EntitySubtype::getType));
                    return deviceTypes;
                }, MoreExecutors.directExecutor());
    }

    @Transactional
    @Override
    public Device assignDeviceToTenant(TenantId tenantId, Device device) {
        log.trace("Executing assignDeviceToTenant [{}][{}]", tenantId, device);
        List<EntityView> entityViews = entityViewService.findEntityViewsByTenantIdAndEntityId(device.getTenantId(), device.getId());
        if (!CollectionUtils.isEmpty(entityViews)) {
            throw new DataValidationException("Can't assign device that has entity views to another tenant!");
        }

        eventService.removeEvents(device.getTenantId(), device.getId());

        relationService.removeRelations(device.getTenantId(), device.getId());

        TenantId oldTenantId = device.getTenantId();

        device.setTenantId(tenantId);
        device.setCustomerId(null);
        device.setDeviceProfileId(null);
        Device savedDevice = doSaveDevice(device, null, true);

        DeviceCacheEvictEvent oldTenantEvent = new DeviceCacheEvictEvent(oldTenantId, device.getId(), device.getName(), null);
        DeviceCacheEvictEvent newTenantEvent = new DeviceCacheEvictEvent(savedDevice.getTenantId(), device.getId(), device.getName(), null);

        // explicitly remove device with previous tenant id from cache
        // result device object will have different tenant id and will not remove entity from cache
        publishEvictEvent(oldTenantEvent);
        publishEvictEvent(newTenantEvent);

        return savedDevice;
    }

    @Override
    @Transactional
    public Device saveDevice(ProvisionRequest provisionRequest, DeviceProfile profile) {
        Device device = new Device();
        device.setName(provisionRequest.getDeviceName());
        device.setType(profile.getName());
        device.setTenantId(profile.getTenantId());
        Device savedDevice = saveDevice(device);
        if (!StringUtils.isEmpty(provisionRequest.getCredentialsData().getToken()) ||
                !StringUtils.isEmpty(provisionRequest.getCredentialsData().getX509CertHash()) ||
                !StringUtils.isEmpty(provisionRequest.getCredentialsData().getUsername()) ||
                !StringUtils.isEmpty(provisionRequest.getCredentialsData().getPassword()) ||
                !StringUtils.isEmpty(provisionRequest.getCredentialsData().getClientId())) {
            DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(savedDevice.getTenantId(), savedDevice.getId());
            if (deviceCredentials == null) {
                deviceCredentials = new DeviceCredentials();
            }
            deviceCredentials.setDeviceId(savedDevice.getId());
            deviceCredentials.setCredentialsType(provisionRequest.getCredentialsType());
            switch (provisionRequest.getCredentialsType()) {
                case ACCESS_TOKEN:
                    deviceCredentials.setCredentialsId(provisionRequest.getCredentialsData().getToken());
                    break;
                case MQTT_BASIC:
                    BasicMqttCredentials mqttCredentials = new BasicMqttCredentials();
                    mqttCredentials.setClientId(provisionRequest.getCredentialsData().getClientId());
                    mqttCredentials.setUserName(provisionRequest.getCredentialsData().getUsername());
                    mqttCredentials.setPassword(provisionRequest.getCredentialsData().getPassword());
                    deviceCredentials.setCredentialsValue(JacksonUtil.toString(mqttCredentials));
                    break;
                case X509_CERTIFICATE:
                    deviceCredentials.setCredentialsValue(provisionRequest.getCredentialsData().getX509CertHash());
                    break;
                case LWM2M_CREDENTIALS:
                    break;
            }
            try {
                deviceCredentialsService.updateDeviceCredentials(savedDevice.getTenantId(), deviceCredentials);
            } catch (Exception e) {
                throw new ProvisionFailedException(ProvisionResponseStatus.FAILURE.name());
            }
        }

        publishEvictEvent(new DeviceCacheEvictEvent(savedDevice.getTenantId(), savedDevice.getId(), provisionRequest.getDeviceName(), null));
        countService.publishCountEntityEvictEvent(savedDevice.getTenantId(), EntityType.DEVICE);
        return savedDevice;
    }

    @Override
    public PageData<UUID> findDevicesIdsByDeviceProfileTransportType(DeviceTransportType transportType, PageLink pageLink) {
        return deviceDao.findDevicesIdsByDeviceProfileTransportType(transportType, pageLink);
    }

    @Override
    public Device assignDeviceToEdge(TenantId tenantId, DeviceId deviceId, EdgeId edgeId) {
        Device device = findDeviceById(tenantId, deviceId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't assign device to non-existent edge!");
        }
        if (!edge.getTenantId().getId().equals(device.getTenantId().getId())) {
            throw new DataValidationException("Can't assign device to edge from different tenant!");
        }
        try {
            createRelation(tenantId, new EntityRelation(edgeId, deviceId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to create device relation. Edge Id: [{}]", deviceId, edgeId);
            throw new RuntimeException(e);
        }
        return device;
    }

    @Override
    public Device unassignDeviceFromEdge(TenantId tenantId, DeviceId deviceId, EdgeId edgeId) {
        Device device = findDeviceById(tenantId, deviceId);
        Edge edge = edgeService.findEdgeById(tenantId, edgeId);
        if (edge == null) {
            throw new DataValidationException("Can't unassign device from non-existent edge!");
        }

        checkAssignedEntityViewsToEdge(tenantId, deviceId, edgeId);

        try {
            deleteRelation(tenantId, new EntityRelation(edgeId, deviceId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE));
        } catch (Exception e) {
            log.warn("[{}] Failed to delete device relation. Edge Id: [{}]", deviceId, edgeId);
            throw new RuntimeException(e);
        }
        return device;
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndEdgeId, tenantId [{}], edgeId [{}], pageLink [{}]", tenantId, edgeId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        validatePageLink(pageLink);
        return deviceDao.findDevicesByTenantIdAndEdgeId(tenantId.getId(), edgeId.getId(), pageLink);
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndEdgeIdAndType(TenantId tenantId, EdgeId edgeId, String type, PageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndEdgeIdAndType, tenantId [{}], edgeId [{}], type [{}] pageLink [{}]", tenantId, edgeId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(edgeId, INCORRECT_EDGE_ID + edgeId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink);
        return deviceDao.findDevicesByTenantIdAndEdgeIdAndType(tenantId.getId(), edgeId.getId(), type, pageLink);
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return deviceDao.countByTenantId(tenantId);
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id) {
        deleteDevice(tenantId, (DeviceId) id);
    }

    private PaginatedRemover<TenantId, Device> tenantDevicesRemover =
            new PaginatedRemover<>() {

                @Override
                protected PageData<Device> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
                    return deviceDao.findDevicesByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(TenantId tenantId, Device entity) {
                    deleteDevice(tenantId, new DeviceId(entity.getUuidId()));
                }
            };

    private PaginatedRemover<CustomerId, Device> customerDeviceUnasigner = new PaginatedRemover<CustomerId, Device>() {

        @Override
        protected PageData<Device> findEntities(TenantId tenantId, CustomerId id, PageLink pageLink) {
            return deviceDao.findDevicesByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, Device entity) {
            unassignDeviceFromCustomer(tenantId, new DeviceId(entity.getUuidId()));
        }
    };

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findDeviceById(tenantId, new DeviceId(entityId.getId())));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DEVICE;
    }

    private String getHttpPublishCommand(String defaultHostname, DeviceCredentials deviceCredentials) {
        DeviceConnectivityInfo httpProps = deviceConnectivityConfiguration.getConnectivity().get("http");
        if (httpProps != null && httpProps.getEnabled() &&
                deviceCredentials.getCredentialsType() == DeviceCredentialsType.ACCESS_TOKEN) {
            String hostName = httpProps.getHost().isEmpty() ? defaultHostname : httpProps.getHost();
            String port = httpProps.getPort().isEmpty() ? "" : ":" + httpProps.getPort();
            return String.format("curl -v -X POST http://%s%s/api/v1/%s/telemetry --header Content-Type:application/json --data " + PAYLOAD,
                    hostName, port, deviceCredentials.getCredentialsId());
        }
        return null;
    }

    private String getHttpsPublishCommand(String defaultHostname, DeviceCredentials deviceCredentials) {
        DeviceConnectivityInfo httpsProps = deviceConnectivityConfiguration.getConnectivity().get("https");
        if (httpsProps != null && httpsProps.getEnabled() &&
                deviceCredentials.getCredentialsType() == DeviceCredentialsType.ACCESS_TOKEN) {
            String hostName = httpsProps.getHost().isEmpty() ? defaultHostname : httpsProps.getHost();
            String port = httpsProps.getPort().isEmpty() ? "" : ":" + httpsProps.getPort();
            return String.format("curl -v -X POST https://%s%s/api/v1/%s/telemetry --header Content-Type:application/json --data " + PAYLOAD,
                    hostName, port, deviceCredentials.getCredentialsId());
        }
        return null;
    }

    private String getMqttPublishCommand(String defaultHostname, DeviceCredentials deviceCredentials) {
        return getMqttPublishCommand(defaultHostname,"v1/devices/me/telemetry", deviceCredentials, " -m " + PAYLOAD);
    }

    private String getMqttPublishCommand(String defaultHostname, String deviceTelemetryTopic, DeviceCredentials deviceCredentials, String payload) {
        DeviceConnectivityInfo mqttProps = deviceConnectivityConfiguration.getConnectivity().get("mqtt");
        if (mqttProps == null || !mqttProps.getEnabled()) {
            return null;
        }

        StringBuilder command = new StringBuilder("mosquitto_pub -d -q 1");
        command.append(" -h ").append(mqttProps.getHost().isEmpty() ? defaultHostname : mqttProps.getHost());
        if (!mqttProps.getPort().isEmpty()) {
            command.append(" -p ").append(mqttProps.getPort());
        }
        command.append(" -t ").append(deviceTelemetryTopic);

        switch (deviceCredentials.getCredentialsType()) {
            case ACCESS_TOKEN:
                command.append(" -u ").append(deviceCredentials.getCredentialsId());
                break;
            case MQTT_BASIC:
                BasicMqttCredentials credentials = JacksonUtil.fromString(deviceCredentials.getCredentialsValue(),
                        BasicMqttCredentials.class);
                if (credentials != null) {
                    if (credentials.getClientId() != null) {
                        command.append(" -i ").append(credentials.getClientId());
                    }
                    if (credentials.getUserName() != null) {
                        command.append(" -u ").append(credentials.getUserName());
                    }
                    if (credentials.getPassword() != null) {
                        command.append(" -P ").append(credentials.getPassword());
                    }
                } else {
                    return null;
                }
                break;
            default:
                return null;
        }
        command.append(payload);
        return command.toString();
    }

    private String getMqttsPublishCommand(String defaultHostname, DeviceCredentials deviceCredentials) {
        return getMqttsPublishCommand(defaultHostname, DEFAULT_DEVICE_TELEMETRY_TOPIC, deviceCredentials, " -m " + PAYLOAD);
    }

    private String getMqttsPublishCommand(String defaultHostname, String deviceTelemetryTopic, DeviceCredentials deviceCredentials, String payload) {
        DeviceConnectivityInfo mqttsProps = deviceConnectivityConfiguration.getConnectivity().get("mqtts");
        if (mqttsProps == null || !mqttsProps.getEnabled()) {
            return null;
        }

        StringBuilder command = new StringBuilder("mosquitto_pub --cafile tb-server-chain.pem -d -q 1");
        command.append(" -h ").append(mqttsProps.getHost().isEmpty() ? defaultHostname : mqttsProps.getHost());
        if (!mqttsProps.getPort().isEmpty()) {
            command.append(" -p ").append(mqttsProps.getPort());
        }
        command.append(" -t ").append(deviceTelemetryTopic);

        switch (deviceCredentials.getCredentialsType()) {
            case ACCESS_TOKEN:
                command.append(" -u ").append(deviceCredentials.getCredentialsId());
                break;
            case MQTT_BASIC:
                BasicMqttCredentials credentials = JacksonUtil.fromString(deviceCredentials.getCredentialsValue(),
                        BasicMqttCredentials.class);
                if (credentials != null) {
                    if (credentials.getClientId() != null) {
                        command.append(" -i ").append(credentials.getClientId());
                    }
                    if (credentials.getUserName() != null) {
                        command.append(" -u ").append(credentials.getUserName());
                    }
                    if (credentials.getPassword() != null) {
                        command.append(" -P ").append(credentials.getPassword());
                    }
                } else {
                    return null;
                }
                break;
            case X509_CERTIFICATE:
                return NOT_SUPPORTED;
            default:
                return null;
        }
        command.append(payload);
        return command.toString();
    }

    private String getCoapPublishCommand(String defaultHostname, DeviceCredentials deviceCredentials) {
        DeviceConnectivityInfo coapProperties = deviceConnectivityConfiguration.getConnectivity().get("coap");
        if (coapProperties == null || !coapProperties.getEnabled()) {
            return null;
        }
        if (deviceCredentials.getCredentialsType() == DeviceCredentialsType.ACCESS_TOKEN) {
            String hostName = coapProperties.getHost().isEmpty() ? defaultHostname : coapProperties.getHost();
            String port = coapProperties.getPort().isEmpty() ? "" : ":" + coapProperties.getPort();

            return String.format("coap-client -m POST coap://%s%s/api/v1/%s/telemetry -t json -e %s",
                    hostName, port, deviceCredentials.getCredentialsId(), PAYLOAD);
        }
        return null;
    }

    private String getCoapsPublishCommand(String defaultHostname, DeviceCredentials deviceCredentials) {
        DeviceConnectivityInfo coapsProperties = deviceConnectivityConfiguration.getConnectivity().get("coaps");
        if (coapsProperties == null || !coapsProperties.getEnabled()) {
            return null;
        }
        switch (deviceCredentials.getCredentialsType()) {
            case ACCESS_TOKEN:
                String hostName = coapsProperties.getHost().isEmpty() ? defaultHostname : coapsProperties.getHost();
                String port = coapsProperties.getPort().isEmpty() ? "" : ":" + coapsProperties.getPort();
                return String.format("coap-client-openssl -v 9 -m POST coaps://%s%s/api/v1/%s/telemetry -t json -e %s",
                        hostName, port, deviceCredentials.getCredentialsId(), PAYLOAD);
            case X509_CERTIFICATE:
                return NOT_SUPPORTED;
            default:
                return null;
        }
    }
}
