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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.notification.NotificationRequestConfig;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.dao.notification.NotificationRequestDao;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.thingsboard.server.dao.model.ModelConstants.NOTIFICATION_TABLE_NAME;

@Slf4j
@Service
@ConditionalOnExpression("${sql.ttl.notifications.enabled:true} && ${sql.ttl.notifications.ttl:0} > 0")
public class NotificationsCleanUpService extends AbstractCleanUpService {

    private final SqlPartitioningRepository partitioningRepository;
    private final NotificationRequestDao notificationRequestDao;
    private final TenantService tenantService;

    @Value("${sql.ttl.notifications.ttl:2592000}")
    private long ttlInSec;
    @Value("${sql.notifications.partition_size:168}")
    private int partitionSizeInHours;
    @Value("${sql.ttl.notifications.removal_batch_size:10000}")
    private int removalBatchSize;

    public NotificationsCleanUpService(PartitionService partitionService, SqlPartitioningRepository partitioningRepository,
                                       NotificationRequestDao notificationRequestDao, TenantService tenantService) {
        super(partitionService);
        this.partitioningRepository = partitioningRepository;
        this.notificationRequestDao = notificationRequestDao;
        this.tenantService = tenantService;
    }

    @Scheduled(initialDelayString = "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.notifications.checking_interval_ms:86400000})}",
            fixedDelayString = "${sql.ttl.notifications.checking_interval_ms:86400000}")
    public void cleanUp() {
        long expTime = System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(ttlInSec);
        long partitionDurationMs = TimeUnit.HOURS.toMillis(partitionSizeInHours);
        if (!isSystemTenantPartitionMine()) {
            partitioningRepository.cleanupPartitionsCache(NOTIFICATION_TABLE_NAME, expTime, partitionDurationMs);
            return;
        }

        long lastRemovedNotificationTs = partitioningRepository.dropPartitionsBefore(NOTIFICATION_TABLE_NAME, expTime, partitionDurationMs);
        if (lastRemovedNotificationTs > 0) {
            long gap = TimeUnit.MINUTES.toMillis(10);
            long requestExpTime = lastRemovedNotificationTs - TimeUnit.SECONDS.toMillis(NotificationRequestConfig.MAX_SENDING_DELAY) - gap;
            cleanUpNotificationRequests(requestExpTime);
        }
    }

    private void cleanUpNotificationRequests(long expirationTime) {
        log.info("Starting notification requests cleanup for records older than {}", expirationTime);
        int totalRemoved = 0;
        int tenantsProcessed = 0;

        // Clean up SYSADMIN's notification requests:
        try {
            totalRemoved += cleanUpByTenant(TenantId.SYS_TENANT_ID, expirationTime);
        } catch (Exception e) {
            log.warn("Failed to clean up notification requests for sysadmin {}", TenantId.SYS_TENANT_ID, e);
        }
        // Clean up notification requests for tenants
        PageDataIterable<TenantId> tenants = new PageDataIterable<>(tenantService::findTenantsIds, 10_000);
        for (TenantId tenantId : tenants) {
            try {
                int tenantRemoved = cleanUpByTenant(tenantId, expirationTime);
                totalRemoved += tenantRemoved;
                tenantsProcessed++;
                if (tenantRemoved > 0) {
                    log.trace("Removed {} notification requests for tenant {}", tenantRemoved, tenantId);
                }
            } catch (Exception e) {
                log.warn("Failed to clean up notification requests for tenant {}", tenantId, e);
            }
        }

        log.info("Notification requests cleanup completed. Processed {} tenants, removed {} total records older than {}", tenantsProcessed, totalRemoved, new Date(expirationTime));
    }

    private int cleanUpByTenant(TenantId tenantId, long expirationTime) {
        int totalRemoved = 0;
        int batchRemoved;

        do {
            batchRemoved = notificationRequestDao.removeByTenantIdAndCreatedTimeBeforeBatch(tenantId, expirationTime, removalBatchSize);
            totalRemoved += batchRemoved;

            if (batchRemoved > 0) {
                log.trace("Removed {} notification requests in batch for tenant {}", batchRemoved, tenantId);
            }
        } while (batchRemoved >= removalBatchSize);

        return totalRemoved;
    }

}
