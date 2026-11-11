// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.rpc;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.service.integration.RemoteIntegrationRpcService;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "integrations.rpc", value = "enabled", havingValue = "false")
public class DummyRemoteIntegrationRpcService implements RemoteIntegrationRpcService {

    @Override
    public void updateIntegration(Integration integration) {

    }

    @Override
    public void updateConverter(Converter converter) {

    }

    @Override
    public boolean handleRemoteDownlink(IntegrationDownlinkMsg msg) {
        return false;
    }
}
