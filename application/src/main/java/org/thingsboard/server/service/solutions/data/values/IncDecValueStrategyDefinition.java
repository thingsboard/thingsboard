// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.values;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class IncDecValueStrategyDefinition implements ValueStrategyDefinition {

    private int precision;
    private double minStartValue;
    private double maxStartValue;
    private double minEndValue;
    private double maxEndValue;

}
