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
package org.thingsboard.server.service.attributes;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;


public abstract class AbstractAttributesTbCacheStatsService<T> implements TbCacheStatsService<T> {
    protected static final String CACHE_STATS_NAME = "attributes";

    @Value("${cache.attributes.enableStats}")
    protected Boolean attributeCacheStatsEnabled;

    @Value("${metrics.enabled:false}")
    protected Boolean metricsEnabled;

    protected MeterRegistry meterRegistry;

    public AbstractAttributesTbCacheStatsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean areCacheStatsEnabled() {
        return metricsEnabled && attributeCacheStatsEnabled;
    }
}
