// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Deprecated
public class SimpleAlarmConditionSpec implements AlarmConditionSpec {
    @Override
    public AlarmConditionSpecType getType() {
        return AlarmConditionSpecType.SIMPLE;
    }
}
