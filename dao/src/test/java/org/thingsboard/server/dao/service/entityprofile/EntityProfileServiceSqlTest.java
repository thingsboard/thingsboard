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

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.entityprofile.EntityProfile;
import org.thingsboard.server.dao.entityprofile.EntityProfileService;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.junit.Assert.*;

@DaoSqlTest
public class EntityProfileServiceSqlTest extends AbstractServiceTest {
    @Autowired
    private EntityProfileService profileService;

    @Test
    public void testFindAll() {
        EntityProfile saved = saveRandomEntityProfile();
        profileService.findAll(AbstractServiceTest.SYSTEM_TENANT_ID).stream()
                .filter(saved::equals)
                .findAny().orElseThrow(AssertionError::new);
    }

    @Test
    public void testFindById() {
        EntityProfile saved = saveRandomEntityProfile();
        assertEquals(saved, profileService.findById(AbstractServiceTest.SYSTEM_TENANT_ID, saved.getId()));
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
        assertNotNull(profileService.findById(AbstractServiceTest.SYSTEM_TENANT_ID, saved.getId()));
        profileService.delete(AbstractServiceTest.SYSTEM_TENANT_ID, saved.getId());
        assertNull(profileService.findById(AbstractServiceTest.SYSTEM_TENANT_ID, saved.getId()));
    }

    private EntityProfile saveRandomEntityProfile() {
        Map<String, Object> profile = new HashMap<>();
        profile.put(randomAlphabetic(5), randomAlphanumeric(10));

        EntityProfile entityProfile = EntityProfile.builder()
                .name(randomAlphanumeric(10))
                .tenantId(AbstractServiceTest.SYSTEM_TENANT_ID)
                .entityType(EntityType.DEVICE)
                .profile(mapper.valueToTree(profile))
                .build();

        return profileService.save(AbstractServiceTest.SYSTEM_TENANT_ID, entityProfile);
    }
}
