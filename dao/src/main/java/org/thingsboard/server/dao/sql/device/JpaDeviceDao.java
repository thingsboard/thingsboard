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
package org.thingsboard.server.dao.sql.device;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.TenantDeviceType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.model.sql.DeviceEntity;
import org.thingsboard.server.dao.model.sql.TenantDeviceTypeEntity;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;

import java.util.*;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@Component
@ConditionalOnProperty(prefix = "sql", value = "enabled", havingValue = "true", matchIfMissing = false)
public class JpaDeviceDao extends JpaAbstractSearchTextDao<DeviceEntity, Device> implements DeviceDao {

    @Autowired
    private DeviceRepository deviceRepository;

    @Override
    protected Class<DeviceEntity> getEntityClass() {
        return DeviceEntity.class;
    }

    @Override
    protected CrudRepository<DeviceEntity, UUID> getCrudRepository() {
        return deviceRepository;
    }

    @Override
    public List<Device> findDevicesByTenantId(UUID tenantId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                deviceRepository.findByTenantId(
                        pageLink.getLimit(),
                        tenantId,
                        pageLink.getTextSearch(),
                        pageLink.getIdOffset() == null ? NULL_UUID : pageLink.getIdOffset())
        );
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> deviceIds) {
        return service.submit(() -> DaoUtil.convertDataList(deviceRepository.findDevicesByTenantIdAndIdIn(tenantId, deviceIds)));
    }

    @Override
    public List<Device> findDevicesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                deviceRepository.findByTenantIdAndCustomerId(
                        pageLink.getLimit(),
                        tenantId,
                        customerId,
                        pageLink.getTextSearch(),
                        pageLink.getIdOffset() == null ? NULL_UUID : pageLink.getIdOffset())
        );
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> deviceIds) {
        return service.submit(() -> DaoUtil.convertDataList(
                deviceRepository.findDevicesByTenantIdAndCustomerIdAndIdIn(tenantId, customerId, deviceIds)));
    }

    @Override
    public Optional<Device> findDeviceByTenantIdAndName(UUID tenantId, String name) {
        Device device = DaoUtil.getData(deviceRepository.findByTenantIdAndName(tenantId, name));
        return Optional.ofNullable(device);
    }

    @Override
    public List<Device> findDevicesByTenantIdAndType(UUID tenantId, String type, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                deviceRepository.findByTenantIdAndType(
                        pageLink.getLimit(),
                        tenantId,
                        type,
                        pageLink.getTextSearch(),
                        pageLink.getIdOffset() == null ? NULL_UUID : pageLink.getIdOffset())
        );
    }

    @Override
    public List<Device> findDevicesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                deviceRepository.findByTenantIdAndCustomerIdAndType(
                        pageLink.getLimit(),
                        tenantId,
                        customerId,
                        type,
                        pageLink.getTextSearch(),
                        pageLink.getIdOffset() == null ? NULL_UUID : pageLink.getIdOffset())
        );
    }

    @Override
    public ListenableFuture<List<TenantDeviceType>> findTenantDeviceTypesAsync() {
        return service.submit(() -> convertTenantDeviceTypeEntityToDto(deviceRepository.findTenantDeviceTypes()));
    }

    private List<TenantDeviceType> convertTenantDeviceTypeEntityToDto(List<TenantDeviceTypeEntity> entities) {
        List<TenantDeviceType> list = Collections.emptyList();
        if (entities != null && !entities.isEmpty()) {
            list = new ArrayList<>();
            for (TenantDeviceTypeEntity entity : entities) {
                list.add(new TenantDeviceType(entity.getType(), new TenantId(entity.getTenantId())));
            }
        }
        return list;
    }
}
