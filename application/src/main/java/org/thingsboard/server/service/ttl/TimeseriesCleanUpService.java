/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@TbCoreComponent
@Slf4j
@Service
public class TimeseriesCleanUpService extends AbstractCleanUpService {

    @Value("${sql.ttl.ts.ts_key_value_ttl}")
    protected long systemTtl;

    @Value("${sql.ttl.ts.enabled}")
    private boolean ttlTaskExecutionEnabled;

    @Value("${sql.ttl.ts.excluded_keys}")
    private String excludedKeysStr;

    private final TimeseriesService timeseriesService;
    private List<String> excludedKeys;

    public TimeseriesCleanUpService(PartitionService partitionService, TimeseriesService timeseriesService) {
        super(partitionService);
        this.timeseriesService = timeseriesService;
    }

    @PostConstruct
    public void init() {
        excludedKeys = StringUtils.hasLength(excludedKeysStr) ? null : new ArrayList<>(Arrays.asList(excludedKeysStr.trim().split("\\s*,\\s*")));
    }

    @Scheduled(initialDelayString = "${sql.ttl.ts.execution_interval_ms}", fixedDelayString = "${sql.ttl.ts.execution_interval_ms}")
    public void cleanUp() {
        if (ttlTaskExecutionEnabled && isSystemTenantPartitionMine()) {
            if (CollectionUtils.isEmpty(excludedKeys)) {
                timeseriesService.cleanup(systemTtl);
            } else {
                timeseriesService.cleanup(systemTtl, excludedKeys);
            }
        }
    }

}