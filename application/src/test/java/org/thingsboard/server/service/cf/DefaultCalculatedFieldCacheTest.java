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
package org.thingsboard.server.service.cf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DefaultCalculatedFieldCacheTest {

    @Mock
    private CalculatedFieldService calculatedFieldService;
    @Mock
    private TbAssetProfileCache assetProfileCache;
    @Mock
    private TbDeviceProfileCache deviceProfileCache;
    @Mock
    private TbTenantProfileCache tenantProfileCache;
    @Mock
    private DeviceService deviceService;
    @Mock
    private AssetService assetService;
    @Mock
    private CustomerService customerService;

    private DefaultCalculatedFieldCache cache;

    @BeforeEach
    public void setUp() {
        // ActorSystemContext is only used in getCalculatedFieldCtx (not tested here), so null is safe
        OwnerService ownerService = new OwnerService(deviceService, assetService, customerService);
        cache = new DefaultCalculatedFieldCache(calculatedFieldService, assetProfileCache,
                deviceProfileCache, tenantProfileCache, null, ownerService);

    }

    // --- Tenant deletion tests ---

    @Test
    public void onComponentLifecycleEvent_tenantDeleted_evictsAllTenantCfsFromAllMaps() {
        TenantId tenant1 = new TenantId(UUID.randomUUID());
        TenantId tenant2 = new TenantId(UUID.randomUUID());
        DeviceId device1 = new DeviceId(UUID.randomUUID());
        DeviceId device2 = new DeviceId(UUID.randomUUID());

        CalculatedField cf1 = addCfToCache(tenant1, device1);
        CalculatedField cf2 = addCfToCache(tenant2, device2);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant1, tenant1, ComponentLifecycleEvent.DELETED));

        assertThat(cache.getCalculatedField(cf1.getId())).isNull();
        assertThat(cache.getCalculatedFieldsByEntityId(device1)).isEmpty();
        assertThat(cache.getCalculatedField(cf2.getId())).isEqualTo(cf2);
        assertThat(cache.getCalculatedFieldsByEntityId(device2)).containsExactly(cf2);
    }

    @Test
    public void onComponentLifecycleEvent_tenantDeleted_evictsOwnerEntities() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        DeviceId device = new DeviceId(UUID.randomUUID());
        stubDeviceOwner(tenant, device, tenant);

        cache.addOwnerEntity(tenant, device);
        assertThat(cache.getDynamicEntities(tenant, tenant)).contains(device);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, tenant, ComponentLifecycleEvent.DELETED));

        // After eviction, getDynamicEntities triggers a fresh load from ownerService (empty)
        assertThat(cache.getDynamicEntities(tenant, tenant)).doesNotContain(device);
    }

    @Test
    public void onComponentLifecycleEvent_tenantDeleted_removesLinksToLinkedEntities() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        DeviceId cfEntity = new DeviceId(UUID.randomUUID());
        DeviceId linkedDevice = new DeviceId(UUID.randomUUID());

        CalculatedField cf = addCfToCache(tenant, cfEntity, linkedDevice);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, tenant, ComponentLifecycleEvent.DELETED));

        assertThat(cache.getCalculatedFieldLinksByEntityId(linkedDevice)).isEmpty();
        assertThat(cache.getCalculatedField(cf.getId())).isNull();
    }

    @Test
    public void onComponentLifecycleEvent_tenantUpdated_doesNotEvictCfs() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        DeviceId device = new DeviceId(UUID.randomUUID());
        CalculatedField cf = addCfToCache(tenant, device);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, tenant, ComponentLifecycleEvent.UPDATED));

        assertThat(cache.getCalculatedField(cf.getId())).isEqualTo(cf);
    }

    // --- Device/Asset deletion tests ---

    @Test
    public void onComponentLifecycleEvent_deviceDeleted_evictsCfsForThatDevice() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        DeviceId device = new DeviceId(UUID.randomUUID());
        CalculatedField cf = addCfToCache(tenant, device);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, device, ComponentLifecycleEvent.DELETED));

        assertThat(cache.getCalculatedField(cf.getId())).isNull();
        assertThat(cache.getCalculatedFieldsByEntityId(device)).isEmpty();
    }

    @Test
    public void onComponentLifecycleEvent_deviceDeleted_removesLinksForLinkedEntities() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        DeviceId device = new DeviceId(UUID.randomUUID());
        DeviceId linkedDevice = new DeviceId(UUID.randomUUID());
        addCfToCache(tenant, device, linkedDevice);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, device, ComponentLifecycleEvent.DELETED));

        assertThat(cache.getCalculatedFieldLinksByEntityId(linkedDevice)).isEmpty();
    }

    @Test
    public void onComponentLifecycleEvent_deviceDeleted_evictsDeviceFromOwnerEntities() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        CustomerId customer = new CustomerId(UUID.randomUUID());
        DeviceId device = new DeviceId(UUID.randomUUID());
        stubDeviceOwner(tenant, device, customer);

        cache.addOwnerEntity(tenant, device);
        assertThat(cache.getDynamicEntities(tenant, customer)).contains(device);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, device, ComponentLifecycleEvent.DELETED));

        assertThat(cache.getDynamicEntities(tenant, customer)).doesNotContain(device);
    }

    @Test
    public void onComponentLifecycleEvent_assetDeleted_evictsCfsForThatAsset() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        AssetId asset = new AssetId(UUID.randomUUID());
        CalculatedField cf = addCfToCache(tenant, asset);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, asset, ComponentLifecycleEvent.DELETED));

        assertThat(cache.getCalculatedField(cf.getId())).isNull();
        assertThat(cache.getCalculatedFieldsByEntityId(asset)).isEmpty();
    }

    @Test
    public void onComponentLifecycleEvent_deviceCreated_addsDeviceToOwnerEntities() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        CustomerId customer = new CustomerId(UUID.randomUUID());
        DeviceId device = new DeviceId(UUID.randomUUID());
        stubDeviceOwner(tenant, device, customer);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, device, ComponentLifecycleEvent.CREATED));

        assertThat(cache.getDynamicEntities(tenant, customer)).contains(device);
    }

    // --- Customer deletion tests ---

    @Test
    public void onComponentLifecycleEvent_customerDeleted_evictsCustomerOwnerEntries() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        CustomerId customer = new CustomerId(UUID.randomUUID());
        DeviceId device = new DeviceId(UUID.randomUUID());
        stubDeviceOwner(tenant, device, customer);

        cache.addOwnerEntity(tenant, device);
        assertThat(cache.getDynamicEntities(tenant, customer)).contains(device);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, customer, ComponentLifecycleEvent.DELETED));

        // The customer's owned-entities entry is evicted; fresh load returns empty
        assertThat(cache.getDynamicEntities(tenant, customer)).doesNotContain(device);
    }

    // --- DeviceProfile/AssetProfile deletion tests ---

    @Test
    public void onComponentLifecycleEvent_deviceProfileDeleted_evictsCfsForThatProfile() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        DeviceProfileId profileId = new DeviceProfileId(UUID.randomUUID());
        CalculatedField cf = addCfToCache(tenant, profileId);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, profileId, ComponentLifecycleEvent.DELETED));

        assertThat(cache.getCalculatedField(cf.getId())).isNull();
        assertThat(cache.getCalculatedFieldsByEntityId(profileId)).isEmpty();
    }

    @Test
    public void onComponentLifecycleEvent_deviceProfileDeleted_removesLinksForLinkedEntities() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        DeviceProfileId profileId = new DeviceProfileId(UUID.randomUUID());
        DeviceId linkedDevice = new DeviceId(UUID.randomUUID());
        addCfToCache(tenant, profileId, linkedDevice);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, profileId, ComponentLifecycleEvent.DELETED));

        assertThat(cache.getCalculatedFieldLinksByEntityId(linkedDevice)).isEmpty();
    }

    @Test
    public void onComponentLifecycleEvent_deviceProfileDeleted_doesNotEvictOtherProfilesCfs() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        DeviceProfileId profile1 = new DeviceProfileId(UUID.randomUUID());
        DeviceProfileId profile2 = new DeviceProfileId(UUID.randomUUID());
        CalculatedField cf1 = addCfToCache(tenant, profile1);
        CalculatedField cf2 = addCfToCache(tenant, profile2);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, profile1, ComponentLifecycleEvent.DELETED));

        assertThat(cache.getCalculatedField(cf1.getId())).isNull();
        assertThat(cache.getCalculatedFieldsByEntityId(profile1)).isEmpty();
        assertThat(cache.getCalculatedField(cf2.getId())).isEqualTo(cf2);
        assertThat(cache.getCalculatedFieldsByEntityId(profile2)).containsExactly(cf2);
    }

    @Test
    public void onComponentLifecycleEvent_deviceProfileUpdated_doesNotEvictCfs() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        DeviceProfileId profileId = new DeviceProfileId(UUID.randomUUID());
        CalculatedField cf = addCfToCache(tenant, profileId);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, profileId, ComponentLifecycleEvent.UPDATED));

        assertThat(cache.getCalculatedField(cf.getId())).isEqualTo(cf);
        assertThat(cache.getCalculatedFieldsByEntityId(profileId)).containsExactly(cf);
    }

    @Test
    public void onComponentLifecycleEvent_assetProfileDeleted_evictsCfsForThatProfile() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        AssetProfileId profileId = new AssetProfileId(UUID.randomUUID());
        CalculatedField cf = addCfToCache(tenant, profileId);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, profileId, ComponentLifecycleEvent.DELETED));

        assertThat(cache.getCalculatedField(cf.getId())).isNull();
        assertThat(cache.getCalculatedFieldsByEntityId(profileId)).isEmpty();
    }

    @Test
    public void onComponentLifecycleEvent_assetProfileDeleted_removesLinksForLinkedEntities() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        AssetProfileId profileId = new AssetProfileId(UUID.randomUUID());
        AssetId linkedAsset = new AssetId(UUID.randomUUID());
        addCfToCache(tenant, profileId, linkedAsset);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, profileId, ComponentLifecycleEvent.DELETED));

        assertThat(cache.getCalculatedFieldLinksByEntityId(linkedAsset)).isEmpty();
    }

    @Test
    public void onComponentLifecycleEvent_assetProfileDeleted_doesNotEvictOtherProfilesCfs() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        AssetProfileId profile1 = new AssetProfileId(UUID.randomUUID());
        AssetProfileId profile2 = new AssetProfileId(UUID.randomUUID());
        CalculatedField cf1 = addCfToCache(tenant, profile1);
        CalculatedField cf2 = addCfToCache(tenant, profile2);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, profile1, ComponentLifecycleEvent.DELETED));

        assertThat(cache.getCalculatedField(cf1.getId())).isNull();
        assertThat(cache.getCalculatedFieldsByEntityId(profile1)).isEmpty();
        assertThat(cache.getCalculatedField(cf2.getId())).isEqualTo(cf2);
        assertThat(cache.getCalculatedFieldsByEntityId(profile2)).containsExactly(cf2);
    }

    @Test
    public void onComponentLifecycleEvent_assetProfileUpdated_doesNotEvictCfs() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        AssetProfileId profileId = new AssetProfileId(UUID.randomUUID());
        CalculatedField cf = addCfToCache(tenant, profileId);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, profileId, ComponentLifecycleEvent.UPDATED));

        assertThat(cache.getCalculatedField(cf.getId())).isEqualTo(cf);
        assertThat(cache.getCalculatedFieldsByEntityId(profileId)).containsExactly(cf);
    }

    // --- CalculatedField lifecycle tests ---

    @Test
    public void onComponentLifecycleEvent_calculatedFieldCreated_addsCfToCache() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        DeviceId device = new DeviceId(UUID.randomUUID());
        CalculatedFieldId cfId = new CalculatedFieldId(UUID.randomUUID());
        CalculatedField cf = buildCalculatedField(cfId, tenant, device, simpleCfConfig());
        when(calculatedFieldService.findById(tenant, cfId)).thenReturn(cf);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, cfId, ComponentLifecycleEvent.CREATED));

        assertThat(cache.getCalculatedField(cfId)).isEqualTo(cf);
        assertThat(cache.getCalculatedFieldsByEntityId(device)).containsExactly(cf);
    }

    @Test
    public void onComponentLifecycleEvent_calculatedFieldDeleted_evictsCfFromCache() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        DeviceId device = new DeviceId(UUID.randomUUID());
        CalculatedField cf = addCfToCache(tenant, device);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, cf.getId(), ComponentLifecycleEvent.DELETED));

        assertThat(cache.getCalculatedField(cf.getId())).isNull();
        assertThat(cache.getCalculatedFieldsByEntityId(device)).isEmpty();
    }

    @Test
    public void onComponentLifecycleEvent_calculatedFieldUpdated_refreshesCfInCache() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        DeviceId device = new DeviceId(UUID.randomUUID());
        CalculatedField cf = addCfToCache(tenant, device);

        CalculatedField updatedCf = buildCalculatedField(cf.getId(), tenant, device, simpleCfConfig());
        updatedCf.setName("updated-name");
        when(calculatedFieldService.findById(tenant, cf.getId())).thenReturn(updatedCf);

        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, cf.getId(), ComponentLifecycleEvent.UPDATED));

        assertThat(cache.getCalculatedField(cf.getId())).isEqualTo(updatedCf);
    }

    // --- evictOwner recursive traversal tests ---

    @Test
    public void evictOwner_customerDeleted_recursivelyEvictsDevicesOwnedByThatCustomer() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        CustomerId customer = new CustomerId(UUID.randomUUID());
        DeviceId device = new DeviceId(UUID.randomUUID());

        stubDeviceOwner(tenant, device, customer);
        when(customerService.findCustomersByTenantId(any(), any())).thenReturn(PageData.emptyPageData());

        // tenant owns customer (getOwner for CUSTOMER returns tenantId)
        cache.addOwnerEntity(tenant, customer);         // ownerEntities[tenant] = {customer}
        cache.addOwnerEntity(tenant, device);           // ownerEntities[customer] = {device}

        assertThat(cache.getDynamicEntities(tenant, tenant)).contains(customer);
        assertThat(cache.getDynamicEntities(tenant, customer)).contains(device);

        // deleting the customer evicts the customer key and recursively cleans its owned set
        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, customer, ComponentLifecycleEvent.DELETED));

        assertThat(cache.getDynamicEntities(tenant, customer)).doesNotContain(device);
    }

    @Test
    public void evictOwner_tenantDeleted_recursivelyEvictsCustomerAndItsOwnedDevices() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        CustomerId customer = new CustomerId(UUID.randomUUID());
        DeviceId device = new DeviceId(UUID.randomUUID());

        stubDeviceOwner(tenant, device, customer);
        when(customerService.findCustomersByTenantId(any(), any())).thenReturn(PageData.emptyPageData());

        cache.addOwnerEntity(tenant, customer);         // ownerEntities[tenant] = {customer}
        cache.addOwnerEntity(tenant, device);           // ownerEntities[customer] = {device}

        assertThat(cache.getDynamicEntities(tenant, tenant)).contains(customer);
        assertThat(cache.getDynamicEntities(tenant, customer)).contains(device);

        // deleting the tenant: evictOwner(tenant) finds customer (CUSTOMER type) and recurses into it
        cache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, tenant, ComponentLifecycleEvent.DELETED));

        // both levels must be gone
        assertThat(cache.getDynamicEntities(tenant, tenant)).doesNotContain(customer);
        assertThat(cache.getDynamicEntities(tenant, customer)).doesNotContain(device);
    }

    // --- TenantProfile lifecycle tests ---

    @Test
    public void onComponentLifecycleEvent_tenantProfileUpdated_callsHandleTenantProfileUpdate() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        TenantProfileId profileId = new TenantProfileId(UUID.randomUUID());
        DefaultCalculatedFieldCache spyCache = spy(cache);

        spyCache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, profileId, ComponentLifecycleEvent.UPDATED));

        verify(spyCache).handleTenantProfileUpdate(profileId);
    }

    @Test
    public void onComponentLifecycleEvent_tenantProfileDeleted_doesNotCallHandleTenantProfileUpdate() {
        TenantId tenant = new TenantId(UUID.randomUUID());
        TenantProfileId profileId = new TenantProfileId(UUID.randomUUID());
        DefaultCalculatedFieldCache spyCache = spy(cache);

        spyCache.onComponentLifecycleEvent(new ComponentLifecycleMsg(tenant, profileId, ComponentLifecycleEvent.DELETED));

        verify(spyCache, never()).handleTenantProfileUpdate(any());
    }

    // --- Helpers ---

    private void stubDeviceOwner(TenantId tenantId, DeviceId deviceId, EntityId ownerId) {
        Device device = new Device();
        device.setId(deviceId);
        device.setTenantId(tenantId);
        if (ownerId instanceof CustomerId customerId) {
            device.setCustomerId(customerId);
        }
        // If ownerId is a TenantId, leaving customerId null means getOwnerId() returns tenantId
        when(deviceService.findDeviceById(tenantId, deviceId)).thenReturn(device);
        // Stubs for getOwnedEntities iteration (empty pages — device is added explicitly)
        when(deviceService.findDeviceInfosByFilter(any(), any())).thenReturn(PageData.emptyPageData());
        when(assetService.findAssetsByTenantIdAndCustomerId(any(), any(), any())).thenReturn(PageData.emptyPageData());
        if (ownerId instanceof TenantId) {
            when(customerService.findCustomersByTenantId(any(), any())).thenReturn(PageData.emptyPageData());
        }
    }

    private CalculatedField addCfToCache(TenantId tenantId, EntityId entityId) {
        CalculatedFieldId cfId = new CalculatedFieldId(UUID.randomUUID());
        CalculatedField cf = buildCalculatedField(cfId, tenantId, entityId, simpleCfConfig());
        when(calculatedFieldService.findById(tenantId, cfId)).thenReturn(cf);
        cache.addCalculatedField(tenantId, cfId);
        return cf;
    }

    private CalculatedField addCfToCache(TenantId tenantId, EntityId entityId, EntityId linkedEntity) {
        CalculatedFieldId cfId = new CalculatedFieldId(UUID.randomUUID());
        CalculatedFieldConfiguration config = linkedEntityCfConfig(tenantId, cfId, linkedEntity);
        CalculatedField cf = buildCalculatedField(cfId, tenantId, entityId, config);
        when(calculatedFieldService.findById(tenantId, cfId)).thenReturn(cf);
        cache.addCalculatedField(tenantId, cfId);
        return cf;
    }

    private CalculatedField buildCalculatedField(CalculatedFieldId id, TenantId tenantId, EntityId entityId, CalculatedFieldConfiguration config) {
        CalculatedField cf = new CalculatedField();
        cf.setId(id);
        cf.setTenantId(tenantId);
        cf.setEntityId(entityId);
        cf.setType(CalculatedFieldType.SIMPLE);
        cf.setName("test-cf-" + id.getId());
        cf.setConfiguration(config);
        return cf;
    }

    private CalculatedFieldConfiguration simpleCfConfig() {
        CalculatedFieldConfiguration config = mock(CalculatedFieldConfiguration.class);
        when(config.getReferencedEntities()).thenReturn(Collections.emptySet());
        when(config.buildCalculatedFieldLinks(any(), any(), any())).thenReturn(Collections.emptyList());
        return config;
    }

    private CalculatedFieldConfiguration linkedEntityCfConfig(TenantId tenantId, CalculatedFieldId cfId, EntityId linkedEntity) {
        CalculatedFieldConfiguration config = mock(CalculatedFieldConfiguration.class);
        CalculatedFieldLink link = new CalculatedFieldLink(tenantId, linkedEntity, cfId);
        when(config.getReferencedEntities()).thenReturn(Set.of(linkedEntity));
        when(config.buildCalculatedFieldLinks(any(), any(), any())).thenReturn(List.of(link));
        when(config.buildCalculatedFieldLink(any(), eq(linkedEntity), any())).thenReturn(link);
        return config;
    }

}
