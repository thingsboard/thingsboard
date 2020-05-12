/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import com.datastax.driver.core.utils.UUIDs;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.thingsboard.server.dao.model.ModelConstants.NULL_UUID;

public abstract class BaseAssetServiceTest extends AbstractServiceTest {

    private IdComparator<Asset> idComparator = new IdComparator<>();

    private TenantId tenantId;

    @Before
    public void before() {
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = tenantService.saveTenant(tenant);
        Assert.assertNotNull(savedTenant);
        tenantId = savedTenant.getId();
    }

    @After
    public void after() {
        tenantService.deleteTenant(tenantId);
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

    @Test(expected = DataValidationException.class)
    public void testSaveAssetWithEmptyName() {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setType("default");
        assetService.saveAsset(asset);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveAssetWithEmptyTenant() {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        assetService.saveAsset(asset);
    }

    @Test(expected = DataValidationException.class)
    public void testSaveAssetWithInvalidTenant() {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        asset.setTenantId(new TenantId(UUIDs.timeBased()));
        assetService.saveAsset(asset);
    }

    @Test(expected = DataValidationException.class)
    public void testAssignAssetToNonExistentCustomer() {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        asset.setTenantId(tenantId);
        asset = assetService.saveAsset(asset);
        try {
            assetService.assignAssetToCustomer(tenantId, asset.getId(), new CustomerId(UUIDs.timeBased()));
        } finally {
            assetService.deleteAsset(tenantId, asset.getId());
        }
    }

    @Test(expected = DataValidationException.class)
    public void testAssignAssetToCustomerFromDifferentTenant() {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        asset.setTenantId(tenantId);
        asset = assetService.saveAsset(asset);
        Tenant tenant = new Tenant();
        tenant.setTitle("Test different tenant");
        tenant = tenantService.saveTenant(tenant);
        Customer customer = new Customer();
        customer.setTenantId(tenant.getId());
        customer.setTitle("Test different customer");
        customer = customerService.saveCustomer(customer);
        try {
            assetService.assignAssetToCustomer(tenantId, asset.getId(), customer.getId());
        } finally {
            assetService.deleteAsset(tenantId, asset.getId());
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
            for (int i=0;i<3;i++) {
                Asset asset = new Asset();
                asset.setTenantId(tenantId);
                asset.setName("My asset B"+i);
                asset.setType("typeB");
                assets.add(assetService.saveAsset(asset));
            }
            for (int i=0;i<7;i++) {
                Asset asset = new Asset();
                asset.setTenantId(tenantId);
                asset.setName("My asset C"+i);
                asset.setType("typeC");
                assets.add(assetService.saveAsset(asset));
            }
            for (int i=0;i<9;i++) {
                Asset asset = new Asset();
                asset.setTenantId(tenantId);
                asset.setName("My asset A"+i);
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
            assets.forEach((asset) -> { assetService.deleteAsset(tenantId, asset.getId()); });
        }
    }

    @Test
    public void testDeleteAsset() {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = assetService.saveAsset(asset);
        Asset foundAsset = assetService.findAssetById(tenantId, savedAsset.getId());
        Assert.assertNotNull(foundAsset);
        assetService.deleteAsset(tenantId, savedAsset.getId());
        foundAsset = assetService.findAssetById(tenantId, savedAsset.getId());
        Assert.assertNull(foundAsset);
    }

    @Test
    public void testFindAssetsByTenantId() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        List<Asset> assets = new ArrayList<>();
        for (int i=0;i<178;i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            asset.setName("Asset"+i);
            asset.setType("default");
            assets.add(assetService.saveAsset(asset));
        }

        List<Asset> loadedAssets = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(23);
        TextPageData<Asset> pageData = null;
        do {
            pageData = assetService.findAssetsByTenantId(tenantId, pageLink);
            loadedAssets.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assets, idComparator);
        Collections.sort(loadedAssets, idComparator);

        Assert.assertEquals(assets, loadedAssets);

        assetService.deleteAssetsByTenantId(tenantId);

        pageLink = new TextPageLink(33);
        pageData = assetService.findAssetsByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

        tenantService.deleteTenant(tenantId);
    }

    @Test
    public void testFindAssetsByTenantIdAndName() {
        String title1 = "Asset title 1";
        List<Asset> assetsTitle1 = new ArrayList<>();
        for (int i=0;i<143;i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title1+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            assetsTitle1.add(assetService.saveAsset(asset));
        }
        String title2 = "Asset title 2";
        List<Asset> assetsTitle2 = new ArrayList<>();
        for (int i=0;i<175;i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title2+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            assetsTitle2.add(assetService.saveAsset(asset));
        }

        List<Asset> loadedAssetsTitle1 = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(15, title1);
        TextPageData<Asset> pageData = null;
        do {
            pageData = assetService.findAssetsByTenantId(tenantId, pageLink);
            loadedAssetsTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsTitle1, idComparator);
        Collections.sort(loadedAssetsTitle1, idComparator);

        Assert.assertEquals(assetsTitle1, loadedAssetsTitle1);

        List<Asset> loadedAssetsTitle2 = new ArrayList<>();
        pageLink = new TextPageLink(4, title2);
        do {
            pageData = assetService.findAssetsByTenantId(tenantId, pageLink);
            loadedAssetsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsTitle2, idComparator);
        Collections.sort(loadedAssetsTitle2, idComparator);

        Assert.assertEquals(assetsTitle2, loadedAssetsTitle2);

        for (Asset asset : loadedAssetsTitle1) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new TextPageLink(4, title1);
        pageData = assetService.findAssetsByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Asset asset : loadedAssetsTitle2) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new TextPageLink(4, title2);
        pageData = assetService.findAssetsByTenantId(tenantId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindAssetsByTenantIdAndType() {
        String title1 = "Asset title 1";
        String type1 = "typeA";
        List<Asset> assetsType1 = new ArrayList<>();
        for (int i=0;i<143;i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title1+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type1);
            assetsType1.add(assetService.saveAsset(asset));
        }
        String title2 = "Asset title 2";
        String type2 = "typeB";
        List<Asset> assetsType2 = new ArrayList<>();
        for (int i=0;i<175;i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title2+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type2);
            assetsType2.add(assetService.saveAsset(asset));
        }

        List<Asset> loadedAssetsType1 = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(15);
        TextPageData<Asset> pageData = null;
        do {
            pageData = assetService.findAssetsByTenantIdAndType(tenantId, type1, pageLink);
            loadedAssetsType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsType1, idComparator);
        Collections.sort(loadedAssetsType1, idComparator);

        Assert.assertEquals(assetsType1, loadedAssetsType1);

        List<Asset> loadedAssetsType2 = new ArrayList<>();
        pageLink = new TextPageLink(4);
        do {
            pageData = assetService.findAssetsByTenantIdAndType(tenantId, type2, pageLink);
            loadedAssetsType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsType2, idComparator);
        Collections.sort(loadedAssetsType2, idComparator);

        Assert.assertEquals(assetsType2, loadedAssetsType2);

        for (Asset asset : loadedAssetsType1) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new TextPageLink(4);
        pageData = assetService.findAssetsByTenantIdAndType(tenantId, type1, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Asset asset : loadedAssetsType2) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new TextPageLink(4);
        pageData = assetService.findAssetsByTenantIdAndType(tenantId, type2, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindAssetsByTenantIdAndCustomerId() {
        Tenant tenant = new Tenant();
        tenant.setTitle("Test tenant");
        tenant = tenantService.saveTenant(tenant);

        TenantId tenantId = tenant.getId();

        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer.setTenantId(tenantId);
        customer = customerService.saveCustomer(customer);
        CustomerId customerId = customer.getId();

        List<Asset> assets = new ArrayList<>();
        for (int i=0;i<278;i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            asset.setName("Asset"+i);
            asset.setType("default");
            asset = assetService.saveAsset(asset);
            assets.add(assetService.assignAssetToCustomer(tenantId, asset.getId(), customerId));
        }

        List<Asset> loadedAssets = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(23);
        TextPageData<Asset> pageData = null;
        do {
            pageData = assetService.findAssetsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedAssets.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assets, idComparator);
        Collections.sort(loadedAssets, idComparator);

        Assert.assertEquals(assets, loadedAssets);

        assetService.unassignCustomerAssets(tenantId, customerId);

        pageLink = new TextPageLink(33);
        pageData = assetService.findAssetsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertTrue(pageData.getData().isEmpty());

        tenantService.deleteTenant(tenantId);
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
        for (int i=0;i<175;i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title1+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            asset = assetService.saveAsset(asset);
            assetsTitle1.add(assetService.assignAssetToCustomer(tenantId, asset.getId(), customerId));
        }
        String title2 = "Asset title 2";
        List<Asset> assetsTitle2 = new ArrayList<>();
        for (int i=0;i<143;i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title2+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            asset = assetService.saveAsset(asset);
            assetsTitle2.add(assetService.assignAssetToCustomer(tenantId, asset.getId(), customerId));
        }

        List<Asset> loadedAssetsTitle1 = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(15, title1);
        TextPageData<Asset> pageData = null;
        do {
            pageData = assetService.findAssetsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedAssetsTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsTitle1, idComparator);
        Collections.sort(loadedAssetsTitle1, idComparator);

        Assert.assertEquals(assetsTitle1, loadedAssetsTitle1);

        List<Asset> loadedAssetsTitle2 = new ArrayList<>();
        pageLink = new TextPageLink(4, title2);
        do {
            pageData = assetService.findAssetsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
            loadedAssetsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsTitle2, idComparator);
        Collections.sort(loadedAssetsTitle2, idComparator);

        Assert.assertEquals(assetsTitle2, loadedAssetsTitle2);

        for (Asset asset : loadedAssetsTitle1) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new TextPageLink(4, title1);
        pageData = assetService.findAssetsByTenantIdAndCustomerId(tenantId, customerId, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Asset asset : loadedAssetsTitle2) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new TextPageLink(4, title2);
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
        for (int i=0;i<175;i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title1+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type1);
            asset = assetService.saveAsset(asset);
            assetsType1.add(assetService.assignAssetToCustomer(tenantId, asset.getId(), customerId));
        }
        String title2 = "Asset title 2";
        String type2 = "typeD";
        List<Asset> assetsType2 = new ArrayList<>();
        for (int i=0;i<143;i++) {
            Asset asset = new Asset();
            asset.setTenantId(tenantId);
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title2+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type2);
            asset = assetService.saveAsset(asset);
            assetsType2.add(assetService.assignAssetToCustomer(tenantId, asset.getId(), customerId));
        }

        List<Asset> loadedAssetsType1 = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(15);
        TextPageData<Asset> pageData = null;
        do {
            pageData = assetService.findAssetsByTenantIdAndCustomerIdAndType(tenantId, customerId, type1, pageLink);
            loadedAssetsType1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsType1, idComparator);
        Collections.sort(loadedAssetsType1, idComparator);

        Assert.assertEquals(assetsType1, loadedAssetsType1);

        List<Asset> loadedAssetsType2 = new ArrayList<>();
        pageLink = new TextPageLink(4);
        do {
            pageData = assetService.findAssetsByTenantIdAndCustomerIdAndType(tenantId, customerId, type2, pageLink);
            loadedAssetsType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsType2, idComparator);
        Collections.sort(loadedAssetsType2, idComparator);

        Assert.assertEquals(assetsType2, loadedAssetsType2);

        for (Asset asset : loadedAssetsType1) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new TextPageLink(4);
        pageData = assetService.findAssetsByTenantIdAndCustomerIdAndType(tenantId, customerId, type1, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Asset asset : loadedAssetsType2) {
            assetService.deleteAsset(tenantId, asset.getId());
        }

        pageLink = new TextPageLink(4);
        pageData = assetService.findAssetsByTenantIdAndCustomerIdAndType(tenantId, customerId, type2, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
        customerService.deleteCustomer(tenantId, customerId);
    }

}
