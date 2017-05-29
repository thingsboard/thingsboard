/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.TenantDeviceType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.model.CustomerEntity;
import org.thingsboard.server.dao.model.DeviceEntity;
import org.thingsboard.server.dao.model.TenantDeviceTypeEntity;
import org.thingsboard.server.dao.model.TenantEntity;
import org.thingsboard.server.dao.relation.EntitySearchDirection;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.tenant.TenantDao;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.DaoUtil.*;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;
import static org.thingsboard.server.dao.service.Validator.*;

@Service
@Slf4j
public class DeviceServiceImpl extends AbstractEntityService implements DeviceService {

    @Autowired
    private DeviceDao deviceDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private CustomerDao customerDao;

    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    @Override
    public Device findDeviceById(DeviceId deviceId) {
        log.trace("Executing findDeviceById [{}]", deviceId);
        validateId(deviceId, "Incorrect deviceId " + deviceId);
        DeviceEntity deviceEntity = deviceDao.findById(deviceId.getId());
        return getData(deviceEntity);
    }

    @Override
    public ListenableFuture<Device> findDeviceByIdAsync(DeviceId deviceId) {
        log.trace("Executing findDeviceById [{}]", deviceId);
        validateId(deviceId, "Incorrect deviceId " + deviceId);
        ListenableFuture<DeviceEntity> deviceEntity = deviceDao.findByIdAsync(deviceId.getId());
        return Futures.transform(deviceEntity, (Function<? super DeviceEntity, ? extends Device>) input -> getData(input));
    }

    @Override
    public Optional<Device> findDeviceByTenantIdAndName(TenantId tenantId, String name) {
        log.trace("Executing findDeviceByTenantIdAndName [{}][{}]", tenantId, name);
        validateId(tenantId, "Incorrect tenantId " + tenantId);
        Optional<DeviceEntity> deviceEntityOpt = deviceDao.findDevicesByTenantIdAndName(tenantId.getId(), name);
        if (deviceEntityOpt.isPresent()) {
            return Optional.of(getData(deviceEntityOpt.get()));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Device saveDevice(Device device) {
        log.trace("Executing saveDevice [{}]", device);
        deviceValidator.validate(device);
        DeviceEntity deviceEntity = deviceDao.save(device);
        if (device.getId() == null) {
            DeviceCredentials deviceCredentials = new DeviceCredentials();
            deviceCredentials.setDeviceId(new DeviceId(deviceEntity.getId()));
            deviceCredentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
            deviceCredentials.setCredentialsId(RandomStringUtils.randomAlphanumeric(20));
            deviceCredentialsService.createDeviceCredentials(deviceCredentials);
        }
        return getData(deviceEntity);
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
        validateId(deviceId, "Incorrect deviceId " + deviceId);
        DeviceCredentials deviceCredentials = deviceCredentialsService.findDeviceCredentialsByDeviceId(deviceId);
        if (deviceCredentials != null) {
            deviceCredentialsService.deleteDeviceCredentials(deviceCredentials);
        }
        deleteEntityRelations(deviceId);
        deviceDao.removeById(deviceId.getId());
    }

    @Override
    public TextPageData<Device> findDevicesByTenantId(TenantId tenantId, TextPageLink pageLink) {
        log.trace("Executing findDevicesByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, "Incorrect tenantId " + tenantId);
        validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<DeviceEntity> deviceEntities = deviceDao.findDevicesByTenantId(tenantId.getId(), pageLink);
        List<Device> devices = convertDataList(deviceEntities);
        return new TextPageData<>(devices, pageLink);
    }

    @Override
    public TextPageData<Device> findDevicesByTenantIdAndType(TenantId tenantId, String type, TextPageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndType, tenantId [{}], type [{}], pageLink [{}]", tenantId, type, pageLink);
        validateId(tenantId, "Incorrect tenantId " + tenantId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<DeviceEntity> deviceEntities = deviceDao.findDevicesByTenantIdAndType(tenantId.getId(), type, pageLink);
        List<Device> devices = convertDataList(deviceEntities);
        return new TextPageData<>(devices, pageLink);
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdAndIdsAsync(TenantId tenantId, List<DeviceId> deviceIds) {
        log.trace("Executing findDevicesByTenantIdAndIdsAsync, tenantId [{}], deviceIds [{}]", tenantId, deviceIds);
        validateId(tenantId, "Incorrect tenantId " + tenantId);
        validateIds(deviceIds, "Incorrect deviceIds " + deviceIds);
        ListenableFuture<List<DeviceEntity>> deviceEntities = deviceDao.findDevicesByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(deviceIds));
        return Futures.transform(deviceEntities, (Function<List<DeviceEntity>, List<Device>>) input -> convertDataList(input));
    }


    @Override
    public void deleteDevicesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteDevicesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, "Incorrect tenantId " + tenantId);
        tenantDevicesRemover.removeEntitites(tenantId);
    }

    @Override
    public TextPageData<Device> findDevicesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId, TextPageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndCustomerId, tenantId [{}], customerId [{}], pageLink [{}]", tenantId, customerId, pageLink);
        validateId(tenantId, "Incorrect tenantId " + tenantId);
        validateId(customerId, "Incorrect customerId " + customerId);
        validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<DeviceEntity> deviceEntities = deviceDao.findDevicesByTenantIdAndCustomerId(tenantId.getId(), customerId.getId(), pageLink);
        List<Device> devices = convertDataList(deviceEntities);
        return new TextPageData<>(devices, pageLink);
    }

    @Override
    public TextPageData<Device> findDevicesByTenantIdAndCustomerIdAndType(TenantId tenantId, CustomerId customerId, String type, TextPageLink pageLink) {
        log.trace("Executing findDevicesByTenantIdAndCustomerIdAndType, tenantId [{}], customerId [{}], type [{}], pageLink [{}]", tenantId, customerId, type, pageLink);
        validateId(tenantId, "Incorrect tenantId " + tenantId);
        validateId(customerId, "Incorrect customerId " + customerId);
        validateString(type, "Incorrect type " + type);
        validatePageLink(pageLink, "Incorrect page link " + pageLink);
        List<DeviceEntity> deviceEntities = deviceDao.findDevicesByTenantIdAndCustomerIdAndType(tenantId.getId(), customerId.getId(), type, pageLink);
        List<Device> devices = convertDataList(deviceEntities);
        return new TextPageData<>(devices, pageLink);
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdCustomerIdAndIdsAsync(TenantId tenantId, CustomerId customerId, List<DeviceId> deviceIds) {
        log.trace("Executing findDevicesByTenantIdCustomerIdAndIdsAsync, tenantId [{}], customerId [{}], deviceIds [{}]", tenantId, customerId, deviceIds);
        validateId(tenantId, "Incorrect tenantId " + tenantId);
        validateId(customerId, "Incorrect customerId " + customerId);
        validateIds(deviceIds, "Incorrect deviceIds " + deviceIds);
        ListenableFuture<List<DeviceEntity>> deviceEntities = deviceDao.findDevicesByTenantIdCustomerIdAndIdsAsync(tenantId.getId(),
                customerId.getId(), toUUIDs(deviceIds));
        return Futures.transform(deviceEntities, (Function<List<DeviceEntity>, List<Device>>) input -> convertDataList(input));
    }

    @Override
    public void unassignCustomerDevices(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing unassignCustomerDevices, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, "Incorrect tenantId " + tenantId);
        validateId(customerId, "Incorrect customerId " + customerId);
        new CustomerDevicesUnassigner(tenantId).removeEntitites(customerId);
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByQuery(DeviceSearchQuery query) {
        ListenableFuture<List<EntityRelation>> relations = relationService.findByQuery(query.toEntitySearchQuery());
        ListenableFuture<List<Device>> devices = Futures.transform(relations, (AsyncFunction<List<EntityRelation>, List<Device>>) relations1 -> {
            EntitySearchDirection direction = query.toEntitySearchQuery().getParameters().getDirection();
            List<ListenableFuture<Device>> futures = new ArrayList<>();
            for (EntityRelation relation : relations1) {
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
                return deviceList.stream().filter(device -> query.getDeviceTypes().contains(device.getType())).collect(Collectors.toList());
            }
        });

        return devices;
    }

    @Override
    public ListenableFuture<List<TenantDeviceType>> findDeviceTypesByTenantId(TenantId tenantId) {
        log.trace("Executing findDeviceTypesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, "Incorrect tenantId " + tenantId);
        ListenableFuture<List<TenantDeviceTypeEntity>> tenantDeviceTypeEntities = deviceDao.findTenantDeviceTypesAsync();
        ListenableFuture<List<TenantDeviceType>> tenantDeviceTypes = Futures.transform(tenantDeviceTypeEntities,
            (Function<List<TenantDeviceTypeEntity>, List<TenantDeviceType>>) deviceTypeEntities -> {
                List<TenantDeviceType> deviceTypes = new ArrayList<>();
                for (TenantDeviceTypeEntity deviceTypeEntity : deviceTypeEntities) {
                    if (deviceTypeEntity.getTenantId().equals(tenantId.getId())) {
                        deviceTypes.add(deviceTypeEntity.toTenantDeviceType());
                    }
                }
                deviceTypes.sort((TenantDeviceType o1, TenantDeviceType o2) -> o1.getType().compareTo(o2.getType()));
                return deviceTypes;
            });
        return tenantDeviceTypes;
    }

    private DataValidator<Device> deviceValidator =
            new DataValidator<Device>() {

                @Override
                protected void validateCreate(Device device) {
                    deviceDao.findDevicesByTenantIdAndName(device.getTenantId().getId(), device.getName()).ifPresent(
                            d -> {
                                throw new DataValidationException("Device with such name already exists!");
                            }
                    );
                }

                @Override
                protected void validateUpdate(Device device) {
                    deviceDao.findDevicesByTenantIdAndName(device.getTenantId().getId(), device.getName()).ifPresent(
                            d -> {
                                if (!d.getId().equals(device.getUuidId())) {
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
                        TenantEntity tenant = tenantDao.findById(device.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Device is referencing to non-existent tenant!");
                        }
                    }
                    if (device.getCustomerId() == null) {
                        device.setCustomerId(new CustomerId(NULL_UUID));
                    } else if (!device.getCustomerId().getId().equals(NULL_UUID)) {
                        CustomerEntity customer = customerDao.findById(device.getCustomerId().getId());
                        if (customer == null) {
                            throw new DataValidationException("Can't assign device to non-existent customer!");
                        }
                        if (!customer.getTenantId().equals(device.getTenantId().getId())) {
                            throw new DataValidationException("Can't assign device to customer from different tenant!");
                        }
                    }
                }
            };

    private PaginatedRemover<TenantId, DeviceEntity> tenantDevicesRemover =
            new PaginatedRemover<TenantId, DeviceEntity>() {

                @Override
                protected List<DeviceEntity> findEntities(TenantId id, TextPageLink pageLink) {
                    return deviceDao.findDevicesByTenantId(id.getId(), pageLink);
                }

                @Override
                protected void removeEntity(DeviceEntity entity) {
                    deleteDevice(new DeviceId(entity.getId()));
                }
            };

    class CustomerDevicesUnassigner extends PaginatedRemover<CustomerId, DeviceEntity> {

        private TenantId tenantId;

        CustomerDevicesUnassigner(TenantId tenantId) {
            this.tenantId = tenantId;
        }

        @Override
        protected List<DeviceEntity> findEntities(CustomerId id, TextPageLink pageLink) {
            return deviceDao.findDevicesByTenantIdAndCustomerId(tenantId.getId(), id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(DeviceEntity entity) {
            unassignDeviceFromCustomer(new DeviceId(entity.getId()));
        }

    }
}
