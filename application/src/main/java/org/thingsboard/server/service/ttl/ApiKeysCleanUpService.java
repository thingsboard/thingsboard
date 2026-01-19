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
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.dao.pat.ApiKeyDao;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Slf4j
@Service
@TbCoreComponent
@ConditionalOnExpression("${sql.ttl.api_keys.enabled:true} && ${sql.ttl.api_keys.ttl:0} > 0")
public class ApiKeysCleanUpService extends AbstractCleanUpService {

    public static final String RANDOM_DELAY_INTERVAL_MS_EXPRESSION =
            "#{T(org.apache.commons.lang3.RandomUtils).nextLong(0, ${sql.ttl.api_keys.checking_interval_ms})}";

    private final ApiKeyDao apiKeyDao;

    public ApiKeysCleanUpService(PartitionService partitionService, ApiKeyDao apiKeyDao) {
        super(partitionService);
        this.apiKeyDao = apiKeyDao;
    }

    @Scheduled(
            initialDelayString = RANDOM_DELAY_INTERVAL_MS_EXPRESSION,
            fixedDelayString = "${sql.ttl.api_keys.checking_interval_ms:86400000}"
    )
    public void cleanUp() {
        long threshold = System.currentTimeMillis();
        if (isSystemTenantPartitionMine()) {
            int deleted = apiKeyDao.deleteAllByExpirationTimeBefore(threshold);
            if (deleted > 0) {
                log.info("API key cleanup removed {} keys (thresholdTs={})", deleted, threshold);
            }
        }
    }

}
