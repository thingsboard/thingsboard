/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.edqs.repo;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edqs.EdqsObject;
import org.thingsboard.server.common.data.edqs.query.QueryResult;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.StringFilterPredicate;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.stats.DummyEdqsStatsService;
import org.thingsboard.server.edqs.util.DefaultEdqsMapper;
import org.thingsboard.server.edqs.util.EdqsMapper;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RunWith(SpringRunner.class)
@Configuration
@ComponentScan({"org.thingsboard.server.edqs.repo", "org.thingsboard.server.edqs.util"})
@EntityScan("org.thingsboard.server.edqs")
@TestPropertySource(locations = {"classpath:edqs-test.properties"})
@TestExecutionListeners({
        DependencyInjectionTestExecutionListener.class,
        DirtiesContextTestExecutionListener.class})
public abstract class AbstractEDQTest {

    @Autowired
    protected DefaultEdqsRepository repository;
    @Autowired
    protected EdqsMapper edqsMapper;
    @MockBean
    private DummyEdqsStatsService edqsStatsService;

    protected final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    protected final CustomerId customerId = new CustomerId(UUID.randomUUID());

    protected final UUID defaultAssetProfileId = UUID.randomUUID();
    protected final UUID defaultDeviceProfileId = UUID.randomUUID();

    @Before
    public final void before() {
        AssetProfile ap = new AssetProfile(new AssetProfileId(defaultAssetProfileId));
        ap.setName("default");
        ap.setDefault(true);
        addOrUpdate(EntityType.ASSET_PROFILE, ap);

        DeviceProfile dp = new DeviceProfile(new DeviceProfileId(defaultDeviceProfileId));
        dp.setName("default");
        dp.setDefault(true);
        dp.setType(DeviceProfileType.DEFAULT);
        addOrUpdate(EntityType.DEVICE_PROFILE, dp);

        createCustomer(customerId.getId(), null, "Customer A");
    }

    @After
    public final void after() {
        repository.clear();
    }

    protected void createCustomer(UUID id, UUID parentCustomerId, String title) {
        Customer entity = new Customer();
        entity.setId(new CustomerId(id));
        entity.setTitle(title);
        addOrUpdate(EntityType.CUSTOMER, entity);
    }


    protected UUID createDevice(String name) {
        return createDevice(null, defaultDeviceProfileId, name);
    }

    protected UUID createDevice(CustomerId customerId, String name) {
        return createDevice(customerId.getId(), defaultDeviceProfileId, name);
    }

    protected UUID createDevice(UUID customerId, UUID profileId, String name) {
        UUID entityId = UUID.randomUUID();
        Device entity = new Device();
        entity.setId(new DeviceId(entityId));
        if (profileId != null) {
            entity.setDeviceProfileId(new DeviceProfileId(profileId));
        }
        if (customerId != null) {
            entity.setCustomerId(new CustomerId(customerId));
        }
        entity.setName(name);
        addOrUpdate(EntityType.DEVICE, entity);
        return entityId;
    }

    protected UUID createDashboard(String name) {
        UUID entityId = UUID.randomUUID();
        Dashboard entity = new Dashboard();
        entity.setId(new DashboardId(entityId));
        entity.setTitle(name);
        addOrUpdate(EntityType.DEVICE, entity);
        return entityId;
    }

    protected UUID createView(String name) {
        return createView(null, "default", name);
    }

    protected UUID createView(CustomerId customerId, String name) {
        return createView(customerId.getId(), "default", name);
    }

    protected UUID createView(UUID customerId, String type, String name) {
        UUID entityId = UUID.randomUUID();
        EntityView entity = new EntityView();
        entity.setId(new EntityViewId(entityId));
        entity.setType(type);
        if (customerId != null) {
            entity.setCustomerId(new CustomerId(customerId));
        }
        entity.setName(name);
        addOrUpdate(EntityType.ENTITY_VIEW, entity);
        return entityId;
    }

    protected UUID createEdge(String name) {
        return createEdge(null, "default", name);
    }

    protected UUID createEdge(CustomerId customerId, String name) {
        return createEdge(customerId.getId(), "default", name);
    }

    protected UUID createEdge(UUID customerId, String type, String name) {
        UUID id = UUID.randomUUID();
        Edge edge = new Edge();
        edge.setId(new EdgeId(id));
        edge.setTenantId(tenantId);
        if (customerId != null) {
            edge.setCustomerId(new CustomerId(customerId));
        }
        edge.setType(type);
        edge.setName(name);
        edge.setCreatedTime(42L);
        addOrUpdate(EntityType.EDGE, edge);
        return id;
    }


    protected UUID createAsset(String name) {
        return createAsset(null, defaultAssetProfileId, name);
    }

    protected UUID createAsset(UUID customerId, String name) {
        return createAsset(customerId, defaultAssetProfileId, name);
    }

    protected UUID createAsset(UUID customerId, UUID profileId, String name) {
        UUID entityId = UUID.randomUUID();
        Asset entity = new Asset();
        entity.setId(new AssetId(entityId));
        if (profileId != null) {
            entity.setAssetProfileId(new AssetProfileId(profileId));
        }
        if (customerId != null) {
            entity.setCustomerId(new CustomerId(customerId));
        }
        entity.setName(name);
        addOrUpdate(EntityType.ASSET, entity);
        return entityId;
    }

    protected void createRelation(EntityType fromType, UUID fromId, EntityType toType, UUID toId, String type) {
        createRelation(fromType, fromId, toType, toId, RelationTypeGroup.COMMON, type);
    }

    protected void createRelation(EntityType fromType, UUID fromId, EntityType toType, UUID toId, RelationTypeGroup group, String type) {
        addOrUpdate(new EntityRelation(EntityIdFactory.getByTypeAndUuid(fromType, fromId), EntityIdFactory.getByTypeAndUuid(toType, toId), type, group));
    }


    protected boolean checkContains(PageData<QueryResult> data, UUID entityId) {
        return data.getData().stream().anyMatch(r -> r.getEntityId().getId().equals(entityId));
    }

    protected List<KeyFilter> createStringKeyFilters(String key, EntityKeyType keyType, StringFilterPredicate.StringOperation operation, String value) {
        KeyFilter filter = new KeyFilter();
        filter.setKey(new EntityKey(keyType, key));
        filter.setValueType(EntityKeyValueType.STRING);
        StringFilterPredicate predicate = new StringFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromString(value));
        predicate.setOperation(operation);
        predicate.setIgnoreCase(true);
        filter.setPredicate(predicate);
        return Collections.singletonList(filter);
    }

    protected void addOrUpdate(EntityType entityType, Object entity) {
        addOrUpdate(DefaultEdqsMapper.toEntity(entityType, entity));
    }

    protected void addOrUpdate(EdqsObject edqsObject) {
        byte[] serialized = edqsMapper.serialize(edqsObject);
        edqsObject = edqsMapper.deserialize(edqsObject.type(), serialized, false);
        repository.get(tenantId).addOrUpdate(edqsObject);
    }

}
