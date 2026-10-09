// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.integration.Integration;

public interface RemoteIntegrationRpcService {

    void updateIntegration(Integration integration);

    void updateConverter(Converter converter);

    boolean handleRemoteDownlink(IntegrationDownlinkMsg msg);
}
