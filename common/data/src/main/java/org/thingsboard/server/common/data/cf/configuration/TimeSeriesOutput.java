// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema
@Data
public class TimeSeriesOutput implements Output {

    private String name;
    private Integer decimalsByDefault;

    private TimeSeriesOutputStrategy strategy;

    public TimeSeriesOutput() {
        this.strategy = new TimeSeriesRuleChainOutputStrategy();
    }

    @Override
    public OutputType getType() {
        return OutputType.TIME_SERIES;
    }

}
