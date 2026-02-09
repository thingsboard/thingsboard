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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.query.QueryResult;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AliasEntityId;
import org.thingsboard.server.common.data.query.EdgeSearchQueryFilter;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.StringFilterPredicate;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class EdgeSearchQueryFilterTest extends AbstractEDQTest {

    @Before
    public void setUp() {
    }

    @Test
    public void testFindDevicesManagesByTenant() {
        UUID edge1 = createEdge("E1");
        UUID edge2 = createEdge("E2");
        UUID device1 = createDevice("D1");
        UUID device2 = createDevice("D2");
        UUID device3 = createDevice("D3");

        createRelation(EntityType.EDGE, edge1, EntityType.DEVICE, device1, "Manages");
        createRelation(EntityType.EDGE, edge2, EntityType.DEVICE, device2, "Manages");
        createRelation(EntityType.EDGE, edge2, EntityType.DEVICE, device3, "Manages");

        // find devices managed by edge
        PageData<QueryResult> relationsResult = findData(null, new DeviceId(device1),
                EntitySearchDirection.TO, "Manages", 2, false, Arrays.asList("default"));
        Assert.assertEquals(1, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, edge1));

        // find devices managed by edge with non-existing type
        relationsResult = findData(null, new DeviceId(device1),
                EntitySearchDirection.TO, "Manages", 1, false, Arrays.asList("non-existing type"));
        Assert.assertEquals(0, relationsResult.getData().size());

        // find all entity views last level only, level = 2
        relationsResult = findData(null, new DeviceId(device1),
                EntitySearchDirection.TO, "Manages", 2, true, Arrays.asList("default"));
        Assert.assertEquals(1, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, edge1));
    }

    @Test
    public void testFindCustomerEdges() {
        UUID edge1 = createEdge(customerId, "E1");
        UUID edge2 = createEdge(customerId, "E2");
        createRelation(EntityType.CUSTOMER, customerId.getId(), EntityType.EDGE, edge1, "Manages");
        createRelation(EntityType.CUSTOMER, customerId.getId(), EntityType.EDGE, edge2, "Manages");

        // find all edges managed by customer
        PageData<QueryResult> relationsResult = findData(customerId, customerId,
                EntitySearchDirection.FROM, "Manages", 2, false, Arrays.asList("default"));
        Assert.assertEquals(2, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, edge1));
        Assert.assertTrue(checkContains(relationsResult, edge2));

        // find all edges managed by customer with non-existing type
        relationsResult = findData(customerId, customerId,
                EntitySearchDirection.FROM, "Manages", 2, false, Arrays.asList("non existing"));
        Assert.assertEquals(0, relationsResult.getData().size());

        // find all entity views with other customer
        relationsResult = findData(new CustomerId(UUID.randomUUID()), customerId,
                EntitySearchDirection.FROM, "Manages", 2, false, Arrays.asList("default"));
        Assert.assertEquals(0, relationsResult.getData().size());
    }

    private PageData<QueryResult> findData(CustomerId customerId, EntityId rootId,
                                           EntitySearchDirection direction, String relationType, int maxLevel, boolean lastLevelOnly, List<String> edgeTypes) {
        EdgeSearchQueryFilter filter = new EdgeSearchQueryFilter();
        filter.setRootEntity(AliasEntityId.fromEntityId(rootId));
        filter.setDirection(direction);
        filter.setRelationType(relationType);
        filter.setEdgeTypes(edgeTypes);
        filter.setFetchLastLevelOnly(lastLevelOnly);
        filter.setMaxLevel(maxLevel);
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, null);
        List<KeyFilter> keyFiltersEqualString = createStringKeyFilters("name", EntityKeyType.ENTITY_FIELD, StringFilterPredicate.StringOperation.STARTS_WITH, "E");
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, Collections.emptyList(), Collections.emptyList(), keyFiltersEqualString);
        return repository.findEntityDataByQuery(tenantId, customerId, query, false);
    }

}
