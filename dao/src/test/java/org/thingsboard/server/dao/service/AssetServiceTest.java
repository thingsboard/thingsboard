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
package org.thingsboard.server.dao.service;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetInfo;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.common.data.tenant.profile.TenantProfileData;
import org.thingsboard.server.dao.asset.AssetDao;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.tenant.TenantProfileService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

@DaoSqlTest
public class AssetServiceTest extends AbstractServiceTest {

    @Autowired
    AssetService assetService;
    @Autowired
    AssetDao assetDao;
    @Autowired
    CustomerService customerService;
    @Autowired
    RelationService relationService;
    @Autowired
    TenantProfileService tenantProfileService;
    @Autowired
    private AssetProfileService assetProfileService;
    @Autowired
    private CalculatedFieldService calculatedFieldService;
    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    private static ListeningExecutorService executor;

    private IdComparator<Asset> idComparator = new IdComparator<>();
    private TenantId anotherTenantId;

    @BeforeClass
    public static void before() {
        executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10, ThingsBoardThreadFactory.forName("AssetServiceTestScope")));
    }

    @AfterClass
    public static void after() {
        executor.shutdownNow();
    }

    @Test
    public void testSaveAsset() {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = assetService.saveAsset(asset);

        Assert.assertNotNull(savedAsset);
        Assert.assertNotNull(savedAsset.getId());
        Assert.assertTrue(savedAsset.getCreatedTime() > 0);
        Assert.assertEquals(asset.getTenantId(), savedAsset.getTenantId());
        Assert.assertNotNull(savedAsset.getCustomerId());
        Assert.assertEquals(NULL_UUID, savedAsset.getCustomerId().getId());
        Assert.assertEquals(asset.getName(), savedAsset.getName());

        savedAsset.setName("My new asset");

        assetService.saveAsset(savedAsset);
        Asset foundAsset = assetService.findAssetById(tenantId, savedAsset.getId());
        Assert.assertEquals(foundAsset.getName(), savedAsset.getName());

        assetService.deleteAsset(tenantId, savedAsset.getId());
    }

    @Test
    public void testAssetLimitOnTenantProfileLevel() throws InterruptedException {
        TenantProfile tenantProfile = new TenantProfile();
        tenantProfile.setName("Test profile");
        tenantProfile.setDescription("Test");
        TenantProfileData profileData = new TenantProfileData();
        profileData.setConfiguration(DefaultTenantProfileConfiguration.builder().maxAssets(5l).build());
        tenantProfile.setProfileData(profileData);
        tenantProfile.setDefault(false);
        tenantProfile.setIsolatedTbRuleEngine(false);

        tenantProfile = tenantProfileService.saveTenantProfile(anotherTenantId, tenantProfile);
        anotherTenantId = createTenant(tenantProfile.getId()).getId();

        for (int i = 0; i < 20; i++) {
            executor.submit(() -> {
                Asset asset = new Asset();
                asset.setTenantId(anotherTenantId);
                asset.setName(RandomStringUtils.randomAlphabetic(10));
                asset.setType("default");
                assetService.saveAsset(asset);
            });
        }

        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            long countByTenantId = assetService.countByTenantId(anotherTenantId);
            return countByTenantId == 5;
        });

        Thread.sleep(2000);
        assertThat(assetService.countByTenantId(anotherTenantId)).isEqualTo(5);
    }

    @Test
    public void testShouldNotPutInCacheRolledbackAssetProfile() {
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setName(StringUtils.randomAlphabetic(10));
        assetProfile.setTenantId(tenantId);

        Asset asset = new Asset();
        asset.setName("My asset" + StringUtils.randomAlphabetic(15));
        asset.setType(assetProfile.getName());
        asset.setTenantId(tenantId);

        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        TransactionStatus status = platformTransactionManager.getTransaction(def);
        try {
            assetProfileService.saveAssetProfile(assetProfile);
            assetService.saveAsset(asset);
        } finally {
            platformTransactionManager.rollback(status);
        }
        AssetProfile assetProfileByName = assetProfileService.findAssetProfileByName(tenantId, assetProfile.getName());
        Assert.assertNull(assetProfileByName);
    }

    @Test
    public void testSaveAssetWithEmptyName() {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setType("default");
        Assertions.assertThrows(DataValidationException.class, () -> {
            assetService.saveAsset(asset);
        });
    }

    @Test
    public void testSaveDeviceWithNameContains0x00_thenDataValidationException() {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setType("default");
        asset.setName("F0929906\000\000\000\000\000\000\000\000\000");
        Assertions.assertThrows(DataValidationException.class, () -> {
            assetService.saveAsset(asset);
        });
    }

    @Test
    public void testSaveAssetWithEmptyTenant() {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Assertions.assertThrows(DataValidationException.class, () -> {
            assetService.saveAsset(asset);
        });
    }

    @Test
    public void testSaveAssetWithInvalidTenant() {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        asset.setTenantId(TenantId.fromUUID(Uuids.timeBased()));
        Assertions.assertThrows(DataValidationException.class, () -> {
            assetService.saveAsset(asset);
        });
    }

    @Test
    public void testAssignAssetToNonExistentCustomer() {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        asset.setTenantId(tenantId);
        Asset savedAsset = assetService.saveAsset(asset);
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                assetService.assignAssetToCustomer(tenantId, savedAsset.getId(), new CustomerId(Uuids.timeBased()));
            });
        } finally {
            assetService.deleteAsset(tenantId, savedAsset.getId());
        }
    }

    @Test
    public void testAssignAssetToCustomerFromDifferentTenant() {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        asset.setTenantId(tenantId);
        Asset savedAsset = assetService.saveAsset(asset);
        Tenant tenant = new Tenant();
        tenant.setTitle("Test different tenant");
        tenant = tenantService.saveTenant(tenant);
        Customer customer = new Customer();
        customer.setTenantId(tenant.getId());
        customer.setTitle("Test different customer");
        Customer savedCustomer = customerService.saveCustomer(customer);
        try {
            Assertions.assertThrows(DataValidationException.class, () -> {
                assetService.assignAssetToCustomer(tenantId, savedAsset.getId(), savedCustomer.getId());
            });
        } finally {
            assetService.deleteAsset(tenantId, savedAsset.getId());
            tenantService.deleteTenant(tenant.getId());
        }
    }

    @Test
    public void testFindAssetById() {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = assetService.saveAsset(asset);
        Asset foundAsset = assetService.findAssetById(tenantId, savedAsset.getId());
        Assert.assertNotNull(foundAsset);
        Assert.assertEquals(savedAsset, foundAsset);
        assetService.deleteAsset(tenantId, savedAsset.getId());
    }

    @Test
    public void testFindAssetTypesByTenantId() throws Exception {
        List<Asset> assets = new ArrayList<>();
        try {
            for (int i = 0; i < 3; i++) {
                Asset asset = new Asset();
                asset.setTenantId(tenantId);
                asset.setName("My asset B" + i);
                asset.setType("typeB");
                assets.add(assetService.saveAsset(asset));
            }
            for (int i = 0; i < 7; i++) {
                Asset asset = new Asset();
                asset.setTenantId(tenantId);
                asset.setName("My asset C" + i);
                asset.setType("typeC");
                assets.add(assetService.saveAsset(asset));
            }
            for (int i = 0; i < 9; i++) {
                Asset asset = new Asset();
                asset.setTenantId(tenantId);
                asset.setName("My asset A" + i);
                asset.setType("typeA");
                assets.add(assetService.saveAsset(asset));
            }
            List<EntitySubtype> assetTypes = assetService.findAssetTypesByTenantId(tenantId).get();
            Assert.assertNotNull(assetTypes);
            Assert.assertEquals(3, assetTypes.size());
            Assert.assertEquals("typeA", assetTypes.get(0).getType());
            Assert.assertEquals("typeB", assetTypes.get(1).getType());
            Assert.assertEquals("typeC", assetTypes.get(2).getType());
        } finally {
            assets.forEach((asset) -> {
                assetService.deleteAsset(tenantId, asset.getId());
            });
        }
    }

    @Test
    public void testDeleteAsset() {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = assetService.saveAsset(asset);
        EntityRelation relation = new EntityRelation(tenantId, savedAsset.getId(), EntityRelation.CONTAINS_TYPE);
        relationService.saveRelation(tenantId, relation);

        Asset foundAsset = assetService.findAssetById(tenantId, savedAsset.getId());
        Assert.assertNotNull(foundAsset);
        assetService.deleteAsset(tenantId, savedAsset.getId());
        foundAsset = assetService.findAssetById(tenantId, savedAsset.getId());
        Assert.assertNull(foundAsset);
        Assert.assertTrue(relationService.findByTo(tenantId, savedAsset.getId(), RelationTypeGroup.COMMON).isEmpty());
    }

    @Test
    public void testFindAssetsByTenantId() {
        List<Asset> assets = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            asset.setName("Asset" + i);
            asset.setType("default");
            assets.add(assetService.saveAsset(asset));
        }

        List<Asset> loadedAssets = new ArrayList<>();
        PageLink pageLink = new PageLink(3);
        PageData<Asset> pageData = null;
        do {
            pageData = assetService.findAssetsByTenantId(tenantId, pageLink);
            loadedAssets.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assets, idComparator);
        Collections.sort(loadedAssets, idComparator);

        Assert.assertEquals(assets, loadedAssets);

        assetService.deleteAssetsByTenantId(tenantId);

        pageLink = new PageLink(4);
        pageData = assetService.findAssetsByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

    @Test
    public void testFindAssetsByTenantIdAndName() {
        String title1 = "Asset title 1";
        List<AssetInfo> assetsTitle1 = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            assetsTitle1.add(new AssetInfo(assetService.saveAsset(asset), null, false, "default"));
        }
        String title2 = "Asset title 2";
        List<AssetInfo> assetsTitle2 = new ArrayList<>();
        for (int i = 0; i < 17; i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            assetsTitle2.add(new AssetInfo(assetService.saveAsset(asset), null, false, "default"));
        }

        List<AssetInfo> loadedAssetsTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(3, 0, title1);
        PageData<AssetInfo> pageData = null;
        do {
            pageData = assetService.findAssetInfosByTenantId(tenantId, pageLink);
            loadedAssetsTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsTitle1, idComparator);
        Collections.sort(loadedAssetsTitle1, idComparator);

        Assert.assertEquals(assetsTitle1, loadedAssetsTitle1);

        List<AssetInfo> loadedAssetsTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = assetService.findAssetInfosByTenantId(tenantId, pageLink);
            loadedAssetsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsTitle2, idComparator);
        Collections.sort(loadedAssetsTitle2, idComparator);

        Assert.assertEquals(assetsTitle2, loadedAssetsTitle2);

        for (Asset asset : loadedAssetsTitle1) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = assetService.findAssetInfosByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Asset asset : loadedAssetsTitle2) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = assetService.findAssetInfosByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindAssetsByTenantIdAndType() {
        String title1 = "Asset title 1";
        String type1 = "typeA";
        List<Asset> assetsType1 = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type1);
            assetsType1.add(assetService.saveAsset(asset));
        }
        String title2 = "Asset title 2";
        String type2 = "typeB";
        List<Asset> assetsType2 = new ArrayList<>();
        for (int i = 0; i < 17; i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type2);
            assetsType2.add(assetService.saveAsset(asset));
        }

        List<Asset> loadedAssetsType1 = new ArrayList<>();
        PageLink pageLink = new PageLink(3);
        PageData<Asset> pageData = null;
        do {
            pageData = assetService.findAssetsByTenantIdAndType(tenantId, type1, pageLink);
            loadedAssetsType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsType1, idComparator);
        Collections.sort(loadedAssetsType1, idComparator);

        Assert.assertEquals(assetsType1, loadedAssetsType1);

        List<Asset> loadedAssetsType2 = new ArrayList<>();
        pageLink = new PageLink(4);
        do {
            pageData = assetService.findAssetsByTenantIdAndType(tenantId, type2, pageLink);
            loadedAssetsType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsType2, idComparator);
        Collections.sort(loadedAssetsType2, idComparator);

        Assert.assertEquals(assetsType2, loadedAssetsType2);

        for (Asset asset : loadedAssetsType1) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new PageLink(4);
        pageData = assetService.findAssetsByTenantIdAndType(tenantId, type1, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Asset asset : loadedAssetsType2) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new PageLink(4);
        pageData = assetService.findAssetsByTenantIdAndType(tenantId, type2, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindAssetsByTenantIdAndCustomerId() {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        List<AssetInfo> assets = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            asset.setName("Asset" + i);
            asset.setType("default");
            asset = assetService.saveAsset(asset);
            assets.add(new AssetInfo(assetService.assignAssetToCustomer(tenantId, asset.getId(), customerId), customer.getTitle(), customer.isPublic(), "default"));
        }

        List<AssetInfo> loadedAssets = new ArrayList<>();
        PageLink pageLink = new PageLink(3);
        PageData<AssetInfo> pageData = null;
        do {
            pageData = assetService.findAssetInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedAssets.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assets, idComparator);
        Collections.sort(loadedAssets, idComparator);

        Assert.assertEquals(assets, loadedAssets);

        assetService.unassignCustomerAssets(tenantId, customerId);

        pageLink = new PageLink(4);
        pageData = assetService.findAssetInfosByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());
    }

    @Test
    public void testFindAssetsByTenantIdCustomerIdAndName() {

        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        String title1 = "Asset title 1";
        List<Asset> assetsTitle1 = new ArrayList<>();
        for (int i = 0; i < 17; i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            asset = assetService.saveAsset(asset);
            assetsTitle1.add(assetService.assignAssetToCustomer(tenantId, asset.getId(), customerId));
        }
        String title2 = "Asset title 2";
        List<Asset> assetsTitle2 = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            asset = assetService.saveAsset(asset);
            assetsTitle2.add(assetService.assignAssetToCustomer(tenantId, asset.getId(), customerId));
        }

        List<Asset> loadedAssetsTitle1 = new ArrayList<>();
        PageLink pageLink = new PageLink(3, 0, title1);
        PageData<Asset> pageData = null;
        do {
            pageData = assetService.findAssetsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedAssetsTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsTitle1, idComparator);
        Collections.sort(loadedAssetsTitle1, idComparator);

        Assert.assertEquals(assetsTitle1, loadedAssetsTitle1);

        List<Asset> loadedAssetsTitle2 = new ArrayList<>();
        pageLink = new PageLink(4, 0, title2);
        do {
            pageData = assetService.findAssetsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedAssetsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsTitle2, idComparator);
        Collections.sort(loadedAssetsTitle2, idComparator);

        Assert.assertEquals(assetsTitle2, loadedAssetsTitle2);

        for (Asset asset : loadedAssetsTitle1) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new PageLink(4, 0, title1);
        pageData = assetService.findAssetsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Asset asset : loadedAssetsTitle2) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new PageLink(4, 0, title2);
        pageData = assetService.findAssetsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
        customerService.deleteCustomer(tenantId, customerId);
    }

    @Test
    public void testFindAssetsByTenantIdCustomerIdAndType() {

        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        String title1 = "Asset title 1";
        String type1 = "typeC";
        List<Asset> assetsType1 = new ArrayList<>();
        for (int i = 0; i < 17; i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title1 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type1);
            asset = assetService.saveAsset(asset);
            assetsType1.add(assetService.assignAssetToCustomer(tenantId, asset.getId(), customerId));
        }
        String title2 = "Asset title 2";
        String type2 = "typeD";
        List<Asset> assetsType2 = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            String suffix = StringUtils.randomAlphanumeric(15);
            String name = title2 + suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type2);
            asset = assetService.saveAsset(asset);
            assetsType2.add(assetService.assignAssetToCustomer(tenantId, asset.getId(), customerId));
        }

        List<Asset> loadedAssetsType1 = new ArrayList<>();
        PageLink pageLink = new PageLink(3);
        PageData<Asset> pageData = null;
        do {
            pageData = assetService.findAssetsByTenantIdAndCustomerIdAndType(tenantId, customerId, type1, pageLink);
            loadedAssetsType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsType1, idComparator);
        Collections.sort(loadedAssetsType1, idComparator);

        Assert.assertEquals(assetsType1, loadedAssetsType1);

        List<Asset> loadedAssetsType2 = new ArrayList<>();
        pageLink = new PageLink(4);
        do {
            pageData = assetService.findAssetsByTenantIdAndCustomerIdAndType(tenantId, customerId, type2, pageLink);
            loadedAssetsType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsType2, idComparator);
        Collections.sort(loadedAssetsType2, idComparator);

        Assert.assertEquals(assetsType2, loadedAssetsType2);

        for (Asset asset : loadedAssetsType1) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new PageLink(4);
        pageData = assetService.findAssetsByTenantIdAndCustomerIdAndType(tenantId, customerId, type1, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Asset asset : loadedAssetsType2) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new PageLink(4);
        pageData = assetService.findAssetsByTenantIdAndCustomerIdAndType(tenantId, customerId, type2, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
        customerService.deleteCustomer(tenantId, customerId);
    }

    @Test
    public void testCleanCacheIfAssetRenamed() {
        String assetNameBeforeRename = StringUtils.randomAlphanumeric(15);
        String assetNameAfterRename = StringUtils.randomAlphanumeric(15);

        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName(assetNameBeforeRename);
        asset.setType("default");
        assetService.saveAsset(asset);

        Asset savedAsset = assetService.findAssetByTenantIdAndName(tenantId, assetNameBeforeRename);

        savedAsset.setName(assetNameAfterRename);
        assetService.saveAsset(savedAsset);

        Asset renamedAsset = assetService.findAssetByTenantIdAndName(tenantId, assetNameBeforeRename);

        Assert.assertNull("Can't find asset by name in cache if it was renamed", renamedAsset);
        assetService.deleteAsset(tenantId, savedAsset.getId());
    }

    @Test
    public void testFindAssetInfoByTenantId() {
        Customer customer = new Customer();
        customer.setTitle("Customer X");
        customer.setTenantId(tenantId);
        Customer savedCustomer = customerService.saveCustomer(customer);

        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("default");
        asset.setType("default");
        asset.setLabel("label");
        asset.setCustomerId(savedCustomer.getId());

        Asset savedAsset = assetService.saveAsset(asset);

        PageLink pageLinkWithLabel = new PageLink(100, 0, "label");
        List<AssetInfo> assetInfosWithLabel = assetService
                .findAssetInfosByTenantId(tenantId, pageLinkWithLabel).getData();

        Assert.assertFalse(assetInfosWithLabel.isEmpty());
        Assert.assertTrue(
                assetInfosWithLabel.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedAsset.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getLabel().equals(savedAsset.getLabel())
                        )
        );

        PageLink pageLinkWithCustomer = new PageLink(100, 0, savedCustomer.getTitle());
        List<AssetInfo> assetInfosWithCustomer = assetService
                .findAssetInfosByTenantId(tenantId, pageLinkWithCustomer).getData();

        Assert.assertFalse(assetInfosWithCustomer.isEmpty());
        Assert.assertTrue(
                assetInfosWithCustomer.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedAsset.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getCustomerId().equals(savedCustomer.getId())
                                        && d.getCustomerTitle().equals(savedCustomer.getTitle())
                        )
        );

        PageLink pageLinkWithType = new PageLink(100, 0, asset.getType());
        List<AssetInfo> assetInfosWithType = assetService
                .findAssetInfosByTenantId(tenantId, pageLinkWithType).getData();

        Assert.assertFalse(assetInfosWithType.isEmpty());
        Assert.assertTrue(
                assetInfosWithType.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedAsset.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getType().equals(asset.getType())
                        )
        );
    }

    @Test
    public void testFindAssetInfoByTenantIdAndType() {
        Customer customer = new Customer();
        customer.setTitle("Customer X");
        customer.setTenantId(tenantId);
        Customer savedCustomer = customerService.saveCustomer(customer);

        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("default");
        asset.setType("default");
        asset.setLabel("label");
        asset.setCustomerId(savedCustomer.getId());
        Asset savedAsset = assetService.saveAsset(asset);

        PageLink pageLinkWithLabel = new PageLink(100, 0, "label");
        List<AssetInfo> assetInfosWithLabel = assetService
                .findAssetInfosByTenantIdAndType(tenantId, asset.getType(), pageLinkWithLabel).getData();

        Assert.assertFalse(assetInfosWithLabel.isEmpty());
        Assert.assertTrue(
                assetInfosWithLabel.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedAsset.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getAssetProfileName().equals(savedAsset.getType())
                                        && d.getLabel().equals(savedAsset.getLabel())
                        )
        );

        PageLink pageLinkWithCustomer = new PageLink(100, 0, savedCustomer.getTitle());
        List<AssetInfo> assetInfosWithCustomer = assetService
                .findAssetInfosByTenantIdAndType(tenantId, asset.getType(), pageLinkWithCustomer).getData();

        Assert.assertFalse(assetInfosWithCustomer.isEmpty());
        Assert.assertTrue(
                assetInfosWithCustomer.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedAsset.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getAssetProfileName().equals(savedAsset.getType())
                                        && d.getCustomerId().equals(savedCustomer.getId())
                                        && d.getCustomerTitle().equals(savedCustomer.getTitle())
                        )
        );
    }

    @Test
    public void testFindAssetInfoByTenantIdAndAssetProfileId() {
        Customer customer = new Customer();
        customer.setTitle("Customer X");
        customer.setTenantId(tenantId);
        Customer savedCustomer = customerService.saveCustomer(customer);

        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("default");
        asset.setLabel("label");
        asset.setCustomerId(savedCustomer.getId());
        Asset savedAsset = assetService.saveAsset(asset);

        PageLink pageLinkWithLabel = new PageLink(100, 0, "label");
        List<AssetInfo> assetInfosWithLabel = assetService
                .findAssetInfosByTenantIdAndAssetProfileId(tenantId, savedAsset.getAssetProfileId(), pageLinkWithLabel).getData();

        Assert.assertFalse(assetInfosWithLabel.isEmpty());
        Assert.assertTrue(
                assetInfosWithLabel.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedAsset.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getAssetProfileId().equals(savedAsset.getAssetProfileId())
                                        && d.getLabel().equals(savedAsset.getLabel())
                        )
        );

        PageLink pageLinkWithCustomer = new PageLink(100, 0, savedCustomer.getTitle());
        List<AssetInfo> assetInfosWithCustomer = assetService
                .findAssetInfosByTenantIdAndAssetProfileId(tenantId, savedAsset.getAssetProfileId(), pageLinkWithCustomer).getData();

        Assert.assertFalse(assetInfosWithCustomer.isEmpty());
        Assert.assertTrue(
                assetInfosWithCustomer.stream()
                        .anyMatch(
                                d -> d.getId().equals(savedAsset.getId())
                                        && d.getTenantId().equals(tenantId)
                                        && d.getAssetProfileId().equals(savedAsset.getAssetProfileId())
                                        && d.getCustomerId().equals(savedCustomer.getId())
                                        && d.getCustomerTitle().equals(savedCustomer.getTitle())
                        )
        );
    }

    @Test
    public void testDeleteAssetIfReferencedInCalculatedField() {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = assetService.saveAsset(asset);

        Asset assetWithCf = new Asset();
        assetWithCf.setTenantId(tenantId);
        assetWithCf.setName("Asset with CF");
        assetWithCf.setType("default");
        Asset savedAssetWithCf = assetService.saveAsset(assetWithCf);

        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setTenantId(tenantId);
        calculatedField.setName("Test CF");
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setEntityId(savedAssetWithCf.getId());

        SimpleCalculatedFieldConfiguration config = new SimpleCalculatedFieldConfiguration();

        Argument argument = new Argument();
        argument.setRefEntityId(savedAsset.getId());
        ReferencedEntityKey refEntityKey = new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null);
        argument.setRefEntityKey(refEntityKey);

        config.setArguments(Map.of("T", argument));

        config.setExpression("T - (100 - H) / 5");

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName("output");

        config.setOutput(output);

        calculatedField.setConfiguration(config);

        CalculatedField savedCalculatedField = calculatedFieldService.save(calculatedField);

        assertThatThrownBy(() -> assetService.deleteAsset(tenantId, savedAsset.getId()))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Can't delete asset that has entity views or is referenced in calculated fields!");

        calculatedFieldService.deleteCalculatedField(tenantId, savedCalculatedField.getId());
    }

}
