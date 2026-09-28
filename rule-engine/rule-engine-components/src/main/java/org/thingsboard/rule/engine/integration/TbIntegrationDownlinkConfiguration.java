// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.integration;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

import java.util.UUID;

@Data
public class TbIntegrationDownlinkConfiguration implements NodeConfiguration<TbIntegrationDownlinkConfiguration> {

    private UUID integrationId;

    @Override
    public TbIntegrationDownlinkConfiguration defaultConfiguration() {
        TbIntegrationDownlinkConfiguration configuration = new TbIntegrationDownlinkConfiguration();
        configuration.setIntegrationId(null);
        return configuration;
    }
}
