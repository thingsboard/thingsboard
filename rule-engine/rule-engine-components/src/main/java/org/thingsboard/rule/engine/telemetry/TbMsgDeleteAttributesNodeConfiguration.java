// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.telemetry;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.DataConstants;

import java.util.Collections;
import java.util.List;

@Data
public class TbMsgDeleteAttributesNodeConfiguration implements NodeConfiguration<TbMsgDeleteAttributesNodeConfiguration> {

    private String scope;
    private List<String> keys;
    private boolean sendAttributesDeletedNotification;
    private boolean notifyDevice;

    @Override
    public TbMsgDeleteAttributesNodeConfiguration defaultConfiguration() {
        TbMsgDeleteAttributesNodeConfiguration configuration = new TbMsgDeleteAttributesNodeConfiguration();
        configuration.setScope(DataConstants.SERVER_SCOPE);
        configuration.setKeys(Collections.emptyList());
        configuration.setSendAttributesDeletedNotification(false);
        configuration.setNotifyDevice(false);
        return configuration;
    }
}
