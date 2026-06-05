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
package org.thingsboard.server.client;

import org.junit.Test;
import org.thingsboard.client.model.AliasEntityId;
import org.thingsboard.client.model.Asset;
import org.thingsboard.client.model.AssetTypeFilter;
import org.thingsboard.client.model.Device;
import org.thingsboard.client.model.DeviceTypeFilter;
import org.thingsboard.client.model.Direction;
import org.thingsboard.client.model.EntityData;
import org.thingsboard.client.model.EntityDataPageLink;
import org.thingsboard.client.model.EntityDataQuery;
import org.thingsboard.client.model.EntityDataSortOrder;
import org.thingsboard.client.model.EntityKey;
import org.thingsboard.client.model.EntityKeyType;
import org.thingsboard.client.model.EntityKeyValueType;
import org.thingsboard.client.model.EntityListFilter;
import org.thingsboard.client.model.EntityNameFilter;
import org.thingsboard.client.model.EntityType;
import org.thingsboard.client.model.FilterPredicateValueString;
import org.thingsboard.client.model.KeyFilter;
import org.thingsboard.client.model.PageDataEntityData;
import org.thingsboard.client.model.SingleEntityFilter;
import org.thingsboard.client.model.StringFilterPredicate;
import org.thingsboard.client.model.StringOperation;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@DaoSqlTest
public class EntityQueryApiClientTest extends AbstractApiClientTest {

    private static final String QUERY_TEST_PREFIX = "QueryTest_";

    private EntityDataPageLink pageLink(int pageSize) {
        return new EntityDataPageLink()
                .pageSize(pageSize)
                .page(0)
                .sortOrder(new EntityDataSortOrder()
                        .key(new EntityKey().type(EntityKeyType.ENTITY_FIELD).key("name"))
                        .direction(Direction.ASC));
    }

    @Test
    public void testFindByDeviceTypeFilter() throws Exception {
        long ts = System.currentTimeMillis();
        String type1 = "temperatureSensor";
        String type2 = "humiditySensor";

        for (int i = 0; i < 3; i++) {
            Device d = new Device();
            d.setName(QUERY_TEST_PREFIX + "temp_" + ts + "_" + i);
            d.setType(type1);
            client.saveDevice(d, null, null, null, null);
        }
        for (int i = 0; i < 2; i++) {
            Device d = new Device();
            d.setName(QUERY_TEST_PREFIX + "hum_" + ts + "_" + i);
            d.setType(type2);
            client.saveDevice(d, null, null, null, null);
        }

        // filter by single device type
        EntityDataQuery singleTypeQuery = new EntityDataQuery()
                .entityFilter(new DeviceTypeFilter()
                        .deviceTypes(List.of(type1)))
                .pageLink(pageLink(10))
                .addEntityFieldsItem(new EntityKey().type(EntityKeyType.ENTITY_FIELD).key("name"));

        PageDataEntityData result = client.findEntityDataByQuery(singleTypeQuery);
        assertNotNull(result);
        assertEquals(3, result.getTotalElements().intValue());
        for (EntityData entity : result.getData()) {
            assertNotNull(entity.getEntityId());
        }

        // filter by multiple device types
        EntityDataQuery multiTypeQuery = new EntityDataQuery()
                .entityFilter(new DeviceTypeFilter()
                        .deviceTypes(List.of(type1, type2)))
                .pageLink(pageLink(10))
                .addEntityFieldsItem(new EntityKey().type(EntityKeyType.ENTITY_FIELD).key("name"));

        PageDataEntityData multiResult = client.findEntityDataByQuery(multiTypeQuery);
        assertNotNull(multiResult);
        assertEquals(5, multiResult.getTotalElements().intValue());

        // filter by device type + name filter
        EntityDataQuery nameFilterQuery = new EntityDataQuery()
                .entityFilter(new DeviceTypeFilter()
                        .deviceTypes(List.of(type1, type2))
                        .deviceNameFilter(QUERY_TEST_PREFIX + "temp_" + ts))
                .pageLink(pageLink(10))
                .addEntityFieldsItem(new EntityKey().type(EntityKeyType.ENTITY_FIELD).key("name"));

        PageDataEntityData nameResult = client.findEntityDataByQuery(nameFilterQuery);
        assertNotNull(nameResult);
        assertEquals(3, nameResult.getTotalElements().intValue());
    }

    @Test
    public void testFindByEntityNameFilter() throws Exception {
        long ts = System.currentTimeMillis();
        String prefix = QUERY_TEST_PREFIX + "named_" + ts;

        for (int i = 0; i < 4; i++) {
            Device d = new Device();
            d.setName(prefix + "_" + i);
            d.setType("default");
            client.saveDevice(d, null, null, null, null);
        }

        EntityDataQuery query = new EntityDataQuery()
                .entityFilter(new EntityNameFilter()
                        .entityType(EntityType.DEVICE)
                        .entityNameFilter(prefix))
                .pageLink(pageLink(10))
                .addEntityFieldsItem(new EntityKey().type(EntityKeyType.ENTITY_FIELD).key("name"));

        PageDataEntityData result = client.findEntityDataByQuery(query);
        assertNotNull(result);
        assertEquals(4, result.getTotalElements().intValue());
        assertFalse(result.getHasNext());
    }

    @Test
    public void testFindByEntityListFilter() throws Exception {
        long ts = System.currentTimeMillis();

        Device d1 = client.saveDevice(new Device().name(QUERY_TEST_PREFIX + "list_" + ts + "_1").type("default"), null, null, null, null);
        Device d2 = client.saveDevice(new Device().name(QUERY_TEST_PREFIX + "list_" + ts + "_2").type("default"), null, null, null, null);
        client.saveDevice(new Device().name(QUERY_TEST_PREFIX + "list_" + ts + "_3").type("default"), null, null, null, null);

        EntityDataQuery query = new EntityDataQuery()
                .entityFilter(new EntityListFilter()
                        .entityType(EntityType.DEVICE)
                        .entityList(List.of(
                                d1.getId().getId().toString(),
                                d2.getId().getId().toString())))
                .pageLink(pageLink(10))
                .addEntityFieldsItem(new EntityKey().type(EntityKeyType.ENTITY_FIELD).key("name"));

        PageDataEntityData result = client.findEntityDataByQuery(query);
        assertNotNull(result);
        assertEquals(2, result.getTotalElements().intValue());

        List<String> returnedIds = result.getData().stream()
                .map(e -> e.getEntityId().getId().toString())
                .collect(Collectors.toList());
        assertTrue(returnedIds.contains(d1.getId().getId().toString()));
        assertTrue(returnedIds.contains(d2.getId().getId().toString()));
    }

    @Test
    public void testFindBySingleEntityFilter() throws Exception {
        long ts = System.currentTimeMillis();
        Device device = client.saveDevice(new Device().name(QUERY_TEST_PREFIX + "single_" + ts).type("default"), null, null, null, null);

        EntityDataQuery query = new EntityDataQuery()
                .entityFilter(new SingleEntityFilter()
                        .singleEntity(new AliasEntityId()
                                .id(device.getId().getId())
                                .entityType(EntityType.DEVICE)))
                .pageLink(pageLink(10))
                .addEntityFieldsItem(new EntityKey().type(EntityKeyType.ENTITY_FIELD).key("name"));

        PageDataEntityData result = client.findEntityDataByQuery(query);
        assertNotNull(result);
        assertEquals(1, result.getTotalElements().intValue());
        assertEquals(device.getId().getId().toString(),
                result.getData().get(0).getEntityId().getId().toString());
    }

    @Test
    public void testFindByAssetTypeFilter() throws Exception {
        long ts = System.currentTimeMillis();
        String assetType = "building";

        for (int i = 0; i < 3; i++) {
            Asset a = new Asset();
            a.setName(QUERY_TEST_PREFIX + "asset_" + ts + "_" + i);
            a.setType(assetType);
            client.saveAsset(a, null, null, null);
        }

        EntityDataQuery query = new EntityDataQuery()
                .entityFilter(new AssetTypeFilter()
                        .assetTypes(List.of(assetType)))
                .pageLink(pageLink(10))
                .addEntityFieldsItem(new EntityKey().type(EntityKeyType.ENTITY_FIELD).key("name"));

        PageDataEntityData result = client.findEntityDataByQuery(query);
        assertNotNull(result);
        assertEquals(3, result.getTotalElements().intValue());
    }

    @Test
    public void testFindWithKeyFilter() throws Exception {
        long ts = System.currentTimeMillis();
        String matchName = QUERY_TEST_PREFIX + "kf_match_" + ts;
        String noMatchName = QUERY_TEST_PREFIX + "kf_other_" + ts;

        client.saveDevice(new Device().name(matchName).type("default"), null, null, null, null);
        client.saveDevice(new Device().name(noMatchName).type("default"), null, null, null, null);

        KeyFilter nameKeyFilter = new KeyFilter()
                .key(new EntityKey().type(EntityKeyType.ENTITY_FIELD).key("name"))
                .valueType(EntityKeyValueType.STRING)
                .predicate(new StringFilterPredicate()
                        .operation(StringOperation.CONTAINS)
                        .value(new FilterPredicateValueString().defaultValue("kf_match"))
                        .ignoreCase(true));

        EntityDataQuery query = new EntityDataQuery()
                .entityFilter(new EntityNameFilter()
                        .entityType(EntityType.DEVICE)
                        .entityNameFilter(QUERY_TEST_PREFIX + "kf_"))
                .addKeyFiltersItem(nameKeyFilter)
                .pageLink(pageLink(10))
                .addEntityFieldsItem(new EntityKey().type(EntityKeyType.ENTITY_FIELD).key("name"));

        PageDataEntityData result = client.findEntityDataByQuery(query);
        assertNotNull(result);
        assertEquals(1, result.getTotalElements().intValue());
    }

    @Test
    public void testFindWithPagination() throws Exception {
        long ts = System.currentTimeMillis();

        for (int i = 0; i < 5; i++) {
            Device d = new Device();
            d.setName(QUERY_TEST_PREFIX + "page_" + ts + "_" + i);
            d.setType("default");
            client.saveDevice(d, null, null, null, null);
        }

        EntityDataPageLink smallPage = new EntityDataPageLink()
                .pageSize(2)
                .page(0)
                .sortOrder(new EntityDataSortOrder()
                        .key(new EntityKey().type(EntityKeyType.ENTITY_FIELD).key("name"))
                        .direction(Direction.ASC));

        EntityDataQuery query = new EntityDataQuery()
                .entityFilter(new EntityNameFilter()
                        .entityType(EntityType.DEVICE)
                        .entityNameFilter(QUERY_TEST_PREFIX + "page_" + ts))
                .pageLink(smallPage)
                .addEntityFieldsItem(new EntityKey().type(EntityKeyType.ENTITY_FIELD).key("name"));

        // first page
        PageDataEntityData page1 = client.findEntityDataByQuery(query);
        assertNotNull(page1);
        assertEquals(5, page1.getTotalElements().intValue());
        assertEquals(3, page1.getTotalPages().intValue());
        assertEquals(2, page1.getData().size());
        assertTrue(page1.getHasNext());

        // last page
        smallPage.setPage(2);
        PageDataEntityData lastPage = client.findEntityDataByQuery(query);
        assertNotNull(lastPage);
        assertEquals(1, lastPage.getData().size());
        assertFalse(lastPage.getHasNext());
    }

}
