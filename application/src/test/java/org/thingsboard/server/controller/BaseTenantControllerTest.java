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

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.page.TextPageLink;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;

public abstract class BaseTenantControllerTest extends AbstractControllerTest {
    
    private IdComparator<Tenant> idComparator = new IdComparator<>();

    @Test
    public void testSaveTenant() throws Exception {
        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Assert.assertNotNull(savedTenant);
        Assert.assertNotNull(savedTenant.getId());
        Assert.assertTrue(savedTenant.getCreatedTime() > 0);
        Assert.assertEquals(tenant.getTitle(), savedTenant.getTitle());
        savedTenant.setTitle("My new tenant");
        doPost("/api/tenant", savedTenant, Tenant.class);
        Tenant foundTenant = doGet("/api/tenant/"+savedTenant.getId().getId().toString(), Tenant.class); 
        Assert.assertEquals(foundTenant.getTitle(), savedTenant.getTitle());
        doDelete("/api/tenant/"+savedTenant.getId().getId().toString())
        .andExpect(status().isOk());
    }
    
    @Test
    public void testFindTenantById() throws Exception {
        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        Tenant foundTenant = doGet("/api/tenant/"+savedTenant.getId().getId().toString(), Tenant.class); 
        Assert.assertNotNull(foundTenant);
        Assert.assertEquals(savedTenant, foundTenant);
        doDelete("/api/tenant/"+savedTenant.getId().getId().toString())
        .andExpect(status().isOk());
    }
    
    @Test
    public void testSaveTenantWithEmptyTitle() throws Exception {
        loginSysAdmin();
        Tenant tenant = new Tenant();
        doPost("/api/tenant", tenant)
        .andExpect(status().isBadRequest())
        .andExpect(statusReason(containsString("Tenant title should be specified")));
    }
    
    @Test
    public void testSaveTenantWithInvalidEmail() throws Exception {
        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        tenant.setEmail("invalid@mail");
        doPost("/api/tenant", tenant)
        .andExpect(status().isBadRequest())
        .andExpect(statusReason(containsString("Invalid email address format")));
    }
    
    @Test
    public void testDeleteTenant() throws Exception {
        loginSysAdmin();
        Tenant tenant = new Tenant();
        tenant.setTitle("My tenant");
        Tenant savedTenant = doPost("/api/tenant", tenant, Tenant.class);
        doDelete("/api/tenant/"+savedTenant.getId().getId().toString())
        .andExpect(status().isOk());        
        doGet("/api/tenant/"+savedTenant.getId().getId().toString())
        .andExpect(status().isNotFound());
    }
    
    @Test
    public void testFindTenants() throws Exception {
        loginSysAdmin();
        List<Tenant> tenants = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(17);
        TextPageData<Tenant> pageData = doGetTypedWithPageLink("/api/tenants?", new TypeReference<TextPageData<Tenant>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getData().size());
        tenants.addAll(pageData.getData());

        for (int i=0;i<56;i++) {
            Tenant tenant = new Tenant();
            tenant.setTitle("Tenant"+i);
            tenants.add(doPost("/api/tenant", tenant, Tenant.class));
        }
        
        List<Tenant> loadedTenants = new ArrayList<>();
        pageLink = new TextPageLink(17);
        do {
            pageData = doGetTypedWithPageLink("/api/tenants?", new TypeReference<TextPageData<Tenant>>(){}, pageLink);
            loadedTenants.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());
        
        Collections.sort(tenants, idComparator);
        Collections.sort(loadedTenants, idComparator);
        
        Assert.assertEquals(tenants, loadedTenants);
        
        for (Tenant tenant : loadedTenants) {
            if (!tenant.getTitle().equals(TEST_TENANT_NAME)) {
                doDelete("/api/tenant/"+tenant.getId().getId().toString())
                .andExpect(status().isOk());        
            }
        }
        
        pageLink = new TextPageLink(17);
        pageData =  doGetTypedWithPageLink("/api/tenants?", new TypeReference<TextPageData<Tenant>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getData().size());
    }
    
    @Test
    public void testFindTenantsByTitle() throws Exception {
        loginSysAdmin();
        String title1 = "Tenant title 1";
        List<Tenant> tenantsTitle1 = new ArrayList<>();
        for (int i=0;i<134;i++) {
            Tenant tenant = new Tenant();
            String suffix = RandomStringUtils.randomAlphanumeric((int)(5 + Math.random()*10));
            String title = title1+suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            tenant.setTitle(title);
            tenantsTitle1.add(doPost("/api/tenant", tenant, Tenant.class));
        }
        String title2 = "Tenant title 2";
        List<Tenant> tenantsTitle2 = new ArrayList<>();
        for (int i=0;i<127;i++) {
            Tenant tenant = new Tenant();
            String suffix = RandomStringUtils.randomAlphanumeric((int)(5 + Math.random()*10));
            String title = title2+suffix;
            title = i % 2 == 0 ? title.toLowerCase() : title.toUpperCase();
            tenant.setTitle(title);
            tenantsTitle2.add(doPost("/api/tenant", tenant, Tenant.class));
        }
        
        List<Tenant> loadedTenantsTitle1 = new ArrayList<>();
        TextPageLink pageLink = new TextPageLink(15, title1);
        TextPageData<Tenant> pageData = null;
        do {
            pageData = doGetTypedWithPageLink("/api/tenants?", new TypeReference<TextPageData<Tenant>>(){}, pageLink);
            loadedTenantsTitle1.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());
        
        Collections.sort(tenantsTitle1, idComparator);
        Collections.sort(loadedTenantsTitle1, idComparator);
        
        Assert.assertEquals(tenantsTitle1, loadedTenantsTitle1);
        
        List<Tenant> loadedTenantsTitle2 = new ArrayList<>();
        pageLink = new TextPageLink(4, title2);
        do {
            pageData = doGetTypedWithPageLink("/api/tenants?", new TypeReference<TextPageData<Tenant>>(){}, pageLink);
            loadedTenantsTitle2.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageData.getNextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(tenantsTitle2, idComparator);
        Collections.sort(loadedTenantsTitle2, idComparator);
        
        Assert.assertEquals(tenantsTitle2, loadedTenantsTitle2);

        for (Tenant tenant : loadedTenantsTitle1) {
            doDelete("/api/tenant/"+tenant.getId().getId().toString())
            .andExpect(status().isOk());        
        }
        
        pageLink = new TextPageLink(4, title1);
        pageData = doGetTypedWithPageLink("/api/tenants?", new TypeReference<TextPageData<Tenant>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
        
        for (Tenant tenant : loadedTenantsTitle2) {
            doDelete("/api/tenant/"+tenant.getId().getId().toString())
            .andExpect(status().isOk());     
        }
        
        pageLink = new TextPageLink(4, title2);
        pageData = doGetTypedWithPageLink("/api/tenants?", new TypeReference<TextPageData<Tenant>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(0, pageData.getData().size());
    }
}
