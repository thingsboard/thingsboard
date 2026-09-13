// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.telemetry;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.telemetry.settings.TimeseriesProcessingSettings;

import static org.thingsboard.rule.engine.telemetry.settings.TimeseriesProcessingSettings.OnEveryMessage;

@Data
public class TbMsgTimeseriesNodeConfiguration implements NodeConfiguration<TbMsgTimeseriesNodeConfiguration> {

    private long defaultTTL;
    private boolean useServerTs;
    @NotNull
    private TimeseriesProcessingSettings processingSettings;

    @Override
    public TbMsgTimeseriesNodeConfiguration defaultConfiguration() {
        TbMsgTimeseriesNodeConfiguration configuration = new TbMsgTimeseriesNodeConfiguration();
        configuration.setDefaultTTL(0L);
        configuration.setUseServerTs(false);
        configuration.setProcessingSettings(new OnEveryMessage());
        return configuration;
    }

}
