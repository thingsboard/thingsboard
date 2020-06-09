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
package org.thingsboard.server.dao.service.entityprofile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.entityprofile.BaseProfile;
import org.thingsboard.server.common.data.entityprofile.EntityProfile;
import org.thingsboard.server.common.data.entityprofile.HasEntityProfileId;
import org.thingsboard.server.common.data.id.EntityProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entityprofile.EntityProfileDao;
import org.thingsboard.server.dao.entityprofile.EntityProfilePostProcessor;
import org.thingsboard.server.dao.entityprofile.EntityProfileServiceImpl;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.Assert.*;

@DaoSqlTest
public class EntityProfileServiceSqlTest extends AbstractServiceTest {
    @Autowired
    private EntityProfileServiceImpl profileService;
    @Autowired
    private EntityProfileDao entityProfileDao;

    @Test
    public void testFindEntityProfilesByTenantIdWithPageLink() {
        List<EntityProfile> all = entityProfileDao.find(TenantId.SYS_TENANT_ID);
        PageLink pageLink = new PageLink(all.size());
        PageData<EntityProfile> page = profileService.findEntityProfilesByTenantId(TenantId.SYS_TENANT_ID, pageLink);
        assertEquals(new HashSet<>(all), new HashSet<>(page.getData()));
    }

    @Test
    public void testFindEntityProfilesByTenantIdAndType() {
        EntityProfile saved = saveRandomEntityProfile(randomAlphanumeric(10), EntityType.ASSET);
        PageLink pageLink = new PageLink(1);
        List<EntityProfile> data = profileService.findEntityProfilesByTenantIdAndType(saved.getTenantId(),
                saved.getEntityType(), pageLink).getData();
        assertEquals(pageLink.getPageSize(), data.size());
    }

    @Test
    public void testFindTenantProfiles() {
        saveRandomEntityProfile(randomAlphanumeric(10), EntityType.TENANT);
        PageLink pageLink = new PageLink(1);
        List<EntityProfile> data = profileService.findTenantProfiles(pageLink).getData();
        assertEquals(pageLink.getPageSize(), data.size());
    }

    @Test
    public void testFindById() {
        EntityProfile saved = saveRandomEntityProfile();
        assertEquals(saved, profileService.findById(TenantId.SYS_TENANT_ID, saved.getId()));
    }

    @Test(expected = DataIntegrityViolationException.class)
    public void testSaveWithSameNameTenantAndType() {
        String sameName = randomAlphanumeric(10);
        EntityType sameEntityType = EntityType.DEVICE;
        saveRandomEntityProfile(sameName, sameEntityType);
        saveRandomEntityProfile(sameName, sameEntityType);
    }

    @Test
    public void testSaveWithSameNameTenantDiffType() {
        String sameName = randomAlphanumeric(10);
        saveRandomEntityProfile(sameName, EntityType.DEVICE);
        saveRandomEntityProfile(sameName, EntityType.ASSET);
    }

    @Test
    public void testSaveAndSerialization() {
        EntityProfile saved = saveRandomEntityProfile();
        byte[] serialized = SerializationUtils.serialize(saved);
        assertEquals(saved, SerializationUtils.deserialize(serialized));
    }

    @Test
    public void testDelete() {
        EntityProfile saved = saveRandomEntityProfile();
        assertNotNull(profileService.findById(TenantId.SYS_TENANT_ID, saved.getId()));
        profileService.delete(TenantId.SYS_TENANT_ID, saved.getId());
        assertNull(profileService.findById(TenantId.SYS_TENANT_ID, saved.getId()));
    }

    @Test
    public void testFindTestProfile() {
        String testValue = randomAlphanumeric(10);
        profileService.setProcessors(Lists.newArrayList(new TestProfilePostProcessor(null)));
        Map<String, Object> profileMap = new HashMap<>();
        profileMap.put("testValue", testValue);
        EntityProfile entityProfile = profileService.save(EntityProfile.builder()
                .name(randomAlphanumeric(10))
                .tenantId(TenantId.SYS_TENANT_ID)
                .entityType(EntityType.DEVICE)
                .profile(mapper.valueToTree(profileMap))
                .build());
        TestDevice testDevice = new TestDevice(TenantId.SYS_TENANT_ID, entityProfile.getId());
        TestProfile profile = profileService.findProfile(testDevice, TestProfile.class);
        assertEquals(testValue, profile.getTestValue());
    }

    @Test
    public void testFindTestProfileWithDefaultValue() {
        String testValue = randomAlphanumeric(10);
        profileService.setProcessors(Lists.newArrayList(new TestProfilePostProcessor(testValue)));
        EntityProfile entityProfile = saveRandomEntityProfile();
        TestDevice testDevice = new TestDevice(TenantId.SYS_TENANT_ID, entityProfile.getId());
        TestProfile profile = profileService.findProfile(testDevice, TestProfile.class);
        assertEquals(testValue, profile.getTestValue());
    }

    @Test
    public void testFindTestProfileWhenEntityProfileNotCreated() {
        String testValue = randomAlphanumeric(10);
        profileService.setProcessors(Lists.newArrayList(new TestProfilePostProcessor(testValue)));
        TestDevice testDevice = new TestDevice(TenantId.SYS_TENANT_ID, null);
        TestProfile profile = profileService.findProfile(testDevice, TestProfile.class);
        assertEquals(testValue, profile.getTestValue());
    }

    private EntityProfile saveRandomEntityProfile() {
        return saveRandomEntityProfile(randomAlphanumeric(10), EntityType.DEVICE);
    }

    private EntityProfile saveRandomEntityProfile(String name, EntityType entityType) {
        Map<String, Object> profile = new HashMap<>();
        profile.put(randomAlphabetic(5), randomAlphanumeric(10));

        EntityProfile entityProfile = EntityProfile.builder()
                .name(name)
                .tenantId(TenantId.SYS_TENANT_ID)
                .entityType(entityType)
                .profile(mapper.valueToTree(profile))
                .build();

        return profileService.save(entityProfile);
    }

    @Value
    private static class TestDevice implements HasEntityProfileId, HasTenantId {
        TenantId tenantId;
        EntityProfileId entityProfileId;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class TestProfile implements BaseProfile {
        String testValue;
    }

    @RequiredArgsConstructor
    private static class TestProfilePostProcessor implements EntityProfilePostProcessor<TestProfile> {
        private final String defaultTestValue;

        @Override
        public Class<TestProfile> getProfileClass() {
            return TestProfile.class;
        }

        @Override
        public void setDefaultValues(TestProfile profile) {
            if (profile.getTestValue() == null) {
                profile.setTestValue(defaultTestValue);
            }
        }
    }
}
