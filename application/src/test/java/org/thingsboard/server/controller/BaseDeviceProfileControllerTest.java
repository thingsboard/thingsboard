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
package org.thingsboard.server.controller;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityInfo;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.util.mapping.JacksonUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public abstract class BaseDeviceProfileControllerTest extends AbstractControllerTest {

    private IdComparator<DeviceProfile> idComparator = new IdComparator<>();
    private IdComparator<EntityInfo> deviceProfileInfoIdComparator = new IdComparator<>();

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

        doDelete("/api/tenant/" + savedTenant.getId().getId().toString())
                .andExpect(status().isOk());
    }

    @Test
    public void testSaveDeviceProfile() throws Exception {
        DeviceProfile deviceProfile = this.createDeviceProfile("Device Profile");
        DeviceProfile savedDeviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        Assert.assertNotNull(savedDeviceProfile);
        Assert.assertNotNull(savedDeviceProfile.getId());
        Assert.assertTrue(savedDeviceProfile.getCreatedTime() > 0);
        Assert.assertEquals(deviceProfile.getName(), savedDeviceProfile.getName());
        Assert.assertEquals(deviceProfile.getDescription(), savedDeviceProfile.getDescription());
        Assert.assertEquals(deviceProfile.getProfileData(), savedDeviceProfile.getProfileData());
        Assert.assertEquals(deviceProfile.isDefault(), savedDeviceProfile.isDefault());
        Assert.assertEquals(deviceProfile.getDefaultRuleChainId(), savedDeviceProfile.getDefaultRuleChainId());
        savedDeviceProfile.setName("New device profile");
        doPost("/api/deviceProfile", savedDeviceProfile, DeviceProfile.class);
        DeviceProfile foundDeviceProfile = doGet("/api/deviceProfile/"+savedDeviceProfile.getId().getId().toString(), DeviceProfile.class);
        Assert.assertEquals(savedDeviceProfile.getName(), foundDeviceProfile.getName());
    }

    @Test
    public void testFindDeviceProfileById() throws Exception {
        DeviceProfile deviceProfile = this.createDeviceProfile("Device Profile");
        DeviceProfile savedDeviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        DeviceProfile foundDeviceProfile = doGet("/api/deviceProfile/"+savedDeviceProfile.getId().getId().toString(), DeviceProfile.class);
        Assert.assertNotNull(foundDeviceProfile);
        Assert.assertEquals(savedDeviceProfile, foundDeviceProfile);
    }

    @Test
    public void testFindDeviceProfileInfoById() throws Exception {
        DeviceProfile deviceProfile = this.createDeviceProfile("Device Profile");
        DeviceProfile savedDeviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        EntityInfo foundDeviceProfileInfo = doGet("/api/deviceProfileInfo/"+savedDeviceProfile.getId().getId().toString(), EntityInfo.class);
        Assert.assertNotNull(foundDeviceProfileInfo);
        Assert.assertEquals(savedDeviceProfile.getId(), foundDeviceProfileInfo.getId());
        Assert.assertEquals(savedDeviceProfile.getName(), foundDeviceProfileInfo.getName());
    }

    @Test
    public void testFindDefaultDeviceProfileInfo() throws Exception {
        EntityInfo foundDefaultDeviceProfileInfo = doGet("/api/deviceProfileInfo/default", EntityInfo.class);
        Assert.assertNotNull(foundDefaultDeviceProfileInfo);
        Assert.assertNotNull(foundDefaultDeviceProfileInfo.getId());
        Assert.assertNotNull(foundDefaultDeviceProfileInfo.getName());
        Assert.assertEquals("Default", foundDefaultDeviceProfileInfo.getName());
    }

    @Test
    public void testSetDefaultDeviceProfile() throws Exception {
        DeviceProfile deviceProfile = this.createDeviceProfile("Device Profile 1");
        DeviceProfile savedDeviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);
        DeviceProfile defaultDeviceProfile = doPost("/api/deviceProfile/"+savedDeviceProfile.getId().getId().toString()+"/default", null, DeviceProfile.class);
        Assert.assertNotNull(defaultDeviceProfile);
        EntityInfo foundDefaultDeviceProfile = doGet("/api/deviceProfileInfo/default", EntityInfo.class);
        Assert.assertNotNull(foundDefaultDeviceProfile);
        Assert.assertEquals(savedDeviceProfile.getName(), foundDefaultDeviceProfile.getName());
        Assert.assertEquals(savedDeviceProfile.getId(), foundDefaultDeviceProfile.getId());
    }

    @Test
    public void testSaveDeviceProfileWithEmptyName() throws Exception {
        DeviceProfile deviceProfile = new DeviceProfile();
        doPost("/api/deviceProfile", deviceProfile).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Device profile name should be specified")));
    }

    @Test
    public void testSaveDeviceProfileWithSameName() throws Exception {
        DeviceProfile deviceProfile = this.createDeviceProfile("Device Profile");
        doPost("/api/deviceProfile", deviceProfile).andExpect(status().isOk());
        DeviceProfile deviceProfile2 = this.createDeviceProfile("Device Profile");
        doPost("/api/deviceProfile", deviceProfile2).andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString("Device profile with such name already exists")));
    }

    @Test
    public void testDeleteDeviceProfile() throws Exception {
        DeviceProfile deviceProfile = this.createDeviceProfile("Device Profile");
        DeviceProfile savedDeviceProfile = doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class);

        doDelete("/api/deviceProfile/" + savedDeviceProfile.getId().getId().toString())
                .andExpect(status().isOk());

        doGet("/api/deviceProfile/" + savedDeviceProfile.getId().getId().toString())
                .andExpect(status().isNotFound());
    }

    @Test
    public void testFindDeviceProfiles() throws Exception {
        List<DeviceProfile> deviceProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<DeviceProfile> pageData = doGetTypedWithPageLink("/api/deviceProfiles?",
                new TypeReference<PageData<DeviceProfile>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
        deviceProfiles.addAll(pageData.getData());

        for (int i=0;i<28;i++) {
            DeviceProfile deviceProfile = this.createDeviceProfile("Device Profile"+i);
            deviceProfiles.add(doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class));
        }

        List<DeviceProfile> loadedDeviceProfiles = new ArrayList<>();
        pageLink = new PageLink(17);
        do {
            pageData = doGetTypedWithPageLink("/api/deviceProfiles?",
                    new TypeReference<PageData<DeviceProfile>>(){}, pageLink);
            loadedDeviceProfiles.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(deviceProfiles, idComparator);
        Collections.sort(loadedDeviceProfiles, idComparator);

        Assert.assertEquals(deviceProfiles, loadedDeviceProfiles);

        for (DeviceProfile deviceProfile : loadedDeviceProfiles) {
            if (!deviceProfile.isDefault()) {
                doDelete("/api/deviceProfile/" + deviceProfile.getId().getId().toString())
                        .andExpect(status().isOk());
            }
        }

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/deviceProfiles?",
                new TypeReference<PageData<DeviceProfile>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

    @Test
    public void testFindDeviceProfileInfos() throws Exception {
        List<DeviceProfile> deviceProfiles = new ArrayList<>();
        PageLink pageLink = new PageLink(17);
        PageData<DeviceProfile> deviceProfilePageData = doGetTypedWithPageLink("/api/deviceProfiles?",
                new TypeReference<PageData<DeviceProfile>>(){}, pageLink);
        Assert.assertFalse(deviceProfilePageData.hasNext());
        Assert.assertEquals(1, deviceProfilePageData.getTotalElements());
        deviceProfiles.addAll(deviceProfilePageData.getData());

        for (int i=0;i<28;i++) {
            DeviceProfile deviceProfile = this.createDeviceProfile("Device Profile"+i);
            deviceProfiles.add(doPost("/api/deviceProfile", deviceProfile, DeviceProfile.class));
        }

        List<EntityInfo> loadedDeviceProfileInfos = new ArrayList<>();
        pageLink = new PageLink(17);
        PageData<EntityInfo> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/deviceProfileInfos?",
                    new TypeReference<PageData<EntityInfo>>(){}, pageLink);
            loadedDeviceProfileInfos.addAll(pageData.getData());
            if (pageData.hasNext()) {
                pageLink = pageLink.nextPageLink();
            }
        } while (pageData.hasNext());

        Collections.sort(deviceProfiles, idComparator);
        Collections.sort(loadedDeviceProfileInfos, deviceProfileInfoIdComparator);

        List<EntityInfo> deviceProfileInfos = deviceProfiles.stream().map(deviceProfile -> new EntityInfo(deviceProfile.getId(),
                deviceProfile.getName())).collect(Collectors.toList());

        Assert.assertEquals(deviceProfileInfos, loadedDeviceProfileInfos);

        for (DeviceProfile deviceProfile : deviceProfiles) {
            if (!deviceProfile.isDefault()) {
                doDelete("/api/deviceProfile/" + deviceProfile.getId().getId().toString())
                        .andExpect(status().isOk());
            }
        }

        pageLink = new PageLink(17);
        pageData = doGetTypedWithPageLink("/api/deviceProfileInfos?",
                new TypeReference<PageData<EntityInfo>>(){}, pageLink);
        Assert.assertFalse(pageData.hasNext());
        Assert.assertEquals(1, pageData.getTotalElements());
    }

    private DeviceProfile createDeviceProfile(String name) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName(name);
        deviceProfile.setDescription(name + " Test");
        deviceProfile.setProfileData(JacksonUtil.OBJECT_MAPPER.createObjectNode());
        deviceProfile.setDefault(false);
        deviceProfile.setDefaultRuleChainId(new RuleChainId(Uuids.timeBased()));
        return deviceProfile;
    }
}
