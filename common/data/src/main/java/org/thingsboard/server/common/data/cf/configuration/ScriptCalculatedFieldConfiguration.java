// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf.configuration;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;

@Data
@EqualsAndHashCode(callSuper = true)
public class ScriptCalculatedFieldConfiguration extends BaseCalculatedFieldConfiguration implements CalculatedFieldConfiguration {

    @Override
    public CalculatedFieldType getType() {
        return CalculatedFieldType.SCRIPT;
    }

}
