// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.iot_hub;

import lombok.Data;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;

@Data
public class AlarmRuleInstalledItemDescriptor implements IotHubInstalledItemDescriptor {

    private CalculatedFieldId calculatedFieldId;
    private EntityId entityId;

}
