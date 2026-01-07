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

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@DaoSqlTest
public class TbCacheSerializationTest extends AbstractServiceTest {

    @Autowired
    TbTransactionalCache<TenantId, PageData<EntitySubtype>> alarmTypesCache;

    @Test
    public void AlarmTypesSerializationTest() {
        var typesCount = 13;
        TenantId tenantId = new TenantId(UUID.randomUUID());
        List<EntitySubtype> types = new ArrayList<>(typesCount);
        for (int i = 0; i < typesCount; i++) {
            types.add(new EntitySubtype(tenantId, EntityType.ALARM, "alarm_type_" + i));
        }
        PageData<EntitySubtype> alarmTypesPage = new PageData<>(types, 1, typesCount, false);
        alarmTypesCache.put(tenantId, alarmTypesPage);
        PageData<EntitySubtype> foundAlarmTypes = alarmTypesCache.get(tenantId).get();
        Assert.assertEquals(alarmTypesPage, foundAlarmTypes);
    }
}
