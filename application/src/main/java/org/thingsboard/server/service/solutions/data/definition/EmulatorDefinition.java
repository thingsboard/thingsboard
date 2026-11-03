// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
