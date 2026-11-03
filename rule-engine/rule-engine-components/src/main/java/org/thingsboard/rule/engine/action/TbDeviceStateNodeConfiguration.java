// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.action;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.msg.TbMsgType;

@Data
public class TbDeviceStateNodeConfiguration implements NodeConfiguration<TbDeviceStateNodeConfiguration> {

    private TbMsgType event;

    @Override
    public TbDeviceStateNodeConfiguration defaultConfiguration() {
        var config = new TbDeviceStateNodeConfiguration();
        config.setEvent(TbMsgType.ACTIVITY_EVENT);
        return config;
    }

}
