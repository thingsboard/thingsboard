/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.notification;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.NotificationRequestId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationRequest;
import org.thingsboard.server.common.data.notification.NotificationRequestStatus;
import org.thingsboard.server.dao.AbstractJpaDaoTest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class JpaNotificationRequestDaoTest extends AbstractJpaDaoTest {

    @Autowired
    JpaNotificationRequestDao notificationRequestDao;

    @Test
    public void testBatchDeletion() {
        TenantId sysTenantId = TenantId.SYS_TENANT_ID;
        long now = System.currentTimeMillis();
        long oldTimestamp = now - TimeUnit.DAYS.toMillis(30);

        NotificationRequest oldRequest1 = createNotificationRequest(sysTenantId, oldTimestamp);
        notificationRequestDao.save(sysTenantId, oldRequest1);

        NotificationRequest oldRequest2 = createNotificationRequest(sysTenantId, oldTimestamp);
        notificationRequestDao.save(sysTenantId, oldRequest2);

        NotificationRequest freshRequest = createNotificationRequest(sysTenantId, now);
        notificationRequestDao.save(sysTenantId, freshRequest);

        TenantId tenant2Id = TenantId.fromUUID(UUID.fromString("3d193a7a-774b-4c05-84d5-f7fdcf7a37cf"));
        NotificationRequest tenant2Request = createNotificationRequest(tenant2Id, oldTimestamp);
        notificationRequestDao.save(tenant2Id, tenant2Request);

        int batchSize = 10_000;

        assertThat(notificationRequestDao.removeByTenantIdAndCreatedTimeBeforeBatch(sysTenantId, oldTimestamp - 1, batchSize)).isEqualTo(0);

        long expirationTime = now - TimeUnit.DAYS.toMillis(15);
        assertThat(notificationRequestDao.removeByTenantIdAndCreatedTimeBeforeBatch(sysTenantId, expirationTime, batchSize)).isEqualTo(2);

        assertThat(notificationRequestDao.findById(sysTenantId, freshRequest.getId().getId())).isNotNull();
        assertThat(notificationRequestDao.removeByTenantIdAndCreatedTimeBeforeBatch(tenant2Id, now + 1, batchSize)).isEqualTo(1);

        notificationRequestDao.removeById(sysTenantId, freshRequest.getId().getId());
    }

    @Test
    public void testBatchDeletionWithSmallBatchSize() {
        TenantId tenantId = TenantId.SYS_TENANT_ID;
        long oldTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);

        for (int i = 0; i < 10; i++) {
            NotificationRequest request = createNotificationRequest(tenantId, oldTimestamp);
            notificationRequestDao.save(tenantId, request);
        }

        int batchSize = 3;
        long expirationTime = System.currentTimeMillis();

        assertThat(notificationRequestDao.removeByTenantIdAndCreatedTimeBeforeBatch(tenantId, expirationTime, batchSize)).isEqualTo(3);
        assertThat(notificationRequestDao.removeByTenantIdAndCreatedTimeBeforeBatch(tenantId, expirationTime, batchSize)).isEqualTo(3);
        assertThat(notificationRequestDao.removeByTenantIdAndCreatedTimeBeforeBatch(tenantId, expirationTime, batchSize)).isEqualTo(3);
        assertThat(notificationRequestDao.removeByTenantIdAndCreatedTimeBeforeBatch(tenantId, expirationTime, batchSize)).isEqualTo(1);
        assertThat(notificationRequestDao.removeByTenantIdAndCreatedTimeBeforeBatch(tenantId, expirationTime, batchSize)).isEqualTo(0);
    }

    @Test
    public void testBatchDeletionIsolationBetweenTenants() {
        TenantId tenant1 = TenantId.SYS_TENANT_ID;
        TenantId tenant2 = TenantId.fromUUID(UUID.fromString("3d193a7a-774b-4c05-84d5-f7fdcf7a37cf"));
        long oldTimestamp = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);

        for (int i = 0; i < 5; i++) {
            NotificationRequest request = createNotificationRequest(tenant1, oldTimestamp);
            notificationRequestDao.save(tenant1, request);
        }

        for (int i = 0; i < 3; i++) {
            NotificationRequest request = createNotificationRequest(tenant2, oldTimestamp);
            notificationRequestDao.save(tenant2, request);
        }

        int batchSize = 10_000;
        long expirationTime = System.currentTimeMillis();

        assertThat(notificationRequestDao.removeByTenantIdAndCreatedTimeBeforeBatch(tenant1, expirationTime, batchSize)).isEqualTo(5);
        assertThat(notificationRequestDao.removeByTenantIdAndCreatedTimeBeforeBatch(tenant2, expirationTime, batchSize)).isEqualTo(3);
    }

    private NotificationRequest createNotificationRequest(TenantId tenantId, long createdTime) {
        NotificationRequest request = new NotificationRequest();
        request.setId(new NotificationRequestId(UUID.randomUUID()));
        request.setTenantId(tenantId);
        request.setCreatedTime(createdTime);
        request.setTargets(List.of(UUID.randomUUID()));
        request.setStatus(NotificationRequestStatus.SENT);
        return request;
    }

}
