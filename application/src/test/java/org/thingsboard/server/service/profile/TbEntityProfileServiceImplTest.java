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
package org.thingsboard.server.service.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.entityprofile.EntityProfile;
import org.thingsboard.server.common.data.entityprofile.HasEntityProfileId;
import org.thingsboard.server.common.data.id.EntityProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.entityprofile.EntityProfileService;
import org.thingsboard.server.service.profile.processor.EntityProfilePostProcessor;
import org.thingsboard.server.service.profile.profile.BaseProfile;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class TbEntityProfileServiceImplTest {
    private ObjectMapper objectMapper = new ObjectMapper();
    private EntityProfileService entityProfileService;
    private TbEntityProfileServiceImpl tbEntityProfileService;

    @Before
    public void setUp() {
        entityProfileService = mock(EntityProfileService.class);
        tbEntityProfileService = new TbEntityProfileServiceImpl(entityProfileService);
        tbEntityProfileService.setSelf(tbEntityProfileService);
    }

    @Test
    public void createDefault() {
        String testValue = randomAlphanumeric(10);
        String name = randomAlphanumeric(10);
        TestProfilePostProcessor processor = new TestProfilePostProcessor(testValue);
        tbEntityProfileService.setProcessors(Lists.newArrayList(processor));
        tbEntityProfileService.createDefault(TenantId.SYS_TENANT_ID, name, TestProfile.class);
        verify(entityProfileService).save(argThat(argument -> {
            TestProfile testProfile = new TestProfile();
            testProfile.setTestValue(testValue);
            return argument.getTenantId().equals(TenantId.SYS_TENANT_ID) &&
                    argument.getName().equals(name) &&
                    argument.getProfile().equals(objectMapper.valueToTree(testProfile)) &&
                    argument.getEntityType().equals(processor.getEntityType());
        }));
    }

    @Test(expected = IllegalStateException.class)
    public void createDefaultWhenProcessorNotFound() {
        tbEntityProfileService.setProcessors(Collections.emptyList());
        tbEntityProfileService.createDefault(TenantId.SYS_TENANT_ID, randomAlphanumeric(10), TestProfile.class);
    }


    @Test
    public void testFindTestProfile() {
        String testValue = randomAlphanumeric(10);
        tbEntityProfileService.setProcessors(Lists.newArrayList(new TestProfilePostProcessor(null)));

        Map<String, Object> profileMap = new HashMap<>();
        profileMap.put("testValue", testValue);
        EntityProfile entityProfile = EntityProfile.builder()
                .id(new EntityProfileId(UUID.randomUUID()))
                .name(randomAlphanumeric(10))
                .tenantId(TenantId.SYS_TENANT_ID)
                .entityType(EntityType.DEVICE)
                .profile(objectMapper.valueToTree(profileMap))
                .build();
        TestDevice testDevice = new TestDevice(TenantId.SYS_TENANT_ID, entityProfile.getId());
        when(entityProfileService.findById(eq(testDevice.getTenantId()), eq(testDevice.getEntityProfileId())))
                .thenReturn(entityProfile);

        TestProfile profile = tbEntityProfileService.findProfile(testDevice, TestProfile.class);
        assertEquals(testValue, profile.getTestValue());
    }

    @Test
    public void testFindTestProfileWithDefaultValue() {
        String testValue = randomAlphanumeric(10);
        tbEntityProfileService.setProcessors(Lists.newArrayList(new TestProfilePostProcessor(testValue)));
        EntityProfile entityProfile = EntityProfile.builder()
                .id(new EntityProfileId(UUID.randomUUID()))
                .name(randomAlphanumeric(10))
                .tenantId(TenantId.SYS_TENANT_ID)
                .entityType(EntityType.DEVICE)
                .profile(objectMapper.createObjectNode())
                .build();
        TestDevice testDevice = new TestDevice(TenantId.SYS_TENANT_ID, entityProfile.getId());
        when(entityProfileService.findById(eq(testDevice.getTenantId()), eq(testDevice.getEntityProfileId())))
                .thenReturn(entityProfile);
        TestProfile profile = tbEntityProfileService.findProfile(testDevice, TestProfile.class);
        assertEquals(testValue, profile.getTestValue());
    }

    @Test
    public void testFindTestProfileWhenEntityProfileNotCreated() {
        String testValue = randomAlphanumeric(10);
        tbEntityProfileService.setProcessors(Lists.newArrayList(new TestProfilePostProcessor(testValue)));
        TestDevice testDevice = new TestDevice(TenantId.SYS_TENANT_ID, null);
        TestProfile profile = tbEntityProfileService.findProfile(testDevice, TestProfile.class);
        assertEquals(testValue, profile.getTestValue());
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
        public EntityType getEntityType() {
            return EntityType.DEVICE;
        }

        @Override
        public void initProfile(TestProfile profile) {
            profile.setTestValue(defaultTestValue);
        }

        @Override
        public void setDefaultValues(TestProfile profile) {
            if (profile.getTestValue() == null) {
                profile.setTestValue(defaultTestValue);
            }
        }
    }
}
