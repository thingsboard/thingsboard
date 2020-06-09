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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    public void testUpdateEntityProfileFromDifferentTenant() throws Exception {
        loginTenantAdmin();
        EntityProfile saved = doPost("/api/entityProfiles", randomEntityProfile(EntityType.ASSET), EntityProfile.class);
        loginDifferentTenant();
        doPost("/api/device", saved, EntityProfile.class, status().isForbidden());
        deleteDifferentTenant();
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

    @Test
    public void testFindEntityProfileByType() throws Exception {
        loginTenantAdmin();

        String title = "Entity Profile ";

        EntityType type1 = EntityType.ASSET;
        int capacity1 = 125;

        EntityType type2 = EntityType.CUSTOMER;
        int capacity2 = 143;

        Set<EntityProfile> created1 = createRandomEntityProfiles(type1, title, capacity1);
        Set<EntityProfile> created2 = createRandomEntityProfiles(type2, title, capacity2);
        Set<EntityProfile> loaded1 = loadEntityProfiles(type1, title, capacity1, created1);
        Set<EntityProfile> loaded2 = loadEntityProfiles(type2, title, capacity2, created2);
        deleteEntityProfiles(type1, title, loaded1);
        deleteEntityProfiles(type2, title, loaded2);
    }

    @Test
    public void testFindEntityProfileByName() throws Exception {
        loginTenantAdmin();

        EntityType type = EntityType.ASSET;

        String title1 = "Entity Profile 1";
        int capacity1 = 125;

        String title2 = "Entity Profile 2";
        int capacity2 = 143;

        Set<EntityProfile> created1 = createRandomEntityProfiles(type, title1, capacity1);
        Set<EntityProfile> created2 = createRandomEntityProfiles(type, title2, capacity2);
        Set<EntityProfile> loaded1 = loadEntityProfiles("", title1, capacity1, created1);
        Set<EntityProfile> loaded2 = loadEntityProfiles("", title2, capacity2, created2);
        deleteEntityProfiles("", title1, loaded1);
        deleteEntityProfiles("", title2, loaded2);
    }

    private Set<EntityProfile> createRandomEntityProfiles(EntityType type, String title, int capacity) throws Exception {
        Set<EntityProfile> created = new HashSet<>(capacity);
        for (int i = 0; i < capacity; i++) {
            String name = title + randomAlphanumeric(10);
            name = i % 2 == 0 ? name.toLowerCase() : name.toUpperCase();
            EntityProfile entityProfile = randomEntityProfile(type, name);
            created.add(doPost("/api/entityProfiles", entityProfile, EntityProfile.class));
        }
        return created;
    }

    private Set<EntityProfile> loadEntityProfiles(Object type, String title, int capacity, Set<EntityProfile> created) throws Exception {
        Set<EntityProfile> loaded = new HashSet<>(capacity);
        PageLink pageLink = new PageLink(15, 0, title);
        PageData<EntityProfile> pageData;
        do {
            pageData = doGetTypedWithPageLink("/api/entityProfiles?type={type}&",
                    PAGE_ENTITY_PROFILE_TYPE, pageLink, type);
            loaded.addAll(pageData.getData());
            pageLink = pageLink.nextPageLink();
        } while (pageData.hasNext());
        assertEquals(created, loaded);
        return loaded;
    }

    private void deleteEntityProfiles(Object type, String title, Set<EntityProfile> loaded) throws Exception {
        for (EntityProfile entityProfile : loaded) {
            doDelete("/api/entityProfiles/" + entityProfile.getId().getId())
                    .andExpect(status().isOk());
        }
        PageLink pageLink = new PageLink(15, 0, title);
        PageData<EntityProfile> pageData = doGetTypedWithPageLink("/api/entityProfiles?type={type}&",
                PAGE_ENTITY_PROFILE_TYPE, pageLink, type);
        assertFalse(pageData.hasNext());
        assertTrue(pageData.getData().isEmpty());
    }

    private EntityProfile randomEntityProfile(EntityType entityType) {
        return randomEntityProfile(entityType, randomAlphanumeric(10));
    }

    private EntityProfile randomEntityProfile(EntityType entityType, String name) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("test", randomAlphanumeric(10));
        return EntityProfile.builder()
                .entityType(entityType)
                .name(name)
                .profile(mapper.valueToTree(profile))
                .build();
    }
}
