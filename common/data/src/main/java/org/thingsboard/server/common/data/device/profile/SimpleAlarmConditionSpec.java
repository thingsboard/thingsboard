// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleAlarmConditionSpec implements AlarmConditionSpec {
    @Override
    public AlarmConditionSpecType getType() {
        return AlarmConditionSpecType.SIMPLE;
    }
}
