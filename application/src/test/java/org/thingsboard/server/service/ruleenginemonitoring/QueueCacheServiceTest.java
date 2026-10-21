// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ruleenginemonitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.QueueShortInfo;
import org.thingsboard.server.dao.sql.ruleenginemonitoring.RuleEngineMonitoringDao;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QueueCacheServiceTest {

    private final UUID sysTenantId = TenantId.SYS_TENANT_ID.getId();
    private final UUID sharedTenantId = UUID.randomUUID();
    private final UUID isolatedTenantId = UUID.randomUUID();
    private final QueueShortInfo systemMain = new QueueShortInfo(UUID.randomUUID(), sysTenantId, "Main", 10, false);
    private final QueueShortInfo isolatedMain = new QueueShortInfo(UUID.randomUUID(), isolatedTenantId, "Main", 10, false);

    private final RuleEngineMonitoringDao dao = Mockito.mock(RuleEngineMonitoringDao.class);
    private final TbTenantProfileCache tenantProfileCache = Mockito.mock(TbTenantProfileCache.class);

    @BeforeEach
    void setUp() {
        when(dao.findAllQueuesShortInfo()).thenReturn(List.of(systemMain, isolatedMain));
        when(tenantProfileCache.get(TenantId.fromUUID(sharedTenantId))).thenReturn(profile(false));
        when(tenantProfileCache.get(TenantId.fromUUID(isolatedTenantId))).thenReturn(profile(true));
    }

    @Test
    void givenSharedAndIsolatedTenants_whenGetQueueId_thenEachResolvesItsOwnQueueOwner() {
        QueueCacheService service = new QueueCacheService(dao, tenantProfileCache, 30);

        assertThat(service.getQueueId(sharedTenantId, "Main")).isEqualTo(systemMain.id());
        assertThat(service.getQueueId(isolatedTenantId, "Main")).isEqualTo(isolatedMain.id());
        assertThat(service.getQueueId(sharedTenantId, "HighPriority")).isNull();
        assertThat(service.getAllQueues()).containsExactly(systemMain, isolatedMain);
    }

    @Test
    void givenTenantWithoutProfile_whenGetQueueId_thenSystemQueuesAreUsed() {
        QueueCacheService service = new QueueCacheService(dao, tenantProfileCache, 30);

        assertThat(service.getQueueId(UUID.randomUUID(), "Main")).isEqualTo(systemMain.id());
    }

    @Test
    void givenLoadedQueues_whenLookedUpRepeatedly_thenLoadedOnceUntilInvalidated() {
        QueueCacheService service = new QueueCacheService(dao, tenantProfileCache, 30);

        for (int i = 0; i < 1000; i++) {
            service.getQueueId(sharedTenantId, "Main");
        }
        verify(dao, times(1)).findAllQueuesShortInfo();

        service.invalidateQueues();
        service.getQueueId(sharedTenantId, "Main");
        verify(dao, times(2)).findAllQueuesShortInfo();
    }

    @Test
    void givenZeroTtl_whenLookedUp_thenReloadedEachTime() throws Exception {
        QueueCacheService service = new QueueCacheService(dao, tenantProfileCache, 0);

        service.getQueueId(sharedTenantId, "Main");
        Thread.sleep(2);
        service.getQueueId(sharedTenantId, "Main");

        verify(dao, times(2)).findAllQueuesShortInfo();
    }

    private static TenantProfile profile(boolean isolated) {
        TenantProfile profile = new TenantProfile();
        profile.setIsolatedTbRuleEngine(isolated);
        return profile;
    }

}
