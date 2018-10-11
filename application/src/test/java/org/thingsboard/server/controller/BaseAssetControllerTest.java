/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.controller;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.*;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.thingsboard.server.common.data.security.Authority;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseAssetControllerTest extends AbstractControllerTest {

    private IdComparator<Asset> idComparator = new IdComparator<>();

    private Tenant savedTenant;
    private User tenantAdmin;

    @Before
    public void beforeTest() throws Exception {
        loginSysAdmin();

        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);

        tenantAdmin = new User();
        tenantAdmin.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin.setTenantId(savedTenant.getId());
        tenantAdmin.setEmail("tenant2@thingsboard.org");
        tenantAdmin.setFirstName("Joe");
        tenantAdmin.setLastName("Downs");

        tenantAdmin = createUserAndLogin(tenantAdmin, "testPassword1");
    }

    @After
    public void afterTest() throws Exception {
        loginSysAdmin();

        doDelete("/api/tenant/"+savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveAsset() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        Assert.assertNotNull(savedAsset);
        Assert.assertNotNull(savedAsset.getId());
        Assert.assertTrue(savedAsset.getCreatedTime() > 0);
        Assert.assertEquals(savedTenant.getId(), savedAsset.getTenantId());
        Assert.assertTrue(savedAsset.getAssignedCustomers().isEmpty());
        Assert.assertEquals(asset.getName(), savedAsset.getName());

        savedAsset.setName("My new asset");
        doPost("/api/asset", savedAsset, Asset.class);

        Asset foundAsset = doGet("/api/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        Assert.assertEquals(foundAsset.getName(), savedAsset.getName());
    }

    @Test
    public void testFindAssetById() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);
        Asset foundAsset = doGet("/api/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        Assert.assertNotNull(foundAsset);
        Assert.assertEquals(savedAsset, foundAsset);
    }

    @Test
    public void testFindAssetTypesByTenantId() throws Exception {
        List<Asset> assets = new ArrayList<>();
        for (int i=0;i<3;i++) {
            Asset asset = new Asset();
            asset.setName("My asset B"+i);
            asset.setType("typeB");
            assets.add(doPost("/api/asset", asset, Asset.class));
        }
        for (int i=0;i<7;i++) {
            Asset asset = new Asset();
            asset.setName("My asset C"+i);
            asset.setType("typeC");
            assets.add(doPost("/api/asset", asset, Asset.class));
        }
        for (int i=0;i<9;i++) {
            Asset asset = new Asset();
            asset.setName("My asset A"+i);
            asset.setType("typeA");
            assets.add(doPost("/api/asset", asset, Asset.class));
        }
        List<EntitySubtype> assetTypes = doGetTyped("/api/asset/types",
                new TypeReference<List<EntitySubtype>>(){});

        Assert.assertNotNull(assetTypes);
        Assert.assertEquals(3, assetTypes.size());
        Assert.assertEquals("typeA", assetTypes.get(0).getType());
        Assert.assertEquals("typeB", assetTypes.get(1).getType());
        Assert.assertEquals("typeC", assetTypes.get(2).getType());
    }

    @Test
    public void testDeleteAsset() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        doDelete("/api/asset/"+savedAsset.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/asset/"+savedAsset.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testSaveAssetWithEmptyType() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");
        doPost("/api/asset", asset)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Asset type should be specified")));
    }

    @Test
    public void testSaveAssetWithEmptyName() throws Exception {
        Asset asset = new Asset();
        asset.setType("default");
        doPost("/api/asset", asset)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Asset name should be specified")));
    }

    @Test
    public void testAssignUnassignAssetToCustomer() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        Customer customer = new Customer();
        customer.setTitle("My customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        Asset assignedAsset = doPost("/api/customer/" + savedCustomer.getId().getId().toString()
                + "/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        List<CustomerId> customerIds = assignedAsset.getAssignedCustomers().stream().map(ShortCustomerInfo::getCustomerId).collect(Collectors.toList());
        Assert.assertTrue(customerIds.contains(savedCustomer.getId()));

        Asset foundAsset = doGet("/api/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        customerIds.clear();
        customerIds = foundAsset.getAssignedCustomers().stream().map(ShortCustomerInfo::getCustomerId).collect(Collectors.toList());
        Assert.assertTrue(customerIds.contains(savedCustomer.getId()));

        Asset unassignedAsset =
                doDelete("/api/customer/" + savedCustomer.getId().getId().toString() + "/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        customerIds.clear();
        customerIds = unassignedAsset.getAssignedCustomers().stream().map(ShortCustomerInfo::getCustomerId).collect(Collectors.toList());
        Assert.assertFalse(customerIds.contains(savedCustomer.getId()));

        foundAsset = doGet("/api/asset/" + savedAsset.getId().getId().toString(), Asset.class);
        customerIds.clear();
        customerIds = foundAsset.getAssignedCustomers().stream().map(ShortCustomerInfo::getCustomerId).collect(Collectors.toList());
        Assert.assertFalse(customerIds.contains(savedCustomer.getId()));
    }

    @Test
    public void testAssignAssetToNonExistentCustomer() throws Exception {
        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        doPost("/api/customer/" + UUIDs.timeBased().toString()
                + "/asset/" + savedAsset.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testAssignAssetToCustomerFromDifferentTenant() throws Exception {
        loginSysAdmin();

        Tenant tenant2 = new Tenant();
        tenant2.setTitle("Different tenant");
        Tenant savedTenant2 = doPost("/api/tenant", tenant2, Tenant.class);
        Assert.assertNotNull(savedTenant2);

        User tenantAdmin2 = new User();
        tenantAdmin2.setAuthority(Authority.TENANT_ADMIN);
        tenantAdmin2.setTenantId(savedTenant2.getId());
        tenantAdmin2.setEmail("tenant3@thingsboard.org");
        tenantAdmin2.setFirstName("Joe");
        tenantAdmin2.setLastName("Downs");

        tenantAdmin2 = createUserAndLogin(tenantAdmin2, "testPassword1");

        Customer customer = new Customer();
        customer.setTitle("Different customer");
        Customer savedCustomer = doPost("/api/customer", customer, Customer.class);

        login(tenantAdmin.getEmail(), "testPassword1");

        Asset asset = new Asset();
        asset.setName("My asset");
        asset.setType("default");
        Asset savedAsset = doPost("/api/asset", asset, Asset.class);

        doPost("/api/customer/" + savedCustomer.getId().getId().toString()
                + "/asset/" + savedAsset.getId().getId().toString())
                .andExpect(status().isForbidden());

        loginSysAdmin();

        doDelete("/api/tenant/"+savedTenant2.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testFindTenantAssets() throws Exception {
        List<Asset> assets = new ArrayList<>();
        for (int i=0;i<178;i++) {
            Asset asset = new Asset();
            asset.setName("Asset"+i);
            asset.setType("default");
            assets.add(doPost("/api/asset", asset, Asset.class));
        }
        List<Asset> loadedAssets = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(23);
        TextPageData<Asset> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/assets?",
                    new TypeReference<TextPageData<Asset>>(){}, pageLink);
            loadedAssets.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assets, idComparator);
        Collections.sort(loadedAssets, idComparator);

        Assert.assertEquals(assets, loadedAssets);
    }

    @Test
    public void testFindTenantAssetsByName() throws Exception {
        String title1 = "Asset title 1";
        List<Asset> assetsTitle1 = new ArrayList<>();
        for (int i=0;i<143;i++) {
            Asset asset = new Asset();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title1+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            assetsTitle1.add(doPost("/api/asset", asset, Asset.class));
        }
        String title2 = "Asset title 2";
        List<Asset> assetsTitle2 = new ArrayList<>();
        for (int i=0;i<75;i++) {
            Asset asset = new Asset();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title2+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            assetsTitle2.add(doPost("/api/asset", asset, Asset.class));
        }

        List<Asset> loadedAssetsTitle1 = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(15, title1);
        TextPageData<Asset> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/assets?",
                    new TypeReference<TextPageData<Asset>>(){}, pageLink);
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
            pageData = doGetTypedWithPageLink("/api/tenant/assets?",
                    new TypeReference<TextPageData<Asset>>(){}, pageLink);
            loadedAssetsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsTitle2, idComparator);
        Collections.sort(loadedAssetsTitle2, idComparator);

        Assert.assertEquals(assetsTitle2, loadedAssetsTitle2);

        for (Asset asset : loadedAssetsTitle1) {
            doDelete("/api/asset/"+asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new TextPageLink(4, title1);
        pageData = doGetTypedWithPageLink("/api/tenant/assets?",
                new TypeReference<TextPageData<Asset>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Asset asset : loadedAssetsTitle2) {
            doDelete("/api/asset/"+asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new TextPageLink(4, title2);
        pageData = doGetTypedWithPageLink("/api/tenant/assets?",
                new TypeReference<TextPageData<Asset>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindTenantAssetsByType() throws Exception {
        String title1 = "Asset title 1";
        String type1 = "typeA";
        List<Asset> assetsType1 = new ArrayList<>();
        for (int i=0;i<143;i++) {
            Asset asset = new Asset();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title1+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type1);
            assetsType1.add(doPost("/api/asset", asset, Asset.class));
        }
        String title2 = "Asset title 2";
        String type2 = "typeB";
        List<Asset> assetsType2 = new ArrayList<>();
        for (int i=0;i<75;i++) {
            Asset asset = new Asset();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title2+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type2);
            assetsType2.add(doPost("/api/asset", asset, Asset.class));
        }

        List<Asset> loadedAssetsType1 = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(15);
        TextPageData<Asset> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenant/assets?type={type}&",
                    new TypeReference<TextPageData<Asset>>(){}, pageLink, type1);
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
            pageData = doGetTypedWithPageLink("/api/tenant/assets?type={type}&",
                    new TypeReference<TextPageData<Asset>>(){}, pageLink, type2);
            loadedAssetsType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsType2, idComparator);
        Collections.sort(loadedAssetsType2, idComparator);

        Assert.assertEquals(assetsType2, loadedAssetsType2);

        for (Asset asset : loadedAssetsType1) {
            doDelete("/api/asset/"+asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new TextPageLink(4);
        pageData = doGetTypedWithPageLink("/api/tenant/assets?type={type}&",
                new TypeReference<TextPageData<Asset>>(){}, pageLink, type1);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Asset asset : loadedAssetsType2) {
            doDelete("/api/asset/"+asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new TextPageLink(4);
        pageData = doGetTypedWithPageLink("/api/tenant/assets?type={type}&",
                new TypeReference<TextPageData<Asset>>(){}, pageLink, type2);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindCustomerAssets() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer = doPost("/api/customer", customer, Customer.class);
        CustomerId customerId = customer.getId();

        List<Asset> assets = new ArrayList<>();
        for (int i=0;i<128;i++) {
            Asset asset = new Asset();
            asset.setName("Asset"+i);
            asset.setType("default");
            asset = doPost("/api/asset", asset, Asset.class);
            assets.add(doPost("/api/customer/" + customerId.getId().toString()
                    + "/asset/" + asset.getId().getId().toString(), Asset.class));
        }

        List<Asset> loadedAssets = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(23);
        TextPageData<Asset> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?",
                    new TypeReference<TextPageData<Asset>>(){}, pageLink);
            loadedAssets.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assets, idComparator);
        Collections.sort(loadedAssets, idComparator);

        Assert.assertEquals(assets, loadedAssets);
    }

    @Test
    public void testFindCustomerAssetsByName() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer = doPost("/api/customer", customer, Customer.class);
        CustomerId customerId = customer.getId();

        String title1 = "Asset title 1";
        List<Asset> assetsTitle1 = new ArrayList<>();
        for (int i=0;i<125;i++) {
            Asset asset = new Asset();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title1+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            asset = doPost("/api/asset", asset, Asset.class);
            assetsTitle1.add(doPost("/api/customer/" + customerId.getId().toString()
                    + "/asset/" + asset.getId().getId().toString(), Asset.class));
        }
        String title2 = "Asset title 2";
        List<Asset> assetsTitle2 = new ArrayList<>();
        for (int i=0;i<143;i++) {
            Asset asset = new Asset();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title2+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType("default");
            asset = doPost("/api/asset", asset, Asset.class);
            assetsTitle2.add(doPost("/api/customer/" + customerId.getId().toString()
                    + "/asset/" + asset.getId().getId().toString(), Asset.class));
        }

        List<Asset> loadedAssetsTitle1 = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(15, title1);
        TextPageData<Asset> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?",
                    new TypeReference<TextPageData<Asset>>(){}, pageLink);
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
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?",
                    new TypeReference<TextPageData<Asset>>(){}, pageLink);
            loadedAssetsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsTitle2, idComparator);
        Collections.sort(loadedAssetsTitle2, idComparator);

        Assert.assertEquals(assetsTitle2, loadedAssetsTitle2);

        for (Asset asset : loadedAssetsTitle1) {
            doDelete("/api/customer/" + customerId.getId().toString() + "/asset/" + asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new TextPageLink(4, title1);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?",
                new TypeReference<TextPageData<Asset>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Asset asset : loadedAssetsTitle2) {
            doDelete("/api/customer/" + customerId.getId().toString() + "/asset/" + asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new TextPageLink(4, title2);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?",
                new TypeReference<TextPageData<Asset>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

    @Test
    public void testFindCustomerAssetsByType() throws Exception {
        Customer customer = new Customer();
        customer.setTitle("Test customer");
        customer = doPost("/api/customer", customer, Customer.class);
        CustomerId customerId = customer.getId();

        String title1 = "Asset title 1";
        String type1 = "typeC";
        List<Asset> assetsType1 = new ArrayList<>();
        for (int i=0;i<125;i++) {
            Asset asset = new Asset();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title1+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type1);
            asset = doPost("/api/asset", asset, Asset.class);
            assetsType1.add(doPost("/api/customer/" + customerId.getId().toString()
                    + "/asset/" + asset.getId().getId().toString(), Asset.class));
        }
        String title2 = "Asset title 2";
        String type2 = "typeD";
        List<Asset> assetsType2 = new ArrayList<>();
        for (int i=0;i<143;i++) {
            Asset asset = new Asset();
            String suffix = RandomStringUtils.randomAlphanumeric(15);
            String name = title2+suffix;
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            asset.setName(name);
            asset.setType(type2);
            asset = doPost("/api/asset", asset, Asset.class);
            assetsType2.add(doPost("/api/customer/" + customerId.getId().toString()
                    + "/asset/" + asset.getId().getId().toString(), Asset.class));
        }

        List<Asset> loadedAssetsType1 = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(15);
        TextPageData<Asset> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?type={type}&",
                    new TypeReference<TextPageData<Asset>>(){}, pageLink, type1);
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
            pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?type={type}&",
                    new TypeReference<TextPageData<Asset>>(){}, pageLink, type2);
            loadedAssetsType2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(assetsType2, idComparator);
        Collections.sort(loadedAssetsType2, idComparator);

        Assert.assertEquals(assetsType2, loadedAssetsType2);

        for (Asset asset : loadedAssetsType1) {
            doDelete("/api/customer/" + customerId.getId().toString() + "/asset/" + asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new TextPageLink(4);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?type={type}&",
                new TypeReference<TextPageData<Asset>>(){}, pageLink, type1);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());

        for (Asset asset : loadedAssetsType2) {
            doDelete("/api/customer/" + customerId.getId().toString() + "/asset/" + asset.getId().getId().toString())
                    .andExpect(status().isOk());
        }

        pageLink = new TextPageLink(4);
        pageData = doGetTypedWithPageLink("/api/customer/" + customerId.getId().toString() + "/assets?type={type}&",
                new TypeReference<TextPageData<Asset>>(){}, pageLink, type2);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }

}
