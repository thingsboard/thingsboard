/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.device.DeviceSearchQuery;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.tenant.TenantDao;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.CacheConstants.DEVICE_CACHE;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Service
@Slf4j
public class DeviceServiceImpl extends AbstractEntityService implements DeviceService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_PAGE_LINK = "Incorrect page link ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_DEVICE_ID = "Incorrect deviceId ";
    @Autowired
    private DeviceDao deviceDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    @Autowired
    private CacheManager cacheManager;

    @Override
    public Device findDeviceById(DeviceId deviceId) {
        log.trace("Executing findDeviceById [{}]", deviceId);
        validateId(deviceId, INCORRECT_DEVICE_ID + deviceId);
        return deviceDao.findById(deviceId.getId());
    }

    @Override
    public ListenableFuture<Device> findDeviceByIdAsync(DeviceId deviceId) {
        log.trace("Executing findDeviceById [{}]", deviceId);
        validateId(deviceId, INCORRECT_DEVICE_ID + deviceId);
        return deviceDao.findByIdAsync(deviceId.getId());
    }

    @Cacheable(cacheNames = DEVICE_CACHE, key = "{#tenantId, #name}")
    @Override
    public Device findDeviceByTenantIdAndName(TenantId tenantId, String name) {
        log.trace("Executing findDeviceByTenantIdAndName [{}][{}]", tenantId, name);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        Optional<Device> deviceOpt = deviceDao.findDeviceByTenantIdAndName(tenantId.getId(), name);
        return deviceOpt.orElse(null);
    }

    @CacheEvict(cacheNames = DEVICE_CACHE, key = "{#device.tenantId, #device.name}")
    @Override
    public Device saveDevice(Device device) {
        log.trace("Executing saveDevice [{}]", device);
        deviceValidator.validate(device);
        Device savedDevice = deviceDao.save(device);
        if (device.getId() == null) {
            DeviceCredentials deviceCredentials = new DeviceCredentials();
            deviceCredentials.setDeviceId(new DeviceId(savedDevice.getUuidId()));
            deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
            deviceCredentials.setCredentialsId(RandomStringUtils.randomAlphanumeric(20));
            deviceCredentialsService.createDeviceCredentials(deviceCredentials);
        }
        return savedDevice;
    }

    @Override
    public Device assignDeviceToCustomer(DeviceId deviceId, CustomerId customerId) {
        Device device = findDeviceById(deviceId);
        device.setCustomerId(customerId);
        return saveDevice(device);
    }

    @Override
    public Device unassignDeviceFromCustomer(DeviceId deviceId) {
        Device device = findDeviceById(deviceId);
        device.setCustomerId(null);
        return saveDevice(device);
    }

    @Override
    public void deleteDevice(DeviceId deviceId) {
        log.trace("Executing deleteDevice [{}]", deviceId);
        Cache cache = cacheManager.getCache(DEVICE_CACHE);
        validateId(deviceId, INCORRECT_DEVICE_ID + deviceId);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(deviceId);
        if (deviceCredentials != null) {
            deviceCredentialsService.deleteDeviceCredentials(deviceCredentials);
        }
        deleteEntityRelations(deviceId);
        Device device = deviceDao.findById(deviceId.getId());
        List<Object> list = new ArrayList<>();
        list.add(device.getTenantId());
        list.add(device.getName());
        cache.evict(list);
        deviceDao.removeById(deviceId.getId());
    }

    @Override
    public TextPageData<Device> findDevicesByTenantId(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findDevicesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Device> devices = deviceDao.findDevicesByTenantId(tenantId.getId(), pageLink);
        return new TextPageData<>(devices, pageLink);
    }

    @Override
    public TextPageData<Device> findDevicesByTenantIdAndType(TenantId tenantId, String type, TextPageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Device> devices = deviceDao.findDevicesByTenantIdAndType(tenantId.getId(), type, pageLink);
        return new TextPageData<>(devices, pageLink);
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdAndIdsAsync(TenantId tenantId, List<DeviceId> deviceIds) {
        log.trace("Executing findDevicesByTenantIdAndIdsAsync, tenantId [{}], deviceIds [{}]", tenantId, deviceIds);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateIds(deviceIds, "Incorrect deviceIds " + deviceIds);
        return deviceDao.findDevicesByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(deviceIds));
    }


    @Override
    public void deleteDevicesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteDevicesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        tenantDevicesRemover.removeEntities(tenantId);
    }

    @Override
    public TextPageData<Device> findDevicesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, TextPageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Device> devices = deviceDao.findDevicesByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
        return new TextPageData<>(devices, pageLink);
    }

    @Override
    public TextPageData<Device> findDevicesByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, TextPageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        validateId(customerId, INCORRECT_CUSTOMER_ID + customerId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink, INCORRECT_PAGE_LINK + pageLink);
        List<Device> devices = deviceDao.findDevicesByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
        return new TextPageData<>(devices, pageLink);
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
        new CustomerDevicesUnassigner(tenantId).removeEntities(customerId);
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByQuery(DeviceSearchQuery query) {
        ListenableFuture<List<EntityRelation>> relations = relationService.findByQuery(query.toEntitySearchQuery());
        ListenableFuture<List<Device>> devices = Futures.transformAsync(relations, r -> {
            EntitySearchDirection direction = query.toEntitySearchQuery().getParameters().getDirection();
            List<ListenableFuture<Device>> futures = new ArrayList<>();
            for (EntityRelation relation : r) {
                EntityId entityId = direction == EntitySearchDirection.FROM ? relation.getTo() : relation.getFrom();
                if (entityId.getEntityType() == EntityType.DEVICE) {
                    futures.add(findDeviceByIdAsync(new DeviceId(entityId.getId())));
                }
            }
            return Futures.successfulAsList(futures);
        });

        devices = Futures.transform(devices, new Function<List<Device>, List<Device>>() {
            @Nullable
            @Override
            public List<Device> apply(@Nullable List<Device> deviceList) {
                return deviceList == null ? Collections.emptyList() : deviceList.stream().filter(device -> query.getDeviceTypes().contains(device.getType())).collect(Collectors.toList());
            }
        });

        return devices;
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findDeviceTypesByTenantId(TenantId tenantId) {
        log.trace("Executing findDeviceTypesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, INCORRECT_TENANT_ID + tenantId);
        ListenableFuture<List<EntitySubtype>> tenantDeviceTypes = deviceDao.findTenantDeviceTypesAsync(tenantId.getId());
        return Futures.transform(tenantDeviceTypes,
                (Function<List<EntitySubtype>, List<EntitySubtype>>) deviceTypes -> {
                    deviceTypes.sort(Comparator.comparing(EntitySubtype::getType));
                    return deviceTypes;
                });
    }

    private DataValidator<Device> deviceValidator =
            new DataValidator<Device>() {

                @Override
                protected void validateCreate(Device device) {
                    deviceDao.findDeviceByTenantIdAndName(device.getTenantId().getId(), device.getName()).ifPresent(
                            d -> {
                                throw new DataValidationException("Device with such name already exists!");
                            }
                    );
                }

                @Override
                protected void validateUpdate(Device device) {
                    deviceDao.findDeviceByTenantIdAndName(device.getTenantId().getId(), device.getName()).ifPresent(
                            d -> {
                                if (!d.getUuidId().equals(device.getUuidId())) {
                                    throw new DataValidationException("Device with such name already exists!");
                                }
                            }
                    );
                }

                @Override
                protected void validateDataImpl(Device device) {
                    if (StringUtils.isEmpty(device.getType())) {
                        throw new DataValidationException("Device type should be specified!");
                    }
                    if (StringUtils.isEmpty(device.getName())) {
                        throw new DataValidationException("Device name should be specified!");
                    }
                    if (device.getTenantId() == null) {
                        throw new DataValidationException("Device should be assigned to tenant!");
                    } else {
                        Tenant tenant = tenantDao.findById(device.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Device is referencing to non-existent tenant!");
                        }
                    }
                    if (device.getCustomerId() == null) {
                        device.setCustomerId(new CustomerId(NULL_UUID));
                    } else if (!device.getCustomerId().getId().equals(NULL_UUID)) {
                        Customer customer = customerDao.findById(device.getCustomerId().getId());
                        if (customer == null) {
                            throw new DataValidationException("Can't assign device to non-existent customer!");
                        }
                        if (!customer.getTenantId().getId().equals(device.getTenantId().getId())) {
                            throw new DataValidationException("Can't assign device to customer from different tenant!");
                        }
                    }
                }
            };

    private PaginatedRemover<TenantId, Device> tenantDevicesRemover =
            new PaginatedRemover<TenantId, Device>() {

                @Override
                protected List<Device> findEntities(TenantId id, TextPageLink pageLink) {
                    return deviceDao.findDevicesByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(Device entity) {
                    deleteDevice(new DeviceId(entity.getUuidId()));
                }
            };

    private class CustomerDevicesUnassigner extends PaginatedRemover<CustomerId, Device> {

        private TenantId tenantId;

        CustomerDevicesUnassigner(TenantId tenantId) {
            this.tenantId = tenantId;
        }

        @Override
        protected List<Device> findEntities(CustomerId id, TextPageLink pageLink) {
            return deviceDao.findDevicesByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(Device entity) {
            unassignDeviceFromCustomer(new DeviceId(entity.getUuidId()));
        }

    }
}
