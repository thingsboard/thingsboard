// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CalculateDeltaNodeConfiguration implements NodeConfiguration<CalculateDeltaNodeConfiguration> {

    private String inputValueKey;
    private String outputValueKey;
    private boolean useCache;
    private boolean addPeriodBetweenMsgs;
    private String periodValueKey;
    private Integer round;
    private boolean tellFailureIfDeltaIsNegative;
    private boolean excludeZeroDeltas;

    @Override
    public CalculateDeltaNodeConfiguration defaultConfiguration() {
        var configuration = new CalculateDeltaNodeConfiguration();
        configuration.setInputValueKey("pulseCounter");
        configuration.setOutputValueKey("delta");
        configuration.setUseCache(true);
        configuration.setAddPeriodBetweenMsgs(false);
        configuration.setPeriodValueKey("periodInMs");
        configuration.setTellFailureIfDeltaIsNegative(true);
        configuration.setExcludeZeroDeltas(false);
        return configuration;
    }

}
