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
package org.thingsboard.server.dao.sql.device;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.model.sql.DeviceEntity;
import org.thingsboard.server.dao.relation.RelationDao;
import org.thingsboard.server.dao.sql.JpaAbstractSearchTextDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.*;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUID;
import static org.thingsboard.server.common.data.UUIDConverter.fromTimeUUIDs;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID_STR;

/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@Component
@SqlDao
public class JpaDeviceDao extends JpaAbstractSearchTextDao<DeviceEntity, Device> implements DeviceDao {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private RelationDao relationDao;

    @Override
    protected Class<DeviceEntity> getEntityClass() {
        return DeviceEntity.class;
    }

    @Override
    protected CrudRepository<DeviceEntity, String> getCrudRepository() {
        return deviceRepository;
    }

    @Override
    public List<Device> findDevicesByTenantId(UUID tenantId, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                deviceRepository.findByTenantId(
                        fromTimeUUID(tenantId),
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> deviceIds) {
        return service.submit(() -> DaoUtil.convertDataList(deviceRepository.findDevicesByTenantIdAndIdIn(UUIDConverter.fromTimeUUID(tenantId), fromTimeUUIDs(deviceIds))));
    }

    @Override
    public List<Device> findDevicesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TextPageLink pageLink) {
        List<EntityRelation> relations = relationDao.findRelations(new CustomerId(customerId),
                EntityRelation.CONTAINS_TYPE, RelationTypeGroup.DEVICE, EntityType.DEVICE);

        List<Device> deviceList = findDevicesByTenantId(tenantId, pageLink);

        return DaoUtil.filterDataListByRelation(relations, deviceList);
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> deviceIds) {
        List<EntityRelation> relations = relationDao.findRelations(new CustomerId(customerId),
                EntityRelation.CONTAINS_TYPE, RelationTypeGroup.DEVICE, EntityType.DEVICE);

        List<UUID> ids = relations.stream().map(entityRelation -> entityRelation.getTo().getId()).collect(Collectors.toList());
        deviceIds.retainAll(ids);
        return findDevicesByTenantIdAndIdsAsync(tenantId, deviceIds);
    }

    @Override
    public Optional<Device> findDeviceByTenantIdAndName(UUID tenantId, String name) {
        Device device = DaoUtil.getData(deviceRepository.findByTenantIdAndName(fromTimeUUID(tenantId), name));
        return Optional.ofNullable(device);
    }

    @Override
    public List<Device> findDevicesByTenantIdAndType(UUID tenantId, String type, TextPageLink pageLink) {
        return DaoUtil.convertDataList(
                deviceRepository.findByTenantIdAndType(
                        fromTimeUUID(tenantId),
                        type,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        pageLink.getIdOffset() == null ? NULL_UUID_STR : fromTimeUUID(pageLink.getIdOffset()),
                        new PageRequest(0, pageLink.getLimit())));
    }

    @Override
    public List<Device> findDevicesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, TextPageLink pageLink) {
        List<EntityRelation> relations = relationDao.findRelations(new CustomerId(customerId),
                EntityRelation.CONTAINS_TYPE, RelationTypeGroup.DEVICE, EntityType.DEVICE);

        List<Device> deviceList = findDevicesByTenantIdAndType(tenantId, type, pageLink);

        return DaoUtil.filterDataListByRelation(relations, deviceList);
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findTenantDeviceTypesAsync(UUID tenantId) {
        return service.submit(() -> convertTenantDeviceTypesToDto(tenantId, deviceRepository.findTenantDeviceTypes(fromTimeUUID(tenantId))));
    }

    private List<EntitySubtype> convertTenantDeviceTypesToDto(UUID tenantId, List<String> types) {
        List<EntitySubtype> list = Collections.emptyList();
        if (types != null && !types.isEmpty()) {
            list = new ArrayList<>();
            for (String type : types) {
                list.add(new EntitySubtype(new TenantId(tenantId), EntityType.DEVICE, type));
            }
        }
        return list;
    }
}
