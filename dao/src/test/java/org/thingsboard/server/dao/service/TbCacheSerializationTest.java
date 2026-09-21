// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
