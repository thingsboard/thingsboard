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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.CollectionUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cache.device.DeviceCacheEvictEvent;
import org.thingsboard.server.cache.device.DeviceCacheKey;
import org.thingsboard.server.common.data.DataConstants;
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
import org.thingsboard.server.common.data.ProfileEntityIdInfo;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.device.data.CoapDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceConfiguration;
import org.thingsboard.server.common.data.device.data.DefaultDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.DeviceData;
import org.thingsboard.server.common.data.device.data.Lwm2mDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.MqttDeviceTransportConfiguration;
import org.thingsboard.server.common.data.device.data.SnmpDeviceTransportConfiguration;
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
import org.thingsboard.server.dao.entity.CachedVersionedEntityService;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.service.validator.DeviceDataValidator;
import org.thingsboard.server.dao.sql.JpaExecutorService;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Service("DeviceDaoService")
@Slf4j
@RequiredArgsConstructor
public class DeviceServiceImpl extends CachedVersionedEntityService<DeviceCacheKey, Device, DeviceCacheEvictEvent> implements DeviceService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_DEVICE_PROFILE_ID = "Incorrect deviceProfileId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_DEVICE_ID = "Incorrect deviceId ";
    public static final String INCORRECT_EDGE_ID = "Incorrect edgeId ";

    private final DeviceDao deviceDao;
    private final DeviceCredentialsService deviceCredentialsService;
    private final DeviceProfileService deviceProfileService;
    private final EventService eventService;
    private final TenantService tenantService;
    private final DeviceDataValidator deviceValidator;
    private final EntityCountService countService;
    private final JpaExecutorService executor;

    @Override
    public DeviceInfo findDeviceInfoById(TenantId tenantId, DeviceId deviceId) {
        log.trace("Executing findDeviceInfoById [{}]", deviceId);
        validateId(deviceId, id -> INCORRECT_DEVICE_ID + id);
        return deviceDao.findDeviceInfoById(tenantId, deviceId.getId());
    }

    @Override
    public Device findDeviceById(TenantId tenantId, DeviceId deviceId) {
        log.trace("Executing findDeviceById [{}]", deviceId);
        validateId(deviceId, id -> INCORRECT_DEVICE_ID + id);
        if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
            return cache.get(new DeviceCacheKey(deviceId),
                    () -> deviceDao.findById(tenantId, deviceId.getId()));
        } else {
            return cache.get(new DeviceCacheKey(tenantId, deviceId),
                    () -> deviceDao.findDeviceByTenantIdAndId(tenantId, deviceId.getId()));
        }
    }

    @Override
    public ListenableFuture<Device> findDeviceByIdAsync(TenantId tenantId, DeviceId deviceId) {
        log.trace("Executing findDeviceByIdAsync [{}]", deviceId);
        validateId(deviceId, id -> INCORRECT_DEVICE_ID + id);
        if (TenantId.SYS_TENANT_ID.equals(tenantId)) {
            return deviceDao.findByIdAsync(tenantId, deviceId.getId());
        } else {
            return deviceDao.findDeviceByTenantIdAndIdAsync(tenantId, deviceId.getId());
        }
    }

    @Override
    public Device findDeviceByTenantIdAndName(TenantId tenantId, String name) {
        log.trace("Executing findDeviceByTenantIdAndName [{}][{}]", tenantId, name);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return cache.getAndPutInTransaction(new DeviceCacheKey(tenantId, name),
                () -> deviceDao.findDeviceByTenantIdAndName(tenantId.getId(), name).orElse(null), true);
    }

    @Override
    public ListenableFuture<Device> findDeviceByTenantIdAndNameAsync(TenantId tenantId, String name) {
        log.trace("Executing findDeviceByTenantIdAndNameAsync [{}][{}]", tenantId, name);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return executor.submit(() -> findDeviceByTenantIdAndName(tenantId, name));
    }

    @Transactional
    @Override
    public Device saveDeviceWithAccessToken(Device device, String accessToken) {
        return doSaveDevice(device, accessToken, true);
    }

    @Override
    public Device saveDevice(Device device, boolean doValidate) {
        return doSaveDevice(device, null, doValidate);
    }

    @Transactional
    @Override
    public Device saveDevice(Device device) {
        return doSaveDevice(device, null, true);
    }

    @Transactional
    @Override
    public Device saveDeviceWithCredentials(Device device, DeviceCredentials deviceCredentials) {
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
        return saveEntity(device, () -> doSaveDeviceWithoutCredentials(device, doValidate));
    }

    private Device doSaveDeviceWithoutCredentials(Device device, boolean doValidate) {
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
                deviceProfile = this.deviceProfileService.findDeviceProfileById(device.getTenantId(), device.getDeviceProfileId(), false);
                if (deviceProfile == null) {
                    throw new DataValidationException("Device is referencing non existing device profile!");
                }
                if (!deviceProfile.getTenantId().equals(device.getTenantId())) {
                    throw new DataValidationException("Device can`t be referencing to device profile from different tenant!");
                }
            }
            device.setType(deviceProfile.getName());
            device.setDeviceData(syncDeviceData(deviceProfile, device.getDeviceData()));
            Device savedDevice = deviceDao.saveAndFlush(device.getTenantId(), device);
            deviceCacheEvictEvent.setSavedDevice(savedDevice);
            publishEvictEvent(deviceCacheEvictEvent);
            if (device.getId() == null) {
                countService.publishCountEntityEvictEvent(savedDevice.getTenantId(), EntityType.DEVICE);
            }
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(savedDevice.getTenantId()).entityId(savedDevice.getId())
                    .entity(savedDevice).oldEntity(oldDevice).created(device.getId() == null).build());
            return savedDevice;
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
        List<DeviceCacheKey> toEvict = new ArrayList<>(3);
        toEvict.add(new DeviceCacheKey(event.getTenantId(), event.getNewName()));
        if (StringUtils.isNotEmpty(event.getOldName()) && !event.getOldName().equals(event.getNewName())) {
            toEvict.add(new DeviceCacheKey(event.getTenantId(), event.getOldName()));
        }
        Device savedDevice = event.getSavedDevice();
        if (savedDevice != null) {
            cache.put(new DeviceCacheKey(event.getDeviceId()), savedDevice);
            cache.put(new DeviceCacheKey(event.getTenantId(), event.getDeviceId()), savedDevice);
        } else {
            toEvict.add(new DeviceCacheKey(event.getDeviceId()));
            toEvict.add(new DeviceCacheKey(event.getTenantId(), event.getDeviceId()));
        }
        cache.evict(toEvict);
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
        if (customerId.equals(device.getCustomerId())) {
            return device;
        }
        device.setCustomerId(customerId);
        return saveDevice(device);
    }

    @Transactional
    @Override
    public Device unassignDeviceFromCustomer(TenantId tenantId, DeviceId deviceId) {
        Device device = findDeviceById(tenantId, deviceId);
        if (device.getCustomerId() == null) {
            return device;
        }
        device.setCustomerId(null);
        return saveDevice(device);
    }

    @Transactional
    @Override
    public void deleteDevice(final TenantId tenantId, final DeviceId deviceId) {
        validateId(deviceId, id -> INCORRECT_DEVICE_ID + id);
        deleteEntity(tenantId, deviceId, false);
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        if (!force && (entityViewService.existsByTenantIdAndEntityId(tenantId, id) || calculatedFieldService.referencedInAnyCalculatedField(tenantId, id))) {
            throw new DataValidationException("Can't delete device that has entity views or is referenced in calculated fields!");
        }

        Device device = deviceDao.findById(tenantId, id.getId());
        if (device == null) {
            return;
        }
        deleteDevice(tenantId, device);
    }

    private void deleteDevice(TenantId tenantId, Device device) {
        log.trace("Executing deleteDevice [{}]", device.getId());
        deviceCredentialsService.deleteDeviceCredentialsByDeviceId(tenantId, device.getId());
        deviceDao.removeById(tenantId, device.getUuidId());

        DeviceCacheEvictEvent deviceCacheEvictEvent = new DeviceCacheEvictEvent(device.getTenantId(), device.getId(), device.getName(), null);
        publishEvictEvent(deviceCacheEvictEvent);
        countService.publishCountEntityEvictEvent(tenantId, EntityType.DEVICE);
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(device.getId()).entity(device).build());
    }

    @Override
    public PageData<Device> findDevicesByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findDevicesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return deviceDao.findDevicesByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<DeviceInfo> findDeviceInfosByFilter(DeviceInfoFilter filter, PageLink pageLink) {
        log.trace("Executing findDeviceInfosByFilter, filter [{}], pageLink [{}]", filter, pageLink);
        if (filter == null) {
            throw new IncorrectParameterException("Filter is empty!");
        }
        validateId(filter.getTenantId(), id -> INCORRECT_TENANT_ID + id);
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
    public PageData<ProfileEntityIdInfo> findProfileEntityIdInfos(PageLink pageLink) {
        log.trace("Executing findProfileEntityIdInfos, pageLink [{}]", pageLink);
        validatePageLink(pageLink);
        return deviceDao.findProfileEntityIdInfos(pageLink);
    }

    @Override
    public PageData<ProfileEntityIdInfo> findProfileEntityIdInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findProfileEntityIdInfosByTenantId, tenantId[{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return deviceDao.findProfileEntityIdInfosByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndType(TenantId tenantId, String type, PageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateString(type, t -> "Incorrect type " + t);
        validatePageLink(pageLink);
        return deviceDao.findDevicesByTenantIdAndType(tenantId.getId(), type, pageLink);
    }

    @Override
    public PageData<DeviceId> findDeviceIdsByTenantIdAndDeviceProfileId(TenantId tenantId, DeviceProfileId deviceProfileId, PageLink pageLink) {
        log.trace("Executing findDeviceIdsByTenantIdAndType, tenantId [{}], deviceProfileId [{}], pageLink [{}]", tenantId, deviceProfileId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(deviceProfileId, id -> INCORRECT_DEVICE_PROFILE_ID + id);
        validatePageLink(pageLink);
        return deviceDao.findDeviceIdsByTenantIdAndDeviceProfileId(tenantId.getId(), deviceProfileId.getId(), pageLink);
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndTypeAndEmptyOtaPackage(TenantId tenantId,
                                                                           DeviceProfileId deviceProfileId,
                                                                           OtaPackageType type,
                                                                           PageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndTypeAndEmptyOtaPackage, tenantId [{}], deviceProfileId [{}], type [{}], pageLink [{}]",
                tenantId, deviceProfileId, type, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(deviceProfileId, id -> INCORRECT_DEVICE_PROFILE_ID + id);
        validatePageLink(pageLink);
        return deviceDao.findDevicesByTenantIdAndTypeAndEmptyOtaPackage(tenantId.getId(), deviceProfileId.getId(), type, pageLink);
    }

    @Override
    public long countDevicesByTenantIdAndDeviceProfileIdAndEmptyOtaPackage(TenantId tenantId, DeviceProfileId deviceProfileId, OtaPackageType type) {
        log.trace("Executing countDevicesByTenantIdAndDeviceProfileIdAndEmptyOtaPackage, tenantId [{}], deviceProfileId [{}], type [{}]", tenantId, deviceProfileId, type);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(deviceProfileId, id -> INCORRECT_DEVICE_PROFILE_ID + id);
        return deviceDao.countDevicesByTenantIdAndDeviceProfileIdAndEmptyOtaPackage(tenantId.getId(), deviceProfileId.getId(), type);
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdAndIdsAsync(TenantId tenantId, List<DeviceId> deviceIds) {
        log.trace("Executing findDevicesByTenantIdAndIdsAsync, tenantId [{}], deviceIds [{}]", tenantId, deviceIds);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateIds(deviceIds, ids -> "Incorrect deviceIds " + ids);
        return deviceDao.findDevicesByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(deviceIds));
    }

    @Override
    public List<Device> findDevicesByIds(List<DeviceId> deviceIds) {
        log.trace("Executing findDevicesByIdsAsync, deviceIds [{}]", deviceIds);
        validateIds(deviceIds, ids -> "Incorrect deviceIds " + ids);
        return deviceDao.findDevicesByIds(toUUIDs(deviceIds));
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByIdsAsync(List<DeviceId> deviceIds) {
        log.trace("Executing findDevicesByIdsAsync, deviceIds [{}]", deviceIds);
        validateIds(deviceIds, ids -> "Incorrect deviceIds " + ids);
        return deviceDao.findDevicesByIdsAsync(toUUIDs(deviceIds));
    }

    @Transactional
    @Override
    public void deleteDevicesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteDevicesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        tenantDevicesRemover.removeEntities(tenantId, tenantId);
    }

    @Transactional
    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteDevicesByTenantId(tenantId);
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, PageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validatePageLink(pageLink);
        return deviceDao.findDevicesByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, PageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validateString(type, t -> "Incorrect type " + t);
        validatePageLink(pageLink);
        return deviceDao.findDevicesByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdCustomerIdAndIdsAsync(TenantId tenantId, CustomerId customerId, List<DeviceId> deviceIds) {
        log.trace("Executing findDevicesByTenantIdCustomerIdAndIdsAsync, tenantId [{}], customerId [{}], deviceIds [{}]", tenantId, customerId, deviceIds);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validateIds(deviceIds, ids -> "Incorrect deviceIds " + ids);
        return deviceDao.findDevicesByTenantIdCustomerIdAndIdsAsync(tenantId.getId(),
                customerId.getId(), toUUIDs(deviceIds));
    }

    @Override
    public void unassignCustomerDevices(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing unassignCustomerDevices, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        customerDevicesRemover.removeEntities(tenantId, customerId);
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
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        return deviceDao.findTenantDeviceTypesAsync(tenantId.getId());
    }

    @Transactional
    @Override
    public Device assignDeviceToTenant(TenantId tenantId, Device device) {
        log.trace("Executing assignDeviceToTenant [{}][{}]", tenantId, device);
        TenantId oldTenantId = device.getTenantId();
        Tenant oldTenant = tenantService.findTenantById(oldTenantId);
        List<EntityView> entityViews = entityViewService.findEntityViewsByTenantIdAndEntityId(oldTenantId, device.getId());
        if (!CollectionUtils.isEmpty(entityViews)) {
            throw new DataValidationException("Can't assign device that has entity views to another tenant!");
        }

        eventService.removeEvents(oldTenantId, device.getId());

        relationService.removeRelations(oldTenantId, device.getId());

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

        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).entity(savedDevice)
                .entityId(savedDevice.getId()).body(JacksonUtil.toString(oldTenant)).actionType(ActionType.ASSIGNED_TO_TENANT).build());

        return savedDevice;
    }

    @Override
    @Transactional
    public Device saveDevice(ProvisionRequest provisionRequest, DeviceProfile profile) {
        Device device = new Device();
        device.setName(provisionRequest.getDeviceName());
        device.setType(profile.getName());
        device.setTenantId(profile.getTenantId());
        if (provisionRequest.getGateway() != null && provisionRequest.getGateway()) {
            ObjectNode additionalInfoNode = JacksonUtil.newObjectNode();
            additionalInfoNode.put(DataConstants.GATEWAY_PARAMETER, true);
            device.setAdditionalInfo(additionalInfoNode);
        }
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
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).edgeId(edgeId).entityId(deviceId)
                .actionType(ActionType.ASSIGNED_TO_EDGE).build());
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
        eventPublisher.publishEvent(ActionEntityEvent.builder().tenantId(tenantId).edgeId(edgeId).entityId(deviceId)
                .actionType(ActionType.UNASSIGNED_FROM_EDGE).build());
        return device;
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndEdgeId(TenantId tenantId, EdgeId edgeId, PageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndEdgeId, tenantId [{}], edgeId [{}], pageLink [{}]", tenantId, edgeId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(edgeId, id -> INCORRECT_EDGE_ID + id);
        validatePageLink(pageLink);
        return deviceDao.findDevicesByTenantIdAndEdgeId(tenantId.getId(), edgeId.getId(), pageLink);
    }

    @Override
    public PageData<Device> findDevicesByTenantIdAndEdgeIdAndType(TenantId tenantId, EdgeId edgeId, String type, PageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndEdgeIdAndType, tenantId [{}], edgeId [{}], type [{}] pageLink [{}]", tenantId, edgeId, type, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(edgeId, id -> INCORRECT_EDGE_ID + id);
        validateString(type, t -> "Incorrect type " + t);
        validatePageLink(pageLink);
        return deviceDao.findDevicesByTenantIdAndEdgeIdAndType(tenantId.getId(), edgeId.getId(), type, pageLink);
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return deviceDao.countByTenantId(tenantId);
    }

    private final PaginatedRemover<TenantId, Device> tenantDevicesRemover = new PaginatedRemover<>() {

        @Override
        protected PageData<Device> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return deviceDao.findDevicesByTenantId(id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, Device device) {
            deleteDevice(tenantId, device);
        }
    };

    private final PaginatedRemover<CustomerId, Device> customerDevicesRemover = new PaginatedRemover<>() {

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

}
