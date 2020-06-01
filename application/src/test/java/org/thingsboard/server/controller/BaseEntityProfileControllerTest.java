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

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.After;
import org.junit.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.entityprofile.EntityProfile;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class BaseEntityProfileControllerTest extends AbstractControllerTest {
    private final TypeReference<PageData<EntityProfile>> PAGE_ENTITY_PROFILE_TYPE = new TypeReference<PageData<EntityProfile>>() {
    };

    @After
    public void tearDown() throws Exception {
        logout();
    }

    @Test
    public void testSysAdminPermissionDenyForNonTenantProfile() throws Exception {
        loginSysAdmin();
        EntityProfile entityProfile = randomEntityProfile(EntityType.ASSET);
        doPost("/api/entityProfiles", entityProfile, EntityProfile.class, status().isForbidden());
    }

    @Test
    public void testSysAdminFindEntityProfiles() throws Exception {
        loginSysAdmin();
        EntityProfile entityProfile = randomEntityProfile(EntityType.TENANT);
        EntityProfile saved = doPost("/api/entityProfiles", entityProfile, EntityProfile.class);
        EntityProfile found = doGet("/api/entityProfiles/{id}", EntityProfile.class,
                saved.getId().getId().toString());
        assertNotNull(found);
        assertEquals(saved, found);
        PageData<EntityProfile> page = doGetTypedWithPageLink("/api/entityProfiles?",
                PAGE_ENTITY_PROFILE_TYPE, new PageLink(5));
        assertFalse(page.getData().isEmpty());
    }

    @Test
    public void testTenantAdminDeviceEntityProfileFlow() throws Exception {
        loginTenantAdmin();
        EntityProfile entityProfile = randomEntityProfile(EntityType.DEVICE);
        EntityProfile saved = doPost("/api/entityProfiles", entityProfile, EntityProfile.class);
        EntityProfile found = doGet("/api/entityProfiles/{id}", EntityProfile.class,
                saved.getId().getId().toString());
        assertNotNull(found);
        assertEquals(saved, found);
        PageData<EntityProfile> page = doGetTypedWithPageLink("/api/entityProfiles?type={type}&",
                PAGE_ENTITY_PROFILE_TYPE, new PageLink(5), EntityType.DEVICE);
        assertEquals(1, page.getData().size());
        assertEquals(saved, page.getData().get(0));
        doDelete("/api/entityProfiles/" + saved.getId().getId())
                .andExpect(status().isOk());
        page = doGetTypedWithPageLink("/api/entityProfiles?type={type}&",
                PAGE_ENTITY_PROFILE_TYPE, new PageLink(5), EntityType.DEVICE);
        assertEquals(0, page.getTotalElements());
    }

    @Test
    public void testTenantAdminGetEntityProfiles() throws Exception {
        loginTenantAdmin();
        doPost("/api/entityProfiles", randomEntityProfile(EntityType.ASSET), EntityProfile.class);
        doPost("/api/entityProfiles", randomEntityProfile(EntityType.CUSTOMER), EntityProfile.class);
        PageData<EntityProfile> page = doGetTypedWithPageLink("/api/entityProfiles?",
                PAGE_ENTITY_PROFILE_TYPE, new PageLink(5));
        assertFalse(page.getData().isEmpty());
    }

    private EntityProfile randomEntityProfile(EntityType asset) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("test", randomAlphanumeric(10));
        return EntityProfile.builder()
                .entityType(asset)
                .name(randomAlphanumeric(10))
                .profile(mapper.valueToTree(profile))
                .build();
    }
}
