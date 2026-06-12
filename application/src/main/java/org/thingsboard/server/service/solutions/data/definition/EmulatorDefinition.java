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
package org.thingsboard.server.service.solutions.data.definition;

import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
public class EmulatorDefinition {
    private String name;
    private String extendz;
    private String clazz;
    private int publishPeriodInDays;
    private int publishFrequencyInSeconds;
    private int publishPauseInMillis;
    private long activityPeriodInMillis;
    private List<TelemetryProfile> telemetryProfiles = Collections.emptyList();

    public void enrich(EmulatorDefinition parent) {
        if (StringUtils.isEmpty(clazz)) {
            clazz = parent.getClazz();
        }
        if (publishPeriodInDays == 0) {
            publishPeriodInDays = parent.getPublishPeriodInDays();
        }
        if (publishFrequencyInSeconds == 0) {
            publishFrequencyInSeconds = parent.getPublishFrequencyInSeconds();
        }
        if (publishPauseInMillis == 0) {
            publishPauseInMillis = parent.getPublishPauseInMillis();
        }
        if (activityPeriodInMillis == 0L) {
            activityPeriodInMillis = parent.getActivityPeriodInMillis();
        }
        var profilesMap = telemetryProfiles.stream().collect(Collectors.toMap(TelemetryProfile::getKey, Function.identity()));
        parent.getTelemetryProfiles().forEach(tp -> profilesMap.putIfAbsent(tp.getKey(), tp));
        telemetryProfiles = new ArrayList<>(profilesMap.values());
    }

    public long getOldestTs(long startTs) {
        return startTs - TimeUnit.DAYS.toMillis(publishPeriodInDays) - publishFrequencyInSeconds;
    }

}
