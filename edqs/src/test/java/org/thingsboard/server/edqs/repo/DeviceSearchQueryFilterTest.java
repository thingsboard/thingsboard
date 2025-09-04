/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.query.QueryResult;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AliasEntityId;
import org.thingsboard.server.common.data.query.DeviceSearchQueryFilter;
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

public class DeviceSearchQueryFilterTest extends AbstractEDQTest {
    private final DeviceProfileId deviceProfileId = new DeviceProfileId(UUID.randomUUID());

    @Before
    public void setUp() {
    }

    @Test
    public void testFindTenantDevices() {
        DeviceProfile deviceProfile = new DeviceProfile(deviceProfileId);
        deviceProfile.setName("thermostat");
        deviceProfile.setDefault(false);
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        addOrUpdate(EntityType.DEVICE_PROFILE, deviceProfile);

        UUID asset1 = createAsset("A1");
        UUID asset2 = createAsset("A2");
        UUID device1 = createDevice(null, deviceProfileId.getId(), "D1");
        UUID device2 = createDevice(null, deviceProfileId.getId(), "D2");

        createRelation(EntityType.ASSET, asset1, EntityType.DEVICE, device1, "Contains");
        createRelation(EntityType.ASSET, asset1, EntityType.ASSET, asset2, "Contains");
        createRelation(EntityType.ASSET, asset2, EntityType.DEVICE, device2, "Contains");

        // find all devices of asset A1
        PageData<QueryResult> relationsResult = findData(null, new AssetId(asset1),
                EntitySearchDirection.FROM, "Contains", 2, false, Arrays.asList("thermostat"));
        Assert.assertEquals(2, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, device1));
        Assert.assertTrue(checkContains(relationsResult, device2));

        // find all devices with max level = 1
        relationsResult = findData(null, new AssetId(asset1),
                EntitySearchDirection.FROM, "Contains", 1, false, Arrays.asList("thermostat"));
        Assert.assertEquals(1, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, device1));

        // find all devices with asset type = default
        relationsResult = findData(null, new AssetId(asset1),
                EntitySearchDirection.FROM, "Contains", 1, false, Arrays.asList("default"));
        Assert.assertEquals(0, relationsResult.getData().size());

        // find all devices last level only, level = 2
        relationsResult = findData(null, new AssetId(asset1),
                EntitySearchDirection.FROM, "Contains", 2, true, Arrays.asList("thermostat"));
        Assert.assertEquals(2, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, device2));
        Assert.assertTrue(checkContains(relationsResult, device1));

        // find all devices last level only, level = 1
        relationsResult = findData(null, new AssetId(asset1),
                EntitySearchDirection.FROM, "Contains", 1, true, Arrays.asList("thermostat"));
        Assert.assertEquals(1, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, device1));
    }

    @Test
    public void testFindCustomerDevices() {
        DeviceProfile deviceProfile = new DeviceProfile(deviceProfileId);
        deviceProfile.setName("thermostat");
        deviceProfile.setDefault(false);
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        addOrUpdate(EntityType.DEVICE_PROFILE, deviceProfile);

        UUID asset1 = createAsset(customerId.getId(), defaultAssetProfileId, "A1");
        UUID asset2 = createAsset(customerId.getId(), defaultAssetProfileId, "A2");
        UUID device1 = createDevice(customerId.getId(), deviceProfileId.getId(), "D1");
        UUID device2 = createDevice(customerId.getId(), defaultDeviceProfileId, "D2");

        createRelation(EntityType.ASSET, asset1, EntityType.ASSET, asset2, "Contains");
        createRelation(EntityType.ASSET, asset1, EntityType.DEVICE, device1, "Contains");
        createRelation(EntityType.ASSET, asset2, EntityType.DEVICE, device2, "Contains");

        // find all devices of type "thermostat"
        PageData<QueryResult> relationsResult = findData(customerId, new AssetId(asset1),
                EntitySearchDirection.FROM, "Contains", 2, false, Arrays.asList("thermostat"));
        Assert.assertEquals(1, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, device1));

        // find all assets of root asset with profile "Office" and "default"
        relationsResult = findData(customerId, new AssetId(asset1),
                EntitySearchDirection.FROM, "Contains", 2, false, Arrays.asList("thermostat", "default"));
        Assert.assertEquals(2, relationsResult.getData().size());
        Assert.assertTrue(checkContains(relationsResult, device1));
        Assert.assertTrue(checkContains(relationsResult, device2));

        // find all assets with other customer
        relationsResult = findData(new CustomerId(UUID.randomUUID()), new AssetId(asset1),
                EntitySearchDirection.FROM, "Contains", 2, false, Arrays.asList("thermostat"));
        Assert.assertEquals(0, relationsResult.getData().size());
    }

    private PageData<QueryResult> findData(CustomerId customerId, EntityId rootId,
                                           EntitySearchDirection direction, String relationType, int maxLevel, boolean lastLevelOnly, List<String> deviceTypes) {
        DeviceSearchQueryFilter filter = new DeviceSearchQueryFilter();
        filter.setRootEntity(AliasEntityId.fromEntityId(rootId));
        filter.setDirection(direction);
        filter.setRelationType(relationType);
        filter.setDeviceTypes(deviceTypes);
        filter.setFetchLastLevelOnly(lastLevelOnly);
        filter.setMaxLevel(maxLevel);
        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, null);
        List<KeyFilter> keyFiltersEqualString = createStringKeyFilters("name", EntityKeyType.ENTITY_FIELD, StringFilterPredicate.StringOperation.STARTS_WITH, "D");
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, Collections.emptyList(), Collections.emptyList(), keyFiltersEqualString);
        return repository.findEntityDataByQuery(tenantId, customerId, query, false);
    }

}
