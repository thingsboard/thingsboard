// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.iot_hub;

import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;

@Data
public class RuleChainInstalledItemDescriptor implements IotHubInstalledItemDescriptor {

    private RuleChainId ruleChainId;
    private EntityId targetProfileId;

}
