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
package org.thingsboard.server.service.entitiy;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.RelationsQueryFilter;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.msg.edqs.EdqsService;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.edqs.util.EdqsRocksDb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;

@DaoSqlTest
@TestPropertySource(properties = {
        "queue.edqs.sync.enabled=true",
        "queue.edqs.api.supported=true",
        "queue.edqs.api.auto_enable=true",
        "queue.edqs.mode=local",
        "queue.edqs.readiness_check_interval=1000"
})
public class EdqsEntityServiceTest extends EntityServiceTest {

    @Autowired
    private EdqsService edqsService;

    @MockitoBean
    private EdqsRocksDb edqsRocksDb;

    @Before
    public void beforeEach() {
        await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> edqsService.isApiEnabled());
    }

    // sql implementation has a bug with data duplication, edqs implementation returns correct value
    @Override
    @Test
    public void testCountHierarchicalEntitiesByMultiRootQuery() throws InterruptedException {
        List<Asset> buildings = new ArrayList<>();
        List<Asset> apartments = new ArrayList<>();
        Map<String, Map<UUID, String>> entityNameByTypeMap = new HashMap<>();
        Map<UUID, UUID> childParentRelationMap = new HashMap<>();
        createMultiRootHierarchy(buildings, apartments, entityNameByTypeMap, childParentRelationMap);

        RelationsQueryFilter filter = new RelationsQueryFilter();
        filter.setMultiRoot(true);
        filter.setMultiRootEntitiesType(EntityType.ASSET);
        filter.setMultiRootEntityIds(buildings.stream().map(IdBased::getId).map(d -> d.getId().toString()).collect(Collectors.toSet()));
        filter.setDirection(EntitySearchDirection.FROM);

        EntityCountQuery countQuery = new EntityCountQuery(filter);
        countByQueryAndCheck(countQuery, 63);

        filter.setFilters(Collections.singletonList(new RelationEntityTypeFilter("AptToHeat", Collections.singletonList(EntityType.DEVICE))));
        countByQueryAndCheck(countQuery, 27);

        filter.setMultiRootEntitiesType(EntityType.ASSET);
        filter.setMultiRootEntityIds(apartments.stream().map(IdBased::getId).map(d -> d.getId().toString()).collect(Collectors.toSet()));
        filter.setDirection(EntitySearchDirection.TO);
        filter.setFilters(Lists.newArrayList(
                new RelationEntityTypeFilter("buildingToApt", Collections.singletonList(EntityType.ASSET)),
                new RelationEntityTypeFilter("AptToEnergy", Collections.singletonList(EntityType.DEVICE))));
        countByQueryAndCheck(countQuery, 3);

        deviceService.deleteDevicesByTenantId(tenantId);
        assetService.deleteAssetsByTenantId(tenantId);
    }

    @Override
    protected PageData<EntityData> findByQueryAndCheck(CustomerId customerId, EntityDataQuery query, long expectedResultSize) {
        return await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> findByQuery(customerId, query),
                result -> result.getTotalElements() == expectedResultSize);
    }

    @Override
    protected List<EntityData> findByQueryAndCheckTelemetry(EntityDataQuery query, EntityKeyType entityKeyType, String key, List<String> expectedTelemetries) {
        return await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> loadAllData(query, expectedTelemetries.size()),
                loadedEntities -> loadedEntities.stream().map(entityData -> entityData.getLatest().get(entityKeyType).get(key).getValue()).toList().containsAll(expectedTelemetries));
    }

    @Override
    protected long countByQueryAndCheck(EntityCountQuery countQuery, int expectedResult) {
        return countByQueryAndCheck(new CustomerId(CustomerId.NULL_UUID), countQuery, expectedResult);
    }

    @Override
    protected long countByQueryAndCheck(CustomerId customerId, EntityCountQuery query, int expectedResult) {
        return await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> countByQuery(customerId, query),
                result -> result == expectedResult);
    }

}
