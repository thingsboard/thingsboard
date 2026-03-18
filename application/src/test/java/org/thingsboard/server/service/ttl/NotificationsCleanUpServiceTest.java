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
package org.thingsboard.server.service.ttl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.dao.notification.NotificationRequestDao;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.discovery.PartitionService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NotificationsCleanUpServiceTest {

    @Mock
    private PartitionService partitionService;
    @Mock
    private SqlPartitioningRepository partitioningRepository;
    @Mock
    private NotificationRequestDao notificationRequestDao;
    @Mock
    private TenantService tenantService;

    private NotificationsCleanUpService cleanUpService;

    private static final int BATCH_SIZE = 3;

    @BeforeEach
    public void setUp() {
        cleanUpService = new NotificationsCleanUpService(partitionService, partitioningRepository, notificationRequestDao, tenantService);
        ReflectionTestUtils.setField(cleanUpService, "ttlInSec", 2592000L);
        ReflectionTestUtils.setField(cleanUpService, "partitionSizeInHours", 168);
        ReflectionTestUtils.setField(cleanUpService, "removalBatchSize", BATCH_SIZE);
    }

    @Test
    public void testBatchLoopCallsDaoMultipleTimes() {
        TopicPartitionInfo myPartition = TopicPartitionInfo.builder().topic("tb_core").myPartition(true).build();
        when(partitionService.resolve(any(), any(), any())).thenReturn(myPartition);
        when(partitioningRepository.dropPartitionsBefore(anyString(), anyLong(), anyLong()))
                .thenReturn(System.currentTimeMillis());

        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        when(tenantService.findTenantsIds(any()))
                .thenReturn(new PageData<>(List.of(tenantId), 1, 1, false));

        // Sysadmin: returns 3 (full batch), then 1 (partial) -> 2 calls
        when(notificationRequestDao.removeByTenantIdAndCreatedTimeBeforeBatch(eq(TenantId.SYS_TENANT_ID), anyLong(), eq(BATCH_SIZE)))
                .thenReturn(BATCH_SIZE)
                .thenReturn(1);
        // Tenant: returns 3, 3, 0 -> 3 calls
        when(notificationRequestDao.removeByTenantIdAndCreatedTimeBeforeBatch(eq(tenantId), anyLong(), eq(BATCH_SIZE)))
                .thenReturn(BATCH_SIZE)
                .thenReturn(BATCH_SIZE)
                .thenReturn(0);

        cleanUpService.cleanUp();

        verify(notificationRequestDao, times(2))
                .removeByTenantIdAndCreatedTimeBeforeBatch(eq(TenantId.SYS_TENANT_ID), anyLong(), eq(BATCH_SIZE));
        verify(notificationRequestDao, times(3))
                .removeByTenantIdAndCreatedTimeBeforeBatch(eq(tenantId), anyLong(), eq(BATCH_SIZE));
    }

    @Test
    public void testSkipsTenantNotOnMyPartition() {
        TopicPartitionInfo myPartition = TopicPartitionInfo.builder().topic("tb_core").myPartition(true).build();
        TopicPartitionInfo notMyPartition = TopicPartitionInfo.builder().topic("tb_core").myPartition(false).build();
        when(partitionService.resolve(any(), eq(TenantId.SYS_TENANT_ID), eq(TenantId.SYS_TENANT_ID)))
                .thenReturn(myPartition);
        when(partitioningRepository.dropPartitionsBefore(anyString(), anyLong(), anyLong()))
                .thenReturn(System.currentTimeMillis());

        // Sysadmin: no records
        when(notificationRequestDao.removeByTenantIdAndCreatedTimeBeforeBatch(eq(TenantId.SYS_TENANT_ID), anyLong(), eq(BATCH_SIZE)))
                .thenReturn(0);

        TenantId myTenant = TenantId.fromUUID(UUID.randomUUID());
        TenantId otherTenant = TenantId.fromUUID(UUID.randomUUID());
        when(tenantService.findTenantsIds(any()))
                .thenReturn(new PageData<>(List.of(myTenant, otherTenant), 2, 1, false));
        when(partitionService.resolve(any(), eq(myTenant), eq(myTenant))).thenReturn(myPartition);
        when(partitionService.resolve(any(), eq(otherTenant), eq(otherTenant))).thenReturn(notMyPartition);

        when(notificationRequestDao.removeByTenantIdAndCreatedTimeBeforeBatch(eq(myTenant), anyLong(), eq(BATCH_SIZE)))
                .thenReturn(0);

        cleanUpService.cleanUp();

        verify(notificationRequestDao).removeByTenantIdAndCreatedTimeBeforeBatch(eq(myTenant), anyLong(), eq(BATCH_SIZE));
        verify(notificationRequestDao, never()).removeByTenantIdAndCreatedTimeBeforeBatch(eq(otherTenant), anyLong(), anyInt());
    }

    @Test
    public void testNoPartitionsDropped_stillCleansUpRequests() {
        TopicPartitionInfo myPartition = TopicPartitionInfo.builder().topic("tb_core").myPartition(true).build();
        when(partitionService.resolve(any(), any(), any())).thenReturn(myPartition);
        when(partitioningRepository.dropPartitionsBefore(anyString(), anyLong(), anyLong()))
                .thenReturn(0L);

        when(notificationRequestDao.removeByTenantIdAndCreatedTimeBeforeBatch(eq(TenantId.SYS_TENANT_ID), anyLong(), eq(BATCH_SIZE)))
                .thenReturn(0);
        when(tenantService.findTenantsIds(any()))
                .thenReturn(new PageData<>(List.of(), 0, 0, false));

        cleanUpService.cleanUp();

        verify(notificationRequestDao).removeByTenantIdAndCreatedTimeBeforeBatch(eq(TenantId.SYS_TENANT_ID), anyLong(), eq(BATCH_SIZE));
    }

}
