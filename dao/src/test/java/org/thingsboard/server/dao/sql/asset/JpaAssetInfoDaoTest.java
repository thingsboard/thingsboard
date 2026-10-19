// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.asset;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetInfo;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.asset.AssetDao;
import org.thingsboard.server.dao.asset.AssetInfoDao;
import org.thingsboard.server.dao.asset.AssetProfileDao;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.service.AbstractServiceTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JpaAssetInfoDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private AssetInfoDao assetInfoDao;

    @Autowired
    private AssetDao assetDao;

    @Autowired
    private AssetProfileDao assetProfileDao;

    @Autowired
    private CustomerDao customerDao;

    private List<Asset> assets = new ArrayList<>();

    private Map<String, AssetProfileId> savedAssetProfiles = new HashMap<>();

    @After
    public void tearDown() {
        for (Asset asset : assets) {
            assetDao.removeById(asset.getTenantId(), asset.getUuidId());
        }
        assets.clear();
        for (AssetProfileId assetProfileId : savedAssetProfiles.values()) {
            assetProfileDao.removeById(TenantId.SYS_TENANT_ID, assetProfileId.getId());
        }
        savedAssetProfiles.clear();
    }

    @Test
    public void testFindAssetInfosByTenantId() {
        UUID tenantId1 = Uuids.timeBased();
        UUID tenantId2 = Uuids.timeBased();

        for (int i = 0; i < 20; i++) {
            assets.add(createAsset(tenantId1, null, i));
            assets.add(createAsset(tenantId2, null, i * 2));
        }

        PageLink pageLink = new PageLink(15, 0, "ASSET");
        PageData<AssetInfo> assetInfos1 = assetInfoDao.findAssetsByTenantId(tenantId1, pageLink);
        Assert.assertEquals(15, assetInfos1.getData().size());

        PageData<AssetInfo> assetInfos2 = assetInfoDao.findAssetsByTenantId(tenantId1, pageLink.nextPageLink());
        Assert.assertEquals(5, assetInfos2.getData().size());
    }

    @Test
    public void testFindAssetInfosBySearchText() {
        UUID tenantId = Uuids.timeBased();
        CustomerId customerId = createCustomer(tenantId, null, 0).getId();

        for (int i = 0; i < 5; i++) {
            Asset asset = new Asset();
            asset.setId(new AssetId(Uuids.timeBased()));
            asset.setTenantId(TenantId.fromUUID(tenantId));
            asset.setCustomerId(customerId);
            asset.setName("ASSET_" + i);
            asset.setLabel("label_" + i);
            String type = "asset_type_" + i;
            asset.setType(type);
            asset.setAssetProfileId(assetProfileId(type));
            assets.add(assetDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, asset));
        }

        PageData<AssetInfo> assetInfosByName = assetInfoDao.findAssetsByTenantIdAndCustomerId(tenantId,customerId.getId(), new PageLink(15, 0, "ASSET_2"));
        Assert.assertEquals(1, assetInfosByName.getData().size());

        PageData<AssetInfo> assetInfosByLabel = assetInfoDao.findAssetsByTenantIdAndCustomerId(tenantId, customerId.getId(), new PageLink(15, 0, "label_3"));
        Assert.assertEquals(1, assetInfosByLabel.getData().size());

        PageData<AssetInfo> assetInfosByType = assetInfoDao.findAssetsByTenantIdAndCustomerId(tenantId, customerId.getId(), new PageLink(15, 0, "asset_type_4"));
        Assert.assertEquals(1, assetInfosByType.getData().size());
    }

    @Test
    public void testFindAssetInfosByTenantIdAndCustomerIdIncludingSubCustomers() {
        UUID tenantId1 = Uuids.timeBased();
        Customer customer1 = createCustomer(tenantId1, null, 0);
        Customer subCustomer2 = createCustomer(tenantId1, customer1.getUuidId(),1);

        for (int i = 0; i < 20; i++) {
            assets.add(createAsset(tenantId1, customer1.getUuidId(), i));
            assets.add(createAsset(tenantId1, subCustomer2.getUuidId(), 20 + i * 2));
        }

        PageLink pageLink = new PageLink(30, 0, "ASSET", new SortOrder("ownerName", SortOrder.Direction.ASC));
        PageData<AssetInfo> assetInfos1 = assetInfoDao.findAssetsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, customer1.getUuidId(), pageLink);
        Assert.assertEquals(30, assetInfos1.getData().size());
        assetInfos1.getData().forEach(assetInfo -> Assert.assertNotEquals("CUSTOMER_0", assetInfo.getOwnerName()));

        PageData<AssetInfo> assetInfos2 = assetInfoDao.findAssetsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, customer1.getUuidId(), pageLink.nextPageLink());
        Assert.assertEquals(10, assetInfos2.getData().size());

        PageData<AssetInfo> assetInfos3 = assetInfoDao.findAssetsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, subCustomer2.getUuidId(), pageLink);
        Assert.assertEquals(20, assetInfos3.getData().size());
    }

    @Test
    public void testFindAssetInfosByTenantIdAndCustomerIdIncludingSubCustomersThreeLevels() {
        UUID tenantId1 = Uuids.timeBased();
        Customer customer1 = createCustomer(tenantId1, null, 0);
        Customer subCustomer2 = createCustomer(tenantId1, customer1.getUuidId(), 1);
        Customer subSubCustomer3 = createCustomer(tenantId1, subCustomer2.getUuidId(), 2);

        for (int i = 0; i < 5; i++) {
            assets.add(createAsset(tenantId1, customer1.getUuidId(), i));
            assets.add(createAsset(tenantId1, subCustomer2.getUuidId(), 5 + i));
            assets.add(createAsset(tenantId1, subSubCustomer3.getUuidId(), 10 + i));
        }

        PageLink pageLink = new PageLink(30, 0, "ASSET", new SortOrder("ownerName", SortOrder.Direction.ASC));

        // From the root: all 15 assets (own + child + grandchild) are included
        PageData<AssetInfo> fromRoot = assetInfoDao.findAssetsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, customer1.getUuidId(), pageLink);
        Assert.assertEquals(15, fromRoot.getData().size());
        fromRoot.getData().forEach(assetInfo -> Assert.assertNotEquals("CUSTOMER_0", assetInfo.getOwnerName()));

        // From the middle: own + grandchild only (10)
        PageData<AssetInfo> fromMiddle = assetInfoDao.findAssetsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, subCustomer2.getUuidId(), pageLink);
        Assert.assertEquals(10, fromMiddle.getData().size());

        // From the leaf (no children): only its own 5
        PageData<AssetInfo> fromLeaf = assetInfoDao.findAssetsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId1, subSubCustomer3.getUuidId(), pageLink);
        Assert.assertEquals(5, fromLeaf.getData().size());
    }

    @Test
    public void testFindAssetsByTenantIdAndCustomerIdAndAssetProfileIdIncludingSubCustomers() {
        UUID tenantId = Uuids.timeBased();
        Customer customer = createCustomer(tenantId, null, 0);
        Customer subCustomer = createCustomer(tenantId, customer.getUuidId(), 1);
        AssetProfileId assetProfileId = assetProfileId("test");

        for (int i = 0; i < 10; i++) {
            assets.add(createAsset(tenantId, customer.getUuidId(), "test", i));
            assets.add(createAsset(tenantId, subCustomer.getUuidId(), "test", 10 + i));
        }

        PageLink pageLink = new PageLink(30, 0, "test", new SortOrder("name", SortOrder.Direction.ASC));

        // From the parent: own + sub-customer assets of the given profile (20).
        PageData<AssetInfo> assetInfos = assetInfoDao.findAssetsByTenantIdAndCustomerIdAndAssetProfileIdIncludingSubCustomers(tenantId, customer.getUuidId(), assetProfileId.getId(), pageLink);
        Assert.assertEquals(20, assetInfos.getTotalElements());
        assetInfos.getData().forEach(asset -> Assert.assertEquals(assetProfileId, asset.getAssetProfileId()));
        assetInfos.getData().forEach(asset -> Assert.assertEquals(tenantId, asset.getTenantId().getId()));
        assetInfos.getData().forEach(asset -> Assert.assertTrue(asset.getName().contains("ASSET_")));

        // From the sub-customer (a leaf): only its own 10 of the given profile.
        PageData<AssetInfo> subAssetInfos = assetInfoDao.findAssetsByTenantIdAndCustomerIdAndAssetProfileIdIncludingSubCustomers(tenantId, subCustomer.getUuidId(), assetProfileId.getId(), pageLink);
        Assert.assertEquals(10, subAssetInfos.getTotalElements());
        subAssetInfos.getData().forEach(asset -> Assert.assertEquals(assetProfileId, asset.getAssetProfileId()));
    }

    @Test
    public void testFindAssetInfosByTenantIdAndNonExistentCustomerIdIncludingSubCustomers() {
        UUID tenantId = Uuids.timeBased();
        // Customer that does not exist (covers the Citus empty-Optional short-circuit, avoiding an IN () query)
        UUID nonExistentCustomerId = Uuids.timeBased();

        for (int i = 0; i < 10; i++) {
            assets.add(createAsset(tenantId, null, i));
        }

        PageLink pageLink = new PageLink(30, 0, "ASSET", new SortOrder("ownerName", SortOrder.Direction.ASC));
        PageData<AssetInfo> assetInfos = assetInfoDao.findAssetsByTenantIdAndCustomerIdIncludingSubCustomers(tenantId, nonExistentCustomerId, pageLink);
        Assert.assertTrue(assetInfos.getData().isEmpty());
        Assert.assertEquals(0, assetInfos.getTotalElements());
    }

    private Asset createAsset(UUID tenantId, UUID customerId, int index) {
        return this.createAsset(tenantId, customerId, null, index);
    }

    private Asset createAsset(UUID tenantId, UUID customerId, String type, int index) {
        if (type == null) {
            type = "default";
        }
        Asset asset = new Asset();
        asset.setId(new AssetId(Uuids.timeBased()));
        asset.setTenantId(TenantId.fromUUID(tenantId));
        asset.setCustomerId(new CustomerId(customerId));
        asset.setName("ASSET_" + index);
        asset.setType(type);
        asset.setAssetProfileId(assetProfileId(type));
        return assetDao.save(AbstractServiceTest.SYSTEM_TENANT_ID, asset);
    }

    private Customer createCustomer(UUID tenantId, UUID parentCustomerId, int index) {
        Customer customer = new Customer();
        customer.setId(new CustomerId(Uuids.timeBased()));
        if (parentCustomerId != null) {
            customer.setParentCustomerId(new CustomerId(parentCustomerId));
        }
        customer.setTenantId(TenantId.fromUUID(tenantId));
        customer.setTitle("CUSTOMER_" + index);
        return customerDao.save(TenantId.fromUUID(tenantId), customer);
    }

    private AssetProfileId assetProfileId(String type) {
        AssetProfileId assetProfileId = savedAssetProfiles.get(type);
        if (assetProfileId == null) {
            AssetProfile assetProfile = new AssetProfile();
            assetProfile.setName(type);
            assetProfile.setTenantId(TenantId.SYS_TENANT_ID);
            assetProfile.setDescription("Test");
            AssetProfile savedAssetProfile = assetProfileDao.save(TenantId.SYS_TENANT_ID, assetProfile);
            assetProfileId = savedAssetProfile.getId();
            savedAssetProfiles.put(type, assetProfileId);
        }
        return assetProfileId;
    }
}
