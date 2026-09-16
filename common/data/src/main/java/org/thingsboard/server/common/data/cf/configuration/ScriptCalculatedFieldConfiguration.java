// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;

@Data
@Schema
@EqualsAndHashCode(callSuper = true)
public class ScriptCalculatedFieldConfiguration extends BaseCalculatedFieldConfiguration implements ArgumentsBasedCalculatedFieldConfiguration {

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.SCRIPT;
    }

}
