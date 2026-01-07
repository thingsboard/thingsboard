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
package org.thingsboard.rule.engine.metadata;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.page.SortOrder.Direction;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by mshvayka on 04.09.18.
 */
@Data
public class TbGetTelemetryNodeConfiguration implements NodeConfiguration<TbGetTelemetryNodeConfiguration> {

    public static final int MAX_FETCH_SIZE = 1000;

    private int startInterval;
    private int endInterval;

    private String startIntervalPattern;
    private String endIntervalPattern;

    private boolean useMetadataIntervalPatterns;

    private String startIntervalTimeUnit;
    private String endIntervalTimeUnit;
    private FetchMode fetchMode; //FIRST, LAST, ALL
    private Direction orderBy; //ASC, DESC
    private Aggregation aggregation; //MIN, MAX, AVG, SUM, COUNT, NONE;
    private int limit;

    private List<String> latestTsKeyNames;

    @Override
    public TbGetTelemetryNodeConfiguration defaultConfiguration() {
        TbGetTelemetryNodeConfiguration configuration = new TbGetTelemetryNodeConfiguration();
        configuration.setLatestTsKeyNames(Collections.emptyList());
        configuration.setFetchMode(FetchMode.FIRST);
        configuration.setStartIntervalTimeUnit(TimeUnit.MINUTES.name());
        configuration.setStartInterval(2);
        configuration.setEndIntervalTimeUnit(TimeUnit.MINUTES.name());
        configuration.setEndInterval(1);
        configuration.setUseMetadataIntervalPatterns(false);
        configuration.setStartIntervalPattern("");
        configuration.setEndIntervalPattern("");
        configuration.setOrderBy(Direction.ASC);
        configuration.setAggregation(Aggregation.NONE);
        configuration.setLimit(MAX_FETCH_SIZE);
        return configuration;
    }
}
