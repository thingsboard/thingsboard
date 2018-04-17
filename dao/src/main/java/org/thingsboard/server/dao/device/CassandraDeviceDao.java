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

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.datastax.driver.mapping.Result;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.device.DeviceStatusQuery;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.EntitySubtypeEntity;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.nosql.DeviceEntity;
import org.thingsboard.server.dao.nosql.CassandraAbstractSearchTextDao;
import org.thingsboard.server.dao.util.NoSqlDao;

import javax.annotation.Nullable;
import java.util.*;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;
import static org.thingsboard.server.dao.model.ModelConstants.*;

@Component
@Slf4j
@NoSqlDao
public class CassandraDeviceDao extends CassandraAbstractSearchTextDao<DeviceEntity, Device> implements DeviceDao {

    @Override
    protected Class<DeviceEntity> getColumnFamilyClass() {
        return DeviceEntity.class;
    }

    @Override
    protected String getColumnFamilyName() {
        return DEVICE_COLUMN_FAMILY_NAME;
    }

    @Override
    public Device save(Device domain) {
        Device savedDevice = super.save(domain);
        EntitySubtype entitySubtype = new EntitySubtype(savedDevice.getTenantId(), EntityType.DEVICE, savedDevice.getType());
        EntitySubtypeEntity entitySubtypeEntity = new EntitySubtypeEntity(entitySubtype);
        Statement saveStatement = cluster.getMapper(EntitySubtypeEntity.class).saveQuery(entitySubtypeEntity);
        executeWrite(saveStatement);
        return savedDevice;
    }

    @Override
    public List<Device> findDevicesByTenantId(UUID tenantId, TextPageLink pageLink) {
        log.debug("Try to find devices by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        List<DeviceEntity> deviceEntities = findPageWithTextSearch(DEVICE_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Collections.singletonList(eq(DEVICE_TENANT_ID_PROPERTY, tenantId)), pageLink);

        log.trace("Found devices [{}] by tenantId [{}] and pageLink [{}]", deviceEntities, tenantId, pageLink);
        return DaoUtil.convertDataList(deviceEntities);
    }

    @Override
    public List<Device> findDevicesByTenantIdAndType(UUID tenantId, String type, TextPageLink pageLink) {
        log.debug("Try to find devices by tenantId [{}], type [{}] and pageLink [{}]", tenantId, type, pageLink);
        List<DeviceEntity> deviceEntities = findPageWithTextSearch(DEVICE_BY_TENANT_BY_TYPE_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(DEVICE_TYPE_PROPERTY, type),
                        eq(DEVICE_TENANT_ID_PROPERTY, tenantId)), pageLink);
        log.trace("Found devices [{}] by tenantId [{}], type [{}] and pageLink [{}]", deviceEntities, tenantId, type, pageLink);
        return DaoUtil.convertDataList(deviceEntities);
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdAndIdsAsync(UUID tenantId, List<UUID> deviceIds) {
        log.debug("Try to find devices by tenantId [{}] and device Ids [{}]", tenantId, deviceIds);
        Select select = select().from(getColumnFamilyName());
        Select.Where query = select.where();
        query.and(eq(DEVICE_TENANT_ID_PROPERTY, tenantId));
        query.and(in(ID_PROPERTY, deviceIds));
        return findListByStatementAsync(query);
    }

    @Override
    public List<Device> findDevicesByTenantIdAndCustomerId(UUID tenantId, UUID customerId, TextPageLink pageLink) {
        log.debug("Try to find devices by tenantId [{}], customerId[{}] and pageLink [{}]", tenantId, customerId, pageLink);
        List<DeviceEntity> deviceEntities = findPageWithTextSearch(DEVICE_BY_CUSTOMER_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(DEVICE_CUSTOMER_ID_PROPERTY, customerId),
                        eq(DEVICE_TENANT_ID_PROPERTY, tenantId)),
                pageLink);

        log.trace("Found devices [{}] by tenantId [{}], customerId [{}] and pageLink [{}]", deviceEntities, tenantId, customerId, pageLink);
        return DaoUtil.convertDataList(deviceEntities);
    }

    @Override
    public List<Device> findDevicesByTenantIdAndCustomerIdAndType(UUID tenantId, UUID customerId, String type, TextPageLink pageLink) {
        log.debug("Try to find devices by tenantId [{}], customerId [{}], type [{}] and pageLink [{}]", tenantId, customerId, type, pageLink);
        List<DeviceEntity> deviceEntities = findPageWithTextSearch(DEVICE_BY_CUSTOMER_BY_TYPE_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME,
                Arrays.asList(eq(DEVICE_TYPE_PROPERTY, type),
                        eq(DEVICE_CUSTOMER_ID_PROPERTY, customerId),
                        eq(DEVICE_TENANT_ID_PROPERTY, tenantId)),
                pageLink);

        log.trace("Found devices [{}] by tenantId [{}], customerId [{}], type [{}] and pageLink [{}]", deviceEntities, tenantId, customerId, type, pageLink);
        return DaoUtil.convertDataList(deviceEntities);
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdCustomerIdAndIdsAsync(UUID tenantId, UUID customerId, List<UUID> deviceIds) {
        log.debug("Try to find devices by tenantId [{}], customerId [{}] and device Ids [{}]", tenantId, customerId, deviceIds);
        Select select = select().from(getColumnFamilyName());
        Select.Where query = select.where();
        query.and(eq(DEVICE_TENANT_ID_PROPERTY, tenantId));
        query.and(eq(DEVICE_CUSTOMER_ID_PROPERTY, customerId));
        query.and(in(ID_PROPERTY, deviceIds));
        return findListByStatementAsync(query);
    }

    @Override
    public Optional<Device> findDeviceByTenantIdAndName(UUID tenantId, String deviceName) {
        Select select = select().from(DEVICE_BY_TENANT_AND_NAME_VIEW_NAME);
        Select.Where query = select.where();
        query.and(eq(DEVICE_TENANT_ID_PROPERTY, tenantId));
        query.and(eq(DEVICE_NAME_PROPERTY, deviceName));
        return Optional.ofNullable(DaoUtil.getData(findOneByStatement(query)));
    }

    @Override
    public ListenableFuture<List<EntitySubtype>> findTenantDeviceTypesAsync(UUID tenantId) {
        Select select = select().from(ENTITY_SUBTYPE_COLUMN_FAMILY_NAME);
        Select.Where query = select.where();
        query.and(eq(ENTITY_SUBTYPE_TENANT_ID_PROPERTY, tenantId));
        query.and(eq(ENTITY_SUBTYPE_ENTITY_TYPE_PROPERTY, EntityType.DEVICE));
        query.setConsistencyLevel(cluster.getDefaultReadConsistencyLevel());
        ResultSetFuture resultSetFuture = executeAsyncRead(query);
        return Futures.transform(resultSetFuture, new Function<ResultSet, List<EntitySubtype>>() {
            @Nullable
            @Override
            public List<EntitySubtype> apply(@Nullable ResultSet resultSet) {
                Result<EntitySubtypeEntity> result = cluster.getMapper(EntitySubtypeEntity.class).map(resultSet);
                if (result != null) {
                    List<EntitySubtype> entitySubtypes = new ArrayList<>();
                    result.all().forEach((entitySubtypeEntity) ->
                            entitySubtypes.add(entitySubtypeEntity.toEntitySubtype())
                    );
                    return entitySubtypes;
                } else {
                    return Collections.emptyList();
                }
            }
        });
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdAndStatus(UUID tenantId, DeviceStatusQuery statusQuery) {
        log.debug("Try to find [{}] devices by tenantId [{}]", statusQuery.getStatus(), tenantId);

        Select select = select().from(DEVICE_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME).allowFiltering();
        Select.Where query = select.where();
        query.and(eq(DEVICE_TENANT_ID_PROPERTY, tenantId));
        Clause clause = statusClause(statusQuery);
        query.and(clause);
        return findListByStatementAsync(query);
    }

    @Override
    public ListenableFuture<List<Device>> findDevicesByTenantIdTypeAndStatus(UUID tenantId, String type, DeviceStatusQuery statusQuery) {
        log.debug("Try to find [{}] devices by tenantId [{}] and type [{}]", statusQuery.getStatus(), tenantId, type);

        Select select = select().from(DEVICE_BY_TENANT_BY_TYPE_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME).allowFiltering();
        Select.Where query = select.where()
                .and(eq(DEVICE_TENANT_ID_PROPERTY, tenantId))
                .and(eq(DEVICE_TYPE_PROPERTY, type));

        query.and(statusClause(statusQuery));
        return findListByStatementAsync(query);
    }


    @Override
    public void saveDeviceStatus(Device device) {
        PreparedStatement statement = prepare("insert into " +
                "device (id, tenant_id, customer_id, type, last_connect, last_update) values (?, ?, ?, ?, ?, ?)");
        BoundStatement boundStatement = statement.bind(device.getUuidId(), device.getTenantId().getId(), device.getCustomerId().getId(),
                device.getType(), device.getLastConnectTs(), device.getLastUpdateTs());
        ResultSetFuture resultSetFuture = executeAsyncWrite(boundStatement);
        Futures.withFallback(resultSetFuture, t -> {
            log.error("Can't update device status for [{}]", device, t);
            throw new IllegalArgumentException("Can't update device status for {" + device + "}", t);
        });
    }

    private String getStatusProperty(DeviceStatusQuery statusQuery) {
        switch (statusQuery.getContactType()) {
            case UPLOAD:
                return DEVICE_LAST_UPDATE_PROPERTY;
            case CONNECT:
                return DEVICE_LAST_CONNECT_PROPERTY;
        }
        return null;
    }

    private Clause statusClause(DeviceStatusQuery statusQuery) {
        long minTime = System.currentTimeMillis() - statusQuery.getThreshold();
        String statusProperty = getStatusProperty(statusQuery);
        if (statusProperty != null) {
            switch (statusQuery.getStatus()) {
                case ONLINE:
                    return gt(statusProperty, minTime);
                case OFFLINE:
                    return lt(statusProperty, minTime);
            }
        }
        log.error("Could not build status query from [{}]", statusQuery);
        throw new IllegalStateException("Could not build status query for device []");
    }

}
