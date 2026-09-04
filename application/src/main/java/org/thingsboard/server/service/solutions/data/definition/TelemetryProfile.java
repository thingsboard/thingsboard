// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.definition;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.service.solutions.data.values.ValueStrategyDefinition;

@Data
@AllArgsConstructor
public class TelemetryProfile {

    private String key;
    private ValueStrategyDefinition valueStrategy;

}
