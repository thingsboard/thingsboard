/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.dao.service;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleEntityState;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class BaseAlarmRuleEntityStateServiceTest extends AbstractServiceTest {

    private TenantId tenantId;
    private Device deviceEntity;
    private Asset assetEntity;

    @Before
    public void before() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();

        Device device = new Device();
        device.setTenantId(tenantId);
        device.setName(StringUtils.randomAlphabetic(5));
        device.setType("default");
        deviceEntity = deviceService.saveDevice(device);

        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName(StringUtils.randomAlphabetic(5));
        asset.setType("default");
        assetEntity = assetService.saveAsset(asset);
    }

    @After
    public void after() {
        alarmRuleEntityStateService.deleteByEntityId(tenantId, deviceEntity.getId());
        alarmRuleEntityStateService.deleteByEntityId(tenantId, assetEntity.getId());
        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testSaveAlarmRuleEntityState() {
        AlarmRuleEntityState alarmRuleEntityState = createAlarmRuleEntityState(tenantId, deviceEntity.getId(), "test data");
        AlarmRuleEntityState ruleEntityState = alarmRuleEntityStateService.save(tenantId, alarmRuleEntityState);
        Assert.assertEquals(ruleEntityState.getData(), alarmRuleEntityState.getData());
        Assert.assertEquals(ruleEntityState.getTenantId(), alarmRuleEntityState.getTenantId());
        Assert.assertEquals(ruleEntityState.getEntityId(), alarmRuleEntityState.getEntityId());

        List<AlarmRuleEntityState> allByIds = alarmRuleEntityStateService.findAllByIds(Collections.singletonList(deviceEntity.getId()));
        Assert.assertEquals(allByIds.size(), 1);
        Assert.assertTrue(allByIds.contains(ruleEntityState));
    }

    @Test
    public void testFindAllEntityStatesByEntityIds() {
        AlarmRuleEntityState alarmRuleEntityState = createAlarmRuleEntityState(tenantId, deviceEntity.getId(), "test data");
        AlarmRuleEntityState alarmRuleEntityState2 = createAlarmRuleEntityState(tenantId, assetEntity.getId(), "test data2");
        AlarmRuleEntityState ruleEntityState = alarmRuleEntityStateService.save(tenantId, alarmRuleEntityState);
        AlarmRuleEntityState ruleEntityState2 = alarmRuleEntityStateService.save(tenantId, alarmRuleEntityState2);


        List<AlarmRuleEntityState> allByIds = alarmRuleEntityStateService.findAllByIds(Arrays.asList(deviceEntity.getId(), assetEntity.getId()));
        Assert.assertEquals(allByIds.size(), 2);
        Assert.assertTrue(allByIds.containsAll(Arrays.asList(ruleEntityState, ruleEntityState2)));

        List<AlarmRuleEntityState> allByIds2 = alarmRuleEntityStateService.findAllByIds(Collections.singletonList(assetEntity.getId()));
        Assert.assertEquals(allByIds2.size(), 1);
        Assert.assertTrue(allByIds2.contains(ruleEntityState2));

        List<AlarmRuleEntityState> all = alarmRuleEntityStateService.findAll(new PageLink(10)).getData();
        Assert.assertEquals(all.size(), 2);
        Assert.assertTrue(all.containsAll(Arrays.asList(ruleEntityState, ruleEntityState2)));
    }

    private AlarmRuleEntityState createAlarmRuleEntityState(TenantId tenantId, EntityId entityId, String data) {
        AlarmRuleEntityState alarmRuleEntityState = new AlarmRuleEntityState();
        alarmRuleEntityState.setEntityId(entityId);
        alarmRuleEntityState.setTenantId(tenantId);
        alarmRuleEntityState.setData(data);
        return alarmRuleEntityState;
    }

}
