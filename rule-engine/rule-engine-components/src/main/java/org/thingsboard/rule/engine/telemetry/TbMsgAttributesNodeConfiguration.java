// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.telemetry;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.rule.engine.telemetry.settings.AttributesProcessingSettings;
import org.thingsboard.server.common.data.DataConstants;

import static org.thingsboard.rule.engine.telemetry.settings.AttributesProcessingSettings.OnEveryMessage;

@Data
public class TbMsgAttributesNodeConfiguration implements NodeConfiguration<TbMsgAttributesNodeConfiguration> {

    @NotNull
    private AttributesProcessingSettings processingSettings;

    private String scope;

    private boolean notifyDevice;
    private boolean sendAttributesUpdatedNotification;
    private boolean updateAttributesOnlyOnValueChange;

    @Override
    public TbMsgAttributesNodeConfiguration defaultConfiguration() {
        TbMsgAttributesNodeConfiguration configuration = new TbMsgAttributesNodeConfiguration();
        configuration.setProcessingSettings(new OnEveryMessage());
        configuration.setScope(DataConstants.SERVER_SCOPE);
        configuration.setNotifyDevice(false);
        configuration.setSendAttributesUpdatedNotification(false);
        // Since version 1. For an existing rule nodes for version 0. See the TbNode implementation
        configuration.setUpdateAttributesOnlyOnValueChange(true);
        return configuration;
    }

}
