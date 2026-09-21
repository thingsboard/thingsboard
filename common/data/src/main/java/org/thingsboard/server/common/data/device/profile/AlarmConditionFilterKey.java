// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serializable;

@Schema
@Data
public class AlarmConditionFilterKey implements Serializable {

    @Schema(description = "The key type", example = "TIME_SERIES")
    private final AlarmConditionKeyType type;
    @NoXss
    @Schema(description = "String value representing the key", example = "temp")
    private final String key;

}
