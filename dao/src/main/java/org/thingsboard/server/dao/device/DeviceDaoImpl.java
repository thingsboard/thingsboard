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

import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.in;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static org.thingsboard.server.dao.model.ModelConstants.*;

import java.util.*;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.mapping.Result;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.AbstractSearchTextDao;
import org.thingsboard.server.dao.model.DeviceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsboard.server.dao.model.TenantDeviceTypeEntity;

import javax.annotation.Nullable;

@Component
@Slf4j
public class DeviceDaoImpl extends AbstractSearchTextDao<DeviceEntity> implements DeviceDao {

    @Override
    protected Class<DeviceEntity> getColumnFamilyClass() {
        return DeviceEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return DEVICE_COLUMN_FAMILY_NAME;
    }

    @Override
    public DeviceEntity save(Device device) {
        log.debug("Save device [{}] ", device);
        return save(new DeviceEntity(device));
    }

    @Override
    public List<DeviceEntity> findDevicesByTenantId(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find devices by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<DeviceEntity> deviceEntities = findPageWithTextSearch(DEVICE_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Collections.singletonList(eq(DEVICE_TENANT_ID_PROPERTY, tenantId)), pageLink);

        log.trace("Found devices [{}] by tenantId [{}] and pageLink [{}]", deviceEntities, tenantId, pageLink);
        return deviceEntities;
    }

    @Override
    public List<DeviceEntity> findDevicesByTenantIdAndType(UUID tenantId, String type, TextPageLink pageLink) {
        log.debug("Try to find devices by tenantId [{}], type [{}] and pageLink [{}]", tenantId, type, pageLink);
        List<DeviceEntity> deviceEntities = findPageWithTextSearch(DEVICE_BY_TENANT_BY_TYPE_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(DEVICE_TYPE_PROPERTY, type),
                              eq(DEVICE_TENANT_ID_PROPERTY, tenantId)), pageLink);
        log.trace("Found devices [{}] by tenantId [{}], type [{}] and pageLink [{}]", deviceEntities, tenantId, type, pageLink);
        return deviceEntities;
    }

    @Override
    public ListenableFuture<List<DeviceEntity>> findDevicesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> deviceIds) {
        log.debug("Try to find devices by tenantId [{}] and device Ids [{}]", tenantId, deviceIds);
        Select select = select().from(getColumnFamilyName());
        Select.Where query = select.where();
        query.and(eq(DEVICE_TENANT_ID_PROPERTY, tenantId));
        query.and(in(ID_PROPERTY, deviceIds));
        return findListByStatementAsync(query);
    }

    @Override
    public List<DeviceEntity> findDevicesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TextPageLink pageLink) {
        log.debug("Try to find devices by tenantId [{}], customerId [{}] and pageLink [{}]", tenantId, customerId, pageLink);
        List<DeviceEntity> deviceEntities = findPageWithTextSearch(DEVICE_BY_CUSTOMER_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(DEVICE_CUSTOMER_ID_PROPERTY, customerId),
                        eq(DEVICE_TENANT_ID_PROPERTY, tenantId)),
                pageLink);

        log.trace("Found devices [{}] by tenantId [{}], customerId [{}] and pageLink [{}]", deviceEntities, tenantId, customerId, pageLink);
        return deviceEntities;
    }

    @Override
    public List<DeviceEntity> findDevicesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, TextPageLink pageLink) {
        log.debug("Try to find devices by tenantId [{}], customerId [{}], type [{}] and pageLink [{}]", tenantId, customerId, type, pageLink);
        List<DeviceEntity> deviceEntities = findPageWithTextSearch(DEVICE_BY_CUSTOMER_BY_TYPE_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(DEVICE_TYPE_PROPERTY, type),
                              eq(DEVICE_CUSTOMER_ID_PROPERTY, customerId),
                              eq(DEVICE_TENANT_ID_PROPERTY, tenantId)),
                pageLink);

        log.trace("Found devices [{}] by tenantId [{}], customerId [{}], type [{}] and pageLink [{}]", deviceEntities, tenantId, customerId, type, pageLink);
        return deviceEntities;
    }

    @Override
    public ListenableFuture<List<DeviceEntity>> findDevicesByTenantIdCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> deviceIds) {
        log.debug("Try to find devices by tenantId [{}], customerId [{}] and device Ids [{}]", tenantId, customerId, deviceIds);
        Select select = select().from(getColumnFamilyName());
        Select.Where query = select.where();
        query.and(eq(DEVICE_TENANT_ID_PROPERTY, tenantId));
        query.and(eq(DEVICE_CUSTOMER_ID_PROPERTY, customerId));
        query.and(in(ID_PROPERTY, deviceIds));
        return findListByStatementAsync(query);
    }

    @Override
    public Optional<DeviceEntity> findDevicesByTenantIdAndName(UUID tenantId, String deviceName) {
        Select select = select().from(DEVICE_BY_TENANT_AND_NAME_VIEW_NAME);
        Select.Where query = select.where();
        query.and(eq(DEVICE_TENANT_ID_PROPERTY, tenantId));
        query.and(eq(DEVICE_NAME_PROPERTY, deviceName));
        return Optional.ofNullable(findOneByStatement(query));
    }

    @Override
    public ListenableFuture<List<TenantDeviceTypeEntity>> findTenantDeviceTypesAsync() {
        Select statement = select().distinct().column(DEVICE_TYPE_PROPERTY).column(DEVICE_TENANT_ID_PROPERTY).from(DEVICE_TYPES_BY_TENANT_VIEW_NAME);
        statement.setConsistencyLevel(cluster.getDefaultReadConsistencyLevel());
        ResultSetFuture resultSetFuture = getSession().executeAsync(statement);
        ListenableFuture<List<TenantDeviceTypeEntity>> result = Futures.transform(resultSetFuture, new Function<ResultSet, List<TenantDeviceTypeEntity>>() {
            @Nullable
            @Override
            public List<TenantDeviceTypeEntity> apply(@Nullable ResultSet resultSet) {
                Result<TenantDeviceTypeEntity> result = cluster.getMapper(TenantDeviceTypeEntity.class).map(resultSet);
                if (result != null) {
                    return result.all();
                } else {
                    return Collections.emptyList();
                }
            }
        });
        return result;
    }

}
